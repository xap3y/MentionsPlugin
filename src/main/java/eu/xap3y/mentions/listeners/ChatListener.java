package eu.xap3y.mentions.listeners;


import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.service.MentionService;
import eu.xap3y.mentions.service.Texter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Pattern;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();

        String prefix = Mentions.getInstance().getConfig().getString("mention-prefix", "@");

        String replacer = null;

        boolean enabledReplacer = Mentions.getInstance().getConfig().getBoolean("chat.enabled", false);
        if (enabledReplacer) {
            replacer = Mentions.getInstance().getConfig().getString("chat.replace", " ");
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            String name = target.getName().toLowerCase();
            String regex = "(?i)(?<!\\w)" + Pattern.quote(prefix) + Pattern.quote(name) + "(?!\\w)";

            if (message.matches(".*" + regex + ".*")) {
                if (enabledReplacer) {
                    String replacement = Texter.colored(replacer.replaceAll("%nick%", name));
                    message = message.replaceAll(regex, replacement);
                }
                MentionService.run(event.getPlayer(), target);
            }
        }
        event.setMessage(message);
    }
}
