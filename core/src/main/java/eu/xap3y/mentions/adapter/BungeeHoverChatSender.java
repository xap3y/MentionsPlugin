package eu.xap3y.mentions.adapter;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.service.ChatMentionService;
import eu.xap3y.mentions.api.dto.ChatMention;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class BungeeHoverChatSender implements HoverChatSender {

    @Override
    public void send(AsyncPlayerChatEvent event, List<ChatMention> mentions, boolean rgbSupported) {
        // Finish component rendering before cancelling the normal Bukkit message.
        BaseComponent[] message = LegacyMentionRenderer.render(event.getFormat(),
                event.getPlayer().getDisplayName(), event.getMessage(), mentions, rgbSupported);
        String plainMessage = BaseComponent.toLegacyText(message);

        event.setCancelled(true);
        for (Player recipient : event.getRecipients()) {
            try {
                BaseComponent[] personalized = LegacyMentionRenderer.render(event.getFormat(),
                        event.getPlayer().getDisplayName(), event.getMessage(),
                        ChatMentionService.forViewer(mentions, event.getPlayer(), recipient), rgbSupported);
                recipient.spigot().sendMessage(personalized);
            } catch (RuntimeException | LinkageError error) {
                Mentions.getInstance().getCompatibilityGuard().warnOnce("Mention hover", error);
                recipient.sendMessage(plainMessage);
            }
        }
        Bukkit.getConsoleSender().sendMessage(plainMessage);
    }
}
