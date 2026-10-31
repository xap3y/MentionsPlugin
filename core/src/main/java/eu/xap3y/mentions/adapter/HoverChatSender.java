package eu.xap3y.mentions.adapter;

import eu.xap3y.mentions.api.dto.ChatMention;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.lang.reflect.Array;
import java.util.List;

/** Keeps optional chat component classes out of the Bukkit listener. */
public interface HoverChatSender {

    void send(AsyncPlayerChatEvent event, List<ChatMention> mentions, boolean rgbSupported);

    static HoverChatSender create() {
        try {
            Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Class.forName("net.md_5.bungee.api.chat.HoverEvent");
            Class<?> baseComponent = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Class<?> componentArray = Array.newInstance(baseComponent, 0).getClass();
            Player.class.getMethod("spigot").getReturnType().getMethod("sendMessage", componentArray);
            return (HoverChatSender) Class.forName("eu.xap3y.mentions.adapter.BungeeHoverChatSender")
                    .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }
}
