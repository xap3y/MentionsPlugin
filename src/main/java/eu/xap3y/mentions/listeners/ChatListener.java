package eu.xap3y.mentions.listeners;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.api.enums.SettingType;
import eu.xap3y.mentions.manager.ConfigManager;
import eu.xap3y.mentions.service.MentionService;
import eu.xap3y.mentions.service.Texter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ALL")
public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();

        String prefix = Mentions.getInstance().getConfig().getString("mention-prefix", "@");
        boolean enabledReplacer = Mentions.getInstance().getConfig().getBoolean("chat.enabled", false);
        String replacer = enabledReplacer ? Mentions.getInstance().getConfig().getString("chat.replace", " ") : null;

        for (Player target : Bukkit.getOnlinePlayers()) {
            String name = target.getName();
            String regex = "(?i)(?<!\\w)" + Pattern.quote(prefix) + Pattern.quote(name) + "(?!\\w)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(message);

            while (matcher.find()) {
                if (!ConfigManager.getSetting(SettingType.ABLE_TO_MENTION, target.getUniqueId())) return;
                // get last color codes before mention
                String before = message.substring(0, matcher.start());
                String activeColors = ChatColor.getLastColors(Texter.colored(before));

                String replacement;
                if (enabledReplacer) {
                    replacement = Texter.colored(replacer.replace("%nick%", "&a&l" + name));
                } else {
                    replacement = Texter.colored("&a&l" + name);
                }

                replacement += activeColors;

                message = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
                matcher = pattern.matcher(message);

                MentionService.run(event.getPlayer(), target);
            }
        }

        event.setMessage(message);
    }
}
