package eu.xap3y.mentions.adapter;

import eu.xap3y.mentions.api.dto.ChatMention;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class PaperMentionRenderer {

    private PaperMentionRenderer() {
    }

    public static Component render(Component message, String plain, List<ChatMention> mentions) {
        if (mentions.isEmpty()) {
            return message;
        }

        Replacement replacement = replacement(plain, mentions);
        return message.replaceText(builder -> builder.match(replacement.pattern())
                .replacement(replacement.callback()));
    }

    public static Replacement replacement(String plain, List<ChatMention> mentions) {
        Map<String, Component> replacements = new HashMap<>();
        for (ChatMention mention : mentions) {
            Component replacement = HexUtil.parseText(mention.getReplacement());
            if (!mention.getHoverText().isEmpty()) {
                replacement = replacement.hoverEvent(HoverEvent.showText(HexUtil.parseText(mention.getHoverText())));
            }
            if (!mention.getClickSuggestion().isEmpty()) {
                replacement = replacement.clickEvent(ClickEvent.suggestCommand(mention.getClickSuggestion()));
            }
            replacements.put(plain.substring(mention.getStart(), mention.getEnd()), replacement);
        }

        String alternatives = replacements.keySet().stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        Pattern pattern = Pattern.compile("(?<!\\w)(?:" + alternatives + ")(?!\\w)");
        return new Replacement(pattern, (match, text) -> replacements.get(match.group()));
    }

    public record Replacement(Pattern pattern, BiFunction<MatchResult, TextComponent.Builder, ComponentLike> callback) {
    }
}
