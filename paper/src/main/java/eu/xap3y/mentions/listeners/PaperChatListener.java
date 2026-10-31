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

public class PaperChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!ChatMentionService.isHoverEnabled()) {
            return;
        }

        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        List<ChatMention> mentions = ChatMentionService.findMentions(event.getPlayer(), plain);
        if (mentions.isEmpty()) {
            return;
        }

        Mentions.getInstance().getCompatibilityGuard().run("Mention hover",
                () -> event.message(PaperMentionRenderer.render(event.message(), plain, mentions)));
        ChatMentionService.notifyTargets(event.getPlayer(), mentions);
    }
}
