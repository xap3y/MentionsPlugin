package eu.xap3y.mentions.listeners;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.adapter.HoverChatSender;
import eu.xap3y.mentions.api.dto.ChatMention;
import eu.xap3y.mentions.service.ChatMentionService;
import eu.xap3y.mentions.service.CompatibilityGuard;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class ChatListener implements Listener {

    private final boolean nativeChat;
    private final boolean rgbSupported;
    private volatile HoverChatSender hoverSender;

    public ChatListener(boolean nativeChat) {
        this.nativeChat = nativeChat;
        String[] version = Bukkit.getBukkitVersion().split("[.-]");
        this.rgbSupported = Integer.parseInt(version[0]) > 1 || Integer.parseInt(version[1]) >= 16;
        this.hoverSender = HoverChatSender.create();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        boolean hoverEnabled = ChatMentionService.isHoverEnabled();
        if (nativeChat && hoverEnabled) {
            return;
        }

        List<ChatMention> mentions = ChatMentionService.findMentions(event.getPlayer(), event.getMessage());
        if (mentions.isEmpty()) {
            return;
        }

        HoverChatSender sender = hoverSender;
        CompatibilityGuard compatibility = Mentions.getInstance().getCompatibilityGuard();
        boolean sentWithHover = false;
        if (hoverEnabled && sender != null) {
            sentWithHover = compatibility.run("Mention hover",
                    () -> sender.send(event, mentions, rgbSupported));
            if (!sentWithHover) {
                hoverSender = null;
            }
        } else if (hoverEnabled) {
            compatibility.warnOnce("Mention hover", "chat component API is unavailable; using plain chat");
        }
        if (!sentWithHover && !event.isCancelled()) {
            event.setMessage(ChatMentionService.replaceLegacy(event.getMessage(), mentions));
        }

        ChatMentionService.notifyTargets(event.getPlayer(), mentions);
    }
}
