package eu.xap3y.mentions.adapter;

import eu.xap3y.mentions.api.dto.ChatMention;
import eu.xap3y.mentions.service.ChatMentionService;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class LegacyMentionRendererTest {

    @Test
    public void hoverIsLimitedToMentionAndPreservesChatFormat() {
        String message = "Hello @Bob, how are you? 100%";
        List<ChatMention> mentions = Collections.singletonList(
                new ChatMention(6, 10, null, "&a&lBob&r", "&bBob\n&7Health: 20"));

        BaseComponent[] rendered = LegacyMentionRenderer.render(
                "§c[rank] %1$s: §e%2$s", "@Bob", message, mentions, false);

        assertEquals("[rank] @Bob: Hello Bob, how are you? 100%", BaseComponent.toPlainText(rendered));
        int hoverCount = 0;
        for (BaseComponent component : rendered) {
            if (component.getHoverEvent() != null) {
                hoverCount++;
                assertEquals("Bob", component.toPlainText());
                assertEquals(HoverEvent.Action.SHOW_TEXT, component.getHoverEvent().getAction());
                assertEquals("Bob\nHealth: 20", BaseComponent.toPlainText(component.getHoverEvent().getValue()));
            } else {
                assertFalse(component.isBold());
            }
        }
        assertEquals(1, hoverCount);
        assertEquals(net.md_5.bungee.api.ChatColor.YELLOW, rendered[rendered.length - 1].getColor());
    }

    @Test
    public void repeatedAndAdjacentMentionsKeepTheirOwnTooltips() {
        String message = "@Bob,@Alice @Bob";
        List<ChatMention> mentions = Arrays.asList(
                new ChatMention(0, 4, null, "&aBob", "First"),
                new ChatMention(5, 11, null, "&bAlice", "Second"),
                new ChatMention(12, 16, null, "&aBob", "First"));

        BaseComponent[] rendered = LegacyMentionRenderer.render("%2$s", "Sender", message, mentions, false);

        assertEquals("Bob,Alice Bob", BaseComponent.toPlainText(rendered));
        assertEquals(3, Arrays.stream(rendered).filter(component -> component.getHoverEvent() != null).count());
    }

    @Test
    public void emptyTooltipDoesNotAddHover() {
        BaseComponent[] rendered = LegacyMentionRenderer.render("%s: %s", "Sender", "@Bob",
                Collections.singletonList(new ChatMention(0, 4, null, "Bob", "")), false);

        assertEquals("Sender: Bob", BaseComponent.toPlainText(rendered));
        assertTrue(Arrays.stream(rendered).allMatch(component -> component.getHoverEvent() == null));
    }

    @Test
    public void supportsHexAndNibbleRgbWithoutAdventure() {
        assertEquals("§x§1§2§3§4§5§6RGB §lbold", LegacyMentionRenderer.color("&#123456RGB &lbold", true));
        assertEquals("§x§1§2§3§4§5§6RGB", LegacyMentionRenderer.color("&x&1&2&3&4&5&6RGB", true));
        assertEquals("§x§1§2§3§4§5§6RGB", LegacyMentionRenderer.color("§x§1§2§3§4§5§6RGB", true));
    }

    @Test
    public void oldClientsReceiveNearestLegacyColor() {
        assertEquals("§bCyan", LegacyMentionRenderer.color("&#55FFFFCyan", false));
        assertEquals("§4Red", LegacyMentionRenderer.color("&x&A&A&0&0&0&0Red", false));
        assertFalse(LegacyMentionRenderer.color("&#123456Text", false).contains("§x"));
    }

    @Test
    public void mentionBoundariesAndLiteralPrefixArePreserved() {
        Pattern pattern = ChatMentionService.mentionPattern("@", "Bob");
        assertTrue(pattern.matcher("Hello @bOb!").find());
        assertFalse(pattern.matcher("Hello @Bobcat").find());
        assertFalse(pattern.matcher("email@Bob").find());
        assertTrue(ChatMentionService.mentionPattern("[+]", "Bob").matcher("[+]Bob!").find());
    }
}
