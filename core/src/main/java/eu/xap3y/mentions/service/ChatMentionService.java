package eu.xap3y.mentions.service;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.adapter.MentionColors;
import eu.xap3y.mentions.api.dto.ChatMention;
import eu.xap3y.mentions.api.enums.SettingType;
import eu.xap3y.mentions.manager.ConfigManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatMentionService {

    private ChatMentionService() {
    }

    public static boolean isHoverEnabled() {
        return Mentions.getInstance().getConfig().getBoolean("chat.hover.enabled", false);
    }

    public static boolean isInteractiveEnabled() {
        return isHoverEnabled() || Mentions.getInstance().getConfig().getBoolean("chat.click.enabled", false);
    }

    public static List<ChatMention> forViewer(List<ChatMention> mentions, Player sender, Player viewer) {
        List<ChatMention> rendered = new ArrayList<>();
        FileConfiguration config = Mentions.getInstance().getConfig();
        for (ChatMention mention : mentions) {
            ChatMention copy = new ChatMention(mention.getStart(), mention.getEnd(), mention.getTarget(),
                    mention.getReplacement(), mention.getHoverText());
            if (mention.getTarget() != null && viewer != null && config.getBoolean("chat.click.enabled", false)) {
                copy.setClickSuggestion(clickVariables(config.getString("chat.click.suggestion", "/msg %target_name% "),
                        sender, mention.getTarget(), viewer));
            }
            rendered.add(copy);
        }
        return rendered;
    }

    public static String clickVariables(String text, Player sender, Player target, Player clicker) {
        return text.replace("%target_name%", target.getName())
                .replace("%target_uuid%", target.getUniqueId().toString())
                .replace("%sender_name%", sender.getName())
                .replace("%sender_uuid%", sender.getUniqueId().toString())
                .replace("%clicker_name%", clicker.getName())
                .replace("%clicker_uuid%", clicker.getUniqueId().toString());
    }

    private static boolean eligible(Player sender, Player target) {
        return (Mentions.getInstance().getConfig().getBoolean("self-mention", false)
                || !sender.getUniqueId().equals(target.getUniqueId()))
                && ConfigManager.isAbleToBeMentionedFromConfig(target.getName())
                && (ConfigManager.getSetting(SettingType.ABLE_TO_MENTION, target.getUniqueId())
                || !target.hasPermission("mentions.admin"));
    }

    private static void addPing(List<ChatMention> mentions, Player sender, String message,
                                ConfigurationSection preset, boolean everyone) {
        if (everyone && preset.getBoolean("require-permisison", false)
                && !sender.hasPermission("mentions.everyone")) {
            return;
        }
        if (!preset.getBoolean("enabled", true)) {
            return;
        }
        String text = preset.getString("text", everyone ? "@everyone" : "");
        if (text.isEmpty()) {
            return;
        }
        Matcher matcher = mentionPattern("", text).matcher(message);
        if (!matcher.find()) {
            return;
        }
        List<Player> recipients = new ArrayList<>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (eligible(sender, target) && (everyone || VaultPingService.matches(target, preset))) {
                recipients.add(target);
            }
        }
        if (recipients.isEmpty()) {
            return;
        }
        do {
            final int start = matcher.start();
            final int end = matcher.end();
            if (mentions.stream().anyMatch(existing -> start < existing.getEnd() && end > existing.getStart())) {
                continue;
            }
            ChatMention mention = new ChatMention(start, end, null, pingReplacement(preset, text), "");
            mention.setRecipients(recipients);
            mention.setSoundOverride(preset.getString("ping-sound", ""));
            mention.setEnableTitle(preset.getBoolean("enable-title", true));
            mention.setEnableActionbar(preset.getBoolean("enable-actionbar", true));
            mentions.add(mention);
        } while (matcher.find());
    }

    public static String pingReplacement(ConfigurationSection preset, String text) {
        return preset.getString("replace", "&a&l%text%&r").replace("%text%", text);
    }

    public static List<ChatMention> findMentions(Player sender, String message) {
        FileConfiguration config = Mentions.getInstance().getConfig();
        String prefix = config.getString("mention-prefix", "@");
        boolean multiMentions = config.getBoolean("multi-mentions", true);
        String replacementTemplate = config.getBoolean("chat.enabled", false)
                ? config.getString("chat.replace", "&a&l%nick%&r")
                : "&a&l%nick%";
        String hoverTemplate = config.isList("chat.hover.text")
                ? String.join("\n", config.getStringList("chat.hover.text"))
                : config.getString("chat.hover.text", "");
        List<ChatMention> mentions = new ArrayList<>();

        ConfigurationSection everyone = config.getConfigurationSection("everyone-ping");
        if (everyone != null) {
            addPing(mentions, sender, message, everyone, true);
        }
        ConfigurationSection groups = config.getConfigurationSection("group-pings");
        if (groups != null && (multiMentions || mentions.isEmpty())) {
            for (String key : groups.getKeys(false)) {
                ConfigurationSection preset = groups.getConfigurationSection(key);
                if (preset != null) {
                    addPing(mentions, sender, message, preset, false);
                }
                if (!multiMentions && !mentions.isEmpty()) {
                    break;
                }
            }
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!multiMentions && !mentions.isEmpty()) {
                break;
            }
            if (!eligible(sender, target)) {
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
                final int start = matcher.start();
                final int end = matcher.end();
                if (mentions.stream().noneMatch(existing -> start < existing.getEnd() && end > existing.getStart())) {
                    mentions.add(new ChatMention(start, end, target, replacement, hover));
                }
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
        String[] version = Bukkit.getBukkitVersion().split("[.-]");
        boolean rgbSupported = Integer.parseInt(version[0]) > 1 || Integer.parseInt(version[1]) >= 16;
        return replaceLegacy(message, mentions, rgbSupported);
    }

    public static String replaceLegacy(String message, List<ChatMention> mentions, boolean rgbSupported) {
        StringBuilder result = new StringBuilder();
        int position = 0;
        for (ChatMention mention : mentions) {
            result.append(message, position, mention.getStart());
            result.append(MentionColors.color(mention.getReplacement(), rgbSupported));
            result.append(ChatColor.RESET);
            result.append(ChatColor.getLastColors(Texter.colored(message.substring(0, mention.getStart()))));
            position = mention.getEnd();
        }
        return result.append(message.substring(position)).toString();
    }

    public static void notifyTargets(Player sender, List<ChatMention> mentions) {
        Map<UUID, ChatMention> options = new LinkedHashMap<>();
        Map<UUID, Player> targets = new LinkedHashMap<>();
        for (ChatMention mention : mentions) {
            List<Player> recipients = mention.getTarget() == null
                    ? mention.getRecipients() : Collections.singletonList(mention.getTarget());
            for (Player target : recipients) {
                UUID id = target.getUniqueId();
                ChatMention previous = options.get(id);
                if (previous == null || (previous.getTarget() != null && mention.getTarget() == null)
                        || (previous.getSoundOverride().isEmpty() && !mention.getSoundOverride().isEmpty())) {
                    options.put(id, mention);
                    targets.put(id, target);
                }
            }
        }
        for (Map.Entry<UUID, Player> entry : targets.entrySet()) {
            ChatMention optionsForTarget = options.get(entry.getKey());
            MentionService.run(sender, entry.getValue(), optionsForTarget.getSoundOverride(),
                    optionsForTarget.isEnableTitle(), optionsForTarget.isEnableActionbar());
        }
    }
}
