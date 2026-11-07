package eu.xap3y.mentions.listeners;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.adapter.PaperMentionRenderer;
import eu.xap3y.mentions.api.dto.ChatMention;
import eu.xap3y.mentions.service.ChatMentionService;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public final class ChatControlListener implements Listener {

    public static void register(Mentions mentionsPlugin) throws ReflectiveOperationException {
        PluginManager manager = mentionsPlugin.getServer().getPluginManager();
        Plugin chatControl = manager.getPlugin("ChatControl");
        if (chatControl == null || !chatControl.isEnabled()) {
            return;
        }

        Class<? extends Event> eventType = Class.forName(
                "org.mineacademy.chatcontrol.api.ChannelPostChatEvent", false,
                chatControl.getClass().getClassLoader()).asSubclass(Event.class);
        Method getSender = eventType.getMethod("getSender");
        Method getMessage = eventType.getMethod("getMessage");
        Method getFormat = eventType.getMethod("getFormat");
        Class<?> formatType = getFormat.getReturnType();
        Method setFormat = eventType.getMethod("setFormat", formatType);
        Method replaceMatch = formatType.getMethod("replaceMatch", Pattern.class, BiFunction.class);

        manager.registerEvent(eventType, new ChatControlListener(), EventPriority.HIGHEST, (listener, event) -> {
            try {
                if (!(getSender.invoke(event) instanceof Player sender)) {
                    return;
                }
                String message = (String) getMessage.invoke(event);
                List<ChatMention> mentions = ChatMentionService.findMentions(sender, message);
                if (mentions.isEmpty()) {
                    return;
                }

                PaperMentionRenderer.Replacement replacement = PaperMentionRenderer.replacement(
                        message, ChatMentionService.forViewer(mentions, sender, null));
                Object format = getFormat.invoke(event);
                Object rendered = replaceMatch.invoke(format, replacement.pattern(), replacement.callback());
                setFormat.invoke(event, rendered);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                mentionsPlugin.getCompatibilityGuard().warnOnce("ChatControl mentions", error);
            }
        }, mentionsPlugin, true);
    }
}
