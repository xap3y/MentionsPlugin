package eu.xap3y.mentions.manager;

import org.bukkit.configuration.ConfigurationSection;

public final class ConfigurationDefaults {

    private ConfigurationDefaults() {
    }

    public static boolean mergeMissing(ConfigurationSection config, ConfigurationSection defaults) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            Object defaultValue = defaults.get(key);
            // Ignore Bukkit's fallback defaults: the value must actually exist in the file.
            Object currentValue = config.get(key, null);

            if (defaultValue instanceof ConfigurationSection) {
                if (currentValue == null) {
                    currentValue = config.createSection(key);
                    changed = true;
                }
                if (currentValue instanceof ConfigurationSection) {
                    boolean sectionChanged = mergeMissing((ConfigurationSection) currentValue,
                            (ConfigurationSection) defaultValue);
                    changed = changed || sectionChanged;
                }
            } else if (currentValue == null) {
                config.set(key, defaultValue);
                changed = true;
            }
        }
        return changed;
    }
}
