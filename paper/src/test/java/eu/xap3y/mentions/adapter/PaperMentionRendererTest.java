package eu.xap3y.mentions.adapter;

import eu.xap3y.mentions.api.dto.ChatMention;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class PaperMentionRendererTest {

    @Test
    public void gradientReplacementWorksWithoutHoverOrClick() {
        String message = "Hi @Bob!";
        ChatMention mention = new ChatMention(3, 7, null,
                "<b><gradient:#DFFF14:#8CFF81>Bob</gradient></b>", "");
        Component rendered = PaperMentionRenderer.render(Component.text(message), message,
                Collections.singletonList(mention));
        assertEquals("Hi Bob!", PlainTextComponentSerializer.plainText().serialize(rendered));
        String legacy = HexUtil.toLegacy(mention.getReplacement(), true);
        assertTrue(legacy.contains("§x"));
        assertFalse(legacy.contains("<gradient"));
    }

    @Test
    public void hashHexReplacementConvertsToLegacyRgb() {
        String legacy = HexUtil.toLegacy("&#ddffffBob", true);
        assertTrue(legacy.contains("§x§d§d§f§f§f§f"));
        assertFalse(legacy.contains("&#"));
        assertFalse(HexUtil.toLegacy("&#ddffffBob", false).contains("§x"));
    }

    @Test
    public void suggestionDoesNotAttachToSurroundingText() {
        ChatMention mention = new ChatMention(3, 7, null, "Bob", "");
        mention.setClickSuggestion("/msg Bob ");
        Component rendered = PaperMentionRenderer.render(Component.text("Hi @Bob!"), "Hi @Bob!",
                Collections.singletonList(mention));
        Component clicked = rendered.children().stream().filter(child -> child.clickEvent() != null)
                .findFirst().orElseThrow();
        assertEquals(ClickEvent.suggestCommand("/msg Bob "), clicked.clickEvent());
        assertNull(rendered.clickEvent());
        assertNull(rendered.children().getLast().clickEvent());
    }

    @Test
    public void hoverSupportsMiniMessageRgbAndMultiline() {
        String message = "Hello @Bob!";
        Component rendered = PaperMentionRenderer.render(Component.text(message), message,
                Collections.singletonList(new ChatMention(6, 10, null, "&aBob",
                        "<gradient:#ff0000:#0000ff>RGB</gradient>\n&#55FFFFHealth: 20")));

        assertEquals("Hello Bob!", PlainTextComponentSerializer.plainText().serialize(rendered));
        Component mention = rendered.children().stream()
                .filter(component -> component.hoverEvent() != null)
                .findFirst().orElseThrow();
        Component tooltip = (Component) mention.hoverEvent().value();
        assertEquals("RGB\nHealth: 20", PlainTextComponentSerializer.plainText().serialize(tooltip));
        assertTrue(tooltip.toString().contains("#55ffff"));
        assertNull(rendered.hoverEvent());
        assertNull(rendered.children().getLast().hoverEvent());
    }

    @Test
    public void preservesExistingComponentsAndDoesNotParsePlayerMarkup() {
        Component link = Component.text("link", NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.openUrl("https://example.com"));
        Component message = Component.text("<red>Hello @Bob! ").append(link);
        String plain = PlainTextComponentSerializer.plainText().serialize(message);

        Component rendered = PaperMentionRenderer.render(message, plain,
                Collections.singletonList(new ChatMention(11, 15, null, "&aBob", "&bTooltip")));

        assertEquals("<red>Hello Bob! link", PlainTextComponentSerializer.plainText().serialize(rendered));
        assertEquals(link, rendered.children().getLast());
        assertTrue(((TextComponent) rendered).content().startsWith("<red>"));
    }

    @Test
    public void noMentionsKeepsOriginalComponent() {
        Component message = Component.text("Hello!");
        assertSame(message, PaperMentionRenderer.render(message, "Hello!", Collections.emptyList()));
    }
}
