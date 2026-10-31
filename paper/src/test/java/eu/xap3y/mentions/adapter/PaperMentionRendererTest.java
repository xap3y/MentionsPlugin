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
