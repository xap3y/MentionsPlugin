package eu.xap3y.mentions.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class ConfigurationFileUpdaterTest {

    private static final String DEFAULTS = "# https://plugins.xap3y.eu/mentions#hover-colors\n"
            + "chat:\n"
            + "    enabled: true\n"
            + "    # https://plugins.xap3y.eu/mentions#click-actions\n"
            + "    click:\n"
            + "        enabled: false\n"
            + "        # Suggests a command; never executes it automatically.\n"
            + "        # %target_name%, %target_uuid%: mentioned player\n"
            + "        # %sender_name%, %sender_uuid%: player who sent the mention\n"
            + "        # %clicker_name%, %clicker_uuid%: player viewing/clicking the message\n"
            + "        suggestion: \"/msg %target_name% \"\n"
            + "# https://plugins.xap3y.eu/mentions#everyone-ping\n"
            + "everyone-ping:\n"
            + "    enabled: false\n"
            + "    require-permisison: false # permission is: mentions.everyone\n";

    @Test
    public void missingClickDefaultsToFalseAndIncludesAllComments() {
        String original = "# My settings\nchat:\n  enabled: false # keep this\ncustom: 'value'\n";
        String updated = ConfigurationFileUpdater.merge(original, DEFAULTS);
        YamlConfiguration loaded = load(updated);
        assertFalse(loaded.getBoolean("chat.click.enabled"));
        assertTrue(loaded.contains("chat.click.enabled", true));
        assertFalse(loaded.getBoolean("chat.enabled"));
        assertEquals("value", loaded.getString("custom"));
        assertTrue(updated.contains("# My settings"));
        assertTrue(updated.contains("enabled: false # keep this"));
        assertTrue(updated.contains("# %clicker_name%, %clicker_uuid%: player viewing/clicking the message"));
        assertTrue(updated.contains("require-permisison: false # permission is: mentions.everyone"));
        assertEquals(updated, ConfigurationFileUpdater.merge(updated, DEFAULTS));
    }

    @Test
    public void existingClickTrueIsPreservedAndMissingCommentsAreRestored() {
        String original = "chat:\n  enabled: true\n  click:\n    enabled: true\n    suggestion: '/custom %target_name%' # my command\n";
        String updated = ConfigurationFileUpdater.merge(original, DEFAULTS);
        assertTrue(load(updated).getBoolean("chat.click.enabled"));
        assertEquals("/custom %target_name%", load(updated).getString("chat.click.suggestion"));
        assertTrue(updated.contains("# my command"));
        assertTrue(updated.contains("# Suggests a command; never executes it automatically."));
        assertEquals(updated, ConfigurationFileUpdater.merge(updated, DEFAULTS));
    }

    @Test
    public void commentedOutValuesRemainCommentsAndDoNotBecomeDefaults() {
        String defaults = "group-pings:\n  staff:\n    text: '@staff'\n    # group: staff\n    permission: pings.staff\n";
        String updated = ConfigurationFileUpdater.merge("group-pings:\n  staff:\n    text: '@helpers'\n", defaults);
        assertEquals("@helpers", load(updated).getString("group-pings.staff.text"));
        assertFalse(load(updated).contains("group-pings.staff.group", true));
        assertEquals("pings.staff", load(updated).getString("group-pings.staff.permission"));
        assertTrue(updated.contains("# group: staff"));
        assertEquals(updated, ConfigurationFileUpdater.merge(updated, defaults));
    }

    @Test
    public void preservesMultilineStringsHexQuotesListsAndCrLf() {
        String original = "# custom header\r\nchat:\r\n  enabled: false\r\n  replace: '&#aabbcc#literal' # user note\r\n"
                + "custom: |\r\n  chat:\r\n    this is not a key\r\nlist:\r\n  - '#literal' # list note\r\n";
        String updated = ConfigurationFileUpdater.merge(original, DEFAULTS);
        assertEquals("&#aabbcc#literal", load(updated).getString("chat.replace"));
        assertEquals("chat:\n  this is not a key\n", load(updated).getString("custom"));
        assertTrue(updated.contains("- '#literal' # list note\r\n"));
        assertTrue(updated.contains("# user note\r\n"));
        assertEquals(updated, ConfigurationFileUpdater.merge(updated, DEFAULTS));
    }

    @Test
    public void flowMappingsAndNullValuesReceiveNestedDefaults() {
        String updated = ConfigurationFileUpdater.merge("chat: {click: {enabled: true}} # user note\n", DEFAULTS);
        assertTrue(load(updated).getBoolean("chat.click.enabled"));
        assertEquals("/msg %target_name% ", load(updated).getString("chat.click.suggestion"));
        assertTrue(updated.contains("# user note"));
        assertEquals(updated, ConfigurationFileUpdater.merge(updated, DEFAULTS));
        String nullValue = ConfigurationFileUpdater.merge("chat:\n  click: null # reset\n", DEFAULTS);
        assertFalse(load(nullValue).getBoolean("chat.click.enabled"));
        assertTrue(load(nullValue).contains("chat.click.enabled", true));
        assertTrue(nullValue.contains("# reset"));
    }

    @Test
    public void actualTemplatesRestoreCommentsEvenWhenAllValuesAlreadyExist() throws Exception {
        for (String name : new String[]{"config.yml", "config-cz.yml"}) {
            String defaults = new String(Files.readAllBytes(Paths.get("src/main/resources", name)), StandardCharsets.UTF_8);
            String uncommented = load(defaults).saveToString();
            String updated = ConfigurationFileUpdater.merge(uncommented, defaults);
            assertTrue(updated.contains("https://plugins.xap3y.eu/mentions#click-actions"));
            assertTrue(updated.contains("# %sender_name%, %sender_uuid%:"));
            assertTrue(updated.contains("# permission is: mentions.everyone"));
            assertFalse(load(updated).getBoolean("chat.click.enabled"));
            assertEquals(updated, ConfigurationFileUpdater.merge(updated, defaults));
            assertEquals(defaults, ConfigurationFileUpdater.merge(defaults, defaults));
        }
    }

    @Test
    public void multilineFlowEmptyMappingsAndCommentOnlyFilesAreMigrated() {
        String flow = "# My header\nchat: {\n  click: {enabled: true} # keep note\n}\n";
        String updated = ConfigurationFileUpdater.merge(flow, DEFAULTS);
        assertTrue(load(updated).getBoolean("chat.click.enabled"));
        assertEquals("/msg %target_name% ", load(updated).getString("chat.click.suggestion"));
        assertTrue(updated.contains("# keep note"));
        assertEquals(updated, ConfigurationFileUpdater.merge(updated, DEFAULTS));
        String root = ConfigurationFileUpdater.merge("{} # empty root\n", DEFAULTS);
        assertTrue(load(root).contains("chat.click.enabled", true));
        assertTrue(root.contains("# empty root"));
        String empty = ConfigurationFileUpdater.merge("# My header\n", DEFAULTS);
        assertTrue(empty.startsWith("# My header"));
        assertTrue(load(empty).contains("chat.click.enabled", true));
    }

    @Test
    public void apostrophesInPlainValuesDoNotHideExistingInlineComments() {
        String defaults = "message: Default # built-in note\n";
        String current = "message: Bob's message # my note\n";
        assertEquals(current, ConfigurationFileUpdater.merge(current, defaults));
    }

    @Test
    public void migrationRemovesOldDuplicateLinksAndPlacesGroupLinkAboveMainKey() throws Exception {
        String defaults = new String(Files.readAllBytes(Paths.get("src/main/resources/config.yml")), StandardCharsets.UTF_8);
        String groupLink = "# https://plugins.xap3y.eu/mentions#group-pings";
        String old = defaults.replace("sound:\n", groupLink + "\nsound:\n")
                .replace("    example-staff:", "    " + groupLink + "\n    example-staff:")
                .replace("metrics:", "# https://plugins.xap3y.eu/mentions#preferences\nmetrics:")
                .replace("    hover:", "    # https://plugins.xap3y.eu/mentions#hover-colors\n    hover:");
        String updated = ConfigurationFileUpdater.merge(old, defaults);
        assertEquals(1, updated.split(java.util.regex.Pattern.quote(groupLink), -1).length - 1);
        assertTrue(updated.contains(groupLink + "\ngroup-pings:"));
        assertEquals(defaults, updated);
        assertEquals(updated, ConfigurationFileUpdater.merge(updated, defaults));
    }

    @Test
    public void tutorialUrlsInsideMultilineValuesAreNotRemoved() {
        String current = "message: |\n  # https://plugins.xap3y.eu/mentions#group-pings\n  My message\n";
        String updated = ConfigurationFileUpdater.merge(current, DEFAULTS);
        assertEquals(load(current).getString("message"), load(updated).getString("message"));
    }

    @Test
    public void customGroupPresetsReceiveReplacementDefaultsWithoutChangingExistingValues() {
        String defaults = "everyone-ping:\n  replace: '&a&l%text%&r'\ngroup-pings: {}\n";
        String current = "everyone-ping:\n  text: '$all'\ngroup-pings:\n"
                + "  helpers:\n    text: '@helpers'\n    permission: pings.helpers\n"
                + "  custom:\n    text: '@team'\n    replace: '&#aabbccTEAM' # keep this\n";
        String updated = ConfigurationFileUpdater.merge(current, defaults);
        assertEquals("&a&l%text%&r", load(updated).getString("group-pings.helpers.replace"));
        assertEquals("&a&l%text%&r", load(updated).getString("everyone-ping.replace"));
        assertEquals("$all", load(updated).getString("everyone-ping.text"));
        assertEquals("&#aabbccTEAM", load(updated).getString("group-pings.custom.replace"));
        assertTrue(updated.contains("# keep this"));
        assertEquals(updated, ConfigurationFileUpdater.merge(updated, defaults));
    }

    private static YamlConfiguration load(String yaml) {
        return YamlConfiguration.loadConfiguration(new StringReader(yaml));
    }
}
