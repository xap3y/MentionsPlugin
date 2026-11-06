package eu.xap3y.mentions.adapter;

import eu.xap3y.mentions.api.dto.ChatMention;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ClickEvent;
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
                if (!mention.getClickSuggestion().isEmpty()) {
                    replacement.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, mention.getClickSuggestion()));
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
        return MentionColors.color(text, rgbSupported);
    }
}
