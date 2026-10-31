package eu.xap3y.mentions.service;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.api.dto.ChatMention;
import eu.xap3y.mentions.api.enums.SettingType;
import eu.xap3y.mentions.manager.ConfigManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatMentionService {

    private ChatMentionService() {
    }

    public static boolean isHoverEnabled() {
        return Mentions.getInstance().getConfig().getBoolean("chat.hover.enabled", false);
    }

    public static List<ChatMention> findMentions(Player sender, String message) {
        FileConfiguration config = Mentions.getInstance().getConfig();
        String prefix = config.getString("mention-prefix", "@");
        boolean multiMentions = config.getBoolean("multi-mentions", true);
        boolean selfMention = config.getBoolean("self-mention", false);
        String replacementTemplate = config.getBoolean("chat.enabled", false)
                ? config.getString("chat.replace", "&a&l%nick%&r")
                : "&a&l%nick%";
        String hoverTemplate = config.isList("chat.hover.text")
                ? String.join("\n", config.getStringList("chat.hover.text"))
                : config.getString("chat.hover.text", "");
        List<ChatMention> mentions = new ArrayList<>();

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!selfMention && target.getUniqueId().equals(sender.getUniqueId())) {
                continue;
            }
            if (!ConfigManager.getSetting(SettingType.ABLE_TO_MENTION, target.getUniqueId())
                    && target.hasPermission("mentions.admin")) {
                continue;
            }
            if (!ConfigManager.isAbleToBeMentionedFromConfig(target.getName())) {
                continue;
            }

            Matcher matcher = mentionPattern(prefix, target.getName()).matcher(message);
            if (!matcher.find()) {
                continue;
            }

            String replacement = replaceVariables(replacementTemplate, sender, target);
            String hover = isHoverEnabled() ? replaceVariables(hoverTemplate, sender, target) : "";
            if (!hover.isEmpty() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                hover = PlaceholderAPI.setPlaceholders(target, hover);
            }

            do {
                mentions.add(new ChatMention(matcher.start(), matcher.end(), target, replacement, hover));
            } while (matcher.find());

            if (!multiMentions) {
                break;
            }
        }

        mentions.sort(Comparator.comparingInt(ChatMention::getStart));
        return mentions;
    }

    public static Pattern mentionPattern(String prefix, String name) {
        return Pattern.compile("(?i)(?<!\\w)" + Pattern.quote(prefix) + Pattern.quote(name) + "(?!\\w)");
    }

    public static String replaceVariables(String text, Player sender, Player target) {
        return text.replace("%nick%", target.getName())
                .replace("%target%", target.getName())
                .replace("%player%", sender.getName());
    }

    public static String replaceLegacy(String message, List<ChatMention> mentions) {
        StringBuilder result = new StringBuilder();
        int position = 0;
        for (ChatMention mention : mentions) {
            result.append(message, position, mention.getStart());
            result.append(Texter.colored(mention.getReplacement()));
            result.append(ChatColor.RESET);
            result.append(ChatColor.getLastColors(Texter.colored(message.substring(0, mention.getStart()))));
            position = mention.getEnd();
        }
        return result.append(message.substring(position)).toString();
    }

    public static void notifyTargets(Player sender, List<ChatMention> mentions) {
        Set<UUID> notified = new HashSet<>();
        for (ChatMention mention : mentions) {
            if (notified.add(mention.getTarget().getUniqueId())) {
                MentionService.run(sender, mention.getTarget());
            }
        }
    }
}
