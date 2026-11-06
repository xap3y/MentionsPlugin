package eu.xap3y.mentions.adapter;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Color fallback that needs neither Adventure nor Bungee classes. */
public final class LegacyColors {

    private static final Pattern HEX = Pattern.compile("(?i)&#([0-9a-f]{6})|[&§]x((?:[&§][0-9a-f]){6})");
    private static final int[] LEGACY_COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private LegacyColors() {
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
