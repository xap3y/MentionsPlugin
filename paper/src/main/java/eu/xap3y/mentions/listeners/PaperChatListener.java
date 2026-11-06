package eu.xap3y.mentions.listeners;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.adapter.PaperMentionRenderer;
import eu.xap3y.mentions.api.dto.ChatMention;
import eu.xap3y.mentions.service.ChatMentionService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import io.papermc.paper.chat.ChatRenderer;
import org.bukkit.entity.Player;

public class PaperChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        List<ChatMention> mentions = ChatMentionService.findMentions(event.getPlayer(), plain);
        if (mentions.isEmpty()) {
            return;
        }

        ChatRenderer previous = event.renderer();
        event.renderer((source, displayName, message, viewer) -> {
            net.kyori.adventure.text.Component rendered = message;
            try {
                rendered = PaperMentionRenderer.render(message, plain,
                        ChatMentionService.forViewer(mentions, source, viewer instanceof Player ? (Player) viewer : null));
            } catch (RuntimeException | LinkageError error) {
                Mentions.getInstance().getCompatibilityGuard().warnOnce("Mention hover", error);
            }
            return previous.render(source, displayName, rendered, viewer);
        });
        ChatMentionService.notifyTargets(event.getPlayer(), mentions);
    }
}
