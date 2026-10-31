package eu.xap3y.mentions.adapter;

import eu.xap3y.mentions.api.dto.ChatMention;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Legacy chat rendering, deliberately independent of Adventure classes. */
@SuppressWarnings("deprecation")
public final class LegacyMentionRenderer {

    private static final Pattern HEX = Pattern.compile("(?i)&#([0-9a-f]{6})|[&§]x((?:[&§][0-9a-f]){6})");
    private static final int[] LEGACY_COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private LegacyMentionRenderer() {
    }

    public static BaseComponent[] render(String format, String displayName, String message,
                                         List<ChatMention> mentions, boolean rgbSupported) {
        // Mark only actual mention occurrences, never matching names in a prefix or display name.
        String marker = "\u0000" + UUID.randomUUID() + ":";
        Pattern markerPattern = Pattern.compile(Pattern.quote(marker) + "(\\d+)\u0000");
        StringBuilder markedMessage = new StringBuilder();
        int position = 0;
        for (int index = 0; index < mentions.size(); index++) {
            ChatMention mention = mentions.get(index);
            markedMessage.append(message, position, mention.getStart());
            markedMessage.append(marker).append(index).append('\u0000');
            position = mention.getEnd();
        }
        markedMessage.append(message.substring(position));
        String formatted = String.format(format, displayName, markedMessage.toString());
        List<BaseComponent> result = new ArrayList<>();

        for (BaseComponent base : TextComponent.fromLegacyText(formatted)) {
            TextComponent original = (TextComponent) base;
            Matcher matcher = markerPattern.matcher(original.getText());
            int offset = 0;
            while (matcher.find()) {
                appendText(result, original, original.getText().substring(offset, matcher.start()));
                ChatMention mention = mentions.get(Integer.parseInt(matcher.group(1)));
                TextComponent replacement = new TextComponent(original);
                replacement.setText("");
                replacement.setClickEvent(null);
                replacement.setExtra(Arrays.asList(TextComponent.fromLegacyText(
                        color(mention.getReplacement(), rgbSupported))));
                if (!mention.getHoverText().isEmpty()) {
                    replacement.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            TextComponent.fromLegacyText(color(mention.getHoverText(), rgbSupported))));
                }
                result.add(replacement);
                offset = matcher.end();
            }
            appendText(result, original, original.getText().substring(offset));
        }

        return result.toArray(new BaseComponent[0]);
    }

    private static void appendText(List<BaseComponent> result, TextComponent style, String text) {
        if (text.isEmpty()) {
            return;
        }
        TextComponent component = new TextComponent(style);
        component.setText(text);
        result.add(component);
    }

    public static String color(String text, boolean rgbSupported) {
        Matcher matcher = HEX.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1) != null
                    ? matcher.group(1)
                    : matcher.group(2).replace("&", "").replace("§", "");
            String replacement;
            if (rgbSupported) {
                StringBuilder rgb = new StringBuilder("§x");
                for (char digit : hex.toCharArray()) {
                    rgb.append('§').append(digit);
                }
                replacement = rgb.toString();
            } else {
                replacement = nearestColor(Integer.parseInt(hex, 16));
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    private static String nearestColor(int rgb) {
        int nearest = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < LEGACY_COLORS.length; index++) {
            int color = LEGACY_COLORS[index];
            int red = (rgb >> 16 & 255) - (color >> 16 & 255);
            int green = (rgb >> 8 & 255) - (color >> 8 & 255);
            int blue = (rgb & 255) - (color & 255);
            int distance = red * red + green * green + blue * blue;
            if (distance < bestDistance) {
                nearest = index;
                bestDistance = distance;
            }
        }
        return "§" + "0123456789abcdef".charAt(nearest);
    }
}
