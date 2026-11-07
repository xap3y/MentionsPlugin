package eu.xap3y.mentions.listeners;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.adapter.PaperMentionRenderer;
import eu.xap3y.mentions.api.dto.ChatMention;
import eu.xap3y.mentions.service.ChatMentionService;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class PaperChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Component message = event.message();
        String plain = PlainTextComponentSerializer.plainText().serialize(message);
        List<ChatMention> mentions = ChatMentionService.findMentions(event.getPlayer(), plain);
        if (mentions.isEmpty()) {
            return;
        }

        if (Mentions.getInstance().getServer().getPluginManager().isPluginEnabled("EssentialsChat")) {
            // EssentialsChat captures its own message component when installing its renderer.
            ChatRenderer previous = event.renderer();
            event.renderer((sender, displayName, body, viewer) -> {
                Component formatted = previous.render(sender, displayName, body, viewer);
                Player recipient = viewer instanceof Player ? (Player) viewer : null;
                return render(formatted, plain, mentions, sender, recipient);
            });
        } else {
            event.message(render(message, plain, mentions, event.getPlayer(), null));
        }
        ChatMentionService.notifyTargets(event.getPlayer(), mentions);
    }

    private Component render(Component message, String plain, List<ChatMention> mentions, Player sender, Player viewer) {
        try {
            return PaperMentionRenderer.render(message, plain, ChatMentionService.forViewer(mentions, sender, viewer));
        } catch (RuntimeException | LinkageError error) {
            Mentions.getInstance().getCompatibilityGuard().warnOnce("Mention hover", error);
            return message;
        }
    }
}
