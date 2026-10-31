package eu.xap3y.mentions.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class ConfigurationDefaultsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void addsMissingNestedValueToFileEvenWhenFallbackAlreadyReturnsIt() throws Exception {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("chat.hover.enabled", false);
        defaults.set("chat.hover.text", Arrays.asList("Default tooltip", "Second line"));
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(
                "chat:\n"
                        + "  enabled: true\n"
                        + "  hover:\n"
                        + "    text: Custom tooltip\n"));
        config.setDefaults(defaults);

        assertTrue(config.contains("chat.hover.enabled"));
        assertFalse(config.contains("chat.hover.enabled", true));
        assertTrue(ConfigurationDefaults.mergeMissing(config, defaults));

        File file = temporaryFolder.newFile("config.yml");
        config.save(file);
        YamlConfiguration persisted = YamlConfiguration.loadConfiguration(file);
        assertTrue(persisted.contains("chat.hover.enabled", true));
        assertFalse(persisted.getBoolean("chat.hover.enabled"));
        assertEquals("Custom tooltip", persisted.getString("chat.hover.text"));
        assertTrue(persisted.getBoolean("chat.enabled"));
    }

    @Test
    public void createsMissingSectionsAndCopiesAllNestedLeaves() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("chat.hover.enabled", false);
        defaults.set("chat.hover.text", Arrays.asList("First", "Second"));
        defaults.set("sound.volume", 1.0);
        YamlConfiguration config = new YamlConfiguration();
        config.set("chat.replace", "Custom replacement");

        assertTrue(ConfigurationDefaults.mergeMissing(config, defaults));
        assertTrue(config.contains("chat.hover.enabled", true));
        assertEquals(Arrays.asList("First", "Second"), config.getStringList("chat.hover.text"));
        assertEquals(1.0, config.getDouble("sound.volume"), 0.0);
        assertEquals("Custom replacement", config.getString("chat.replace"));
        assertFalse(ConfigurationDefaults.mergeMissing(config, defaults));
    }

    @Test
    public void preservesFalseZeroEmptyStringsListsAndCustomKeys() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("chat.enabled", true);
        defaults.set("chat.replace", "Default replacement");
        defaults.set("chat.hover.text", Arrays.asList("Default tooltip"));
        defaults.set("sound.volume", 1.0);
        YamlConfiguration config = new YamlConfiguration();
        config.set("chat.enabled", false);
        config.set("chat.replace", "");
        config.set("chat.hover.text", Collections.emptyList());
        config.set("sound.volume", 0);
        config.set("custom.setting", "Keep me");

        assertFalse(ConfigurationDefaults.mergeMissing(config, defaults));
        assertFalse(config.getBoolean("chat.enabled"));
        assertEquals("", config.getString("chat.replace"));
        assertEquals(Collections.emptyList(), config.getStringList("chat.hover.text"));
        assertEquals(0, config.getInt("sound.volume"));
        assertEquals("Keep me", config.getString("custom.setting"));
    }

    @Test
    public void doesNotReplaceExistingScalarWithSection() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("chat.hover.enabled", false);
        YamlConfiguration config = new YamlConfiguration();
        config.set("chat.hover", "Existing custom value");

        assertFalse(ConfigurationDefaults.mergeMissing(config, defaults));
        assertEquals("Existing custom value", config.getString("chat.hover"));
    }
}
