package eu.xap3y.mentions.manager;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.api.enums.SettingType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.UUID;

public class ConfigManager {

    @Getter
    @Setter
    private static YamlConfiguration playerConfig;

    private static final File playerConfigYamlFile = new File(Mentions.getInstance().getDataFolder(), "player-settings.yml");

    public static void reloadConfig() {
        if (!Mentions.getInstance().getDataFolder().exists()) {
            Mentions.getInstance().getDataFolder().mkdir();
        }

        Mentions.getInstance().saveDefaultConfig();
        Mentions.getInstance().reloadConfig();
    }

    public static void initStorageConfig() {
        if (!playerConfigYamlFile.exists()) {
            try {
                Mentions.getInstance().saveResource("player-settings.yml", false);
                if (Mentions.getInstance().isFolia()) {
                    Mentions.getInstance().getServer().getGlobalRegionScheduler().runDelayed(Mentions.getInstance(), (e) -> {
                        ConfigManager.initStorageConfig();
                    }, 40L);
                } else {
                    Bukkit.getScheduler().runTaskLater(Mentions.getInstance(), ConfigManager::initStorageConfig, 40L);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        playerConfig = YamlConfiguration.loadConfiguration(playerConfigYamlFile);
    }

    public static void savePlayerConfig() {
        try {
            playerConfig.save(playerConfigYamlFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setSetting(SettingType type, UUID playerUUID, boolean value) {
        String path = playerUUID.toString() + "." + type.name().toLowerCase();
        playerConfig.set(path, value);
        savePlayerConfig();
    }

    public static boolean getSetting(SettingType type, UUID playerUUID) {
        String path = playerUUID.toString() + "." + type.name().toLowerCase();
        return playerConfig.getBoolean(path, true);
    }
}
