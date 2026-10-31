package eu.xap3y.mentions.manager;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.api.enums.SettingType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConfigManager {

    @Getter
    @Setter
    private static YamlConfiguration playerConfig;

    @Getter
    private static final Map<UUID, Sound> playerSoundMap = new HashMap<>();

    private static final File playerConfigYamlFile = new File(Mentions.getInstance().getDataFolder(), "player-settings.yml");

    public static void reloadConfig() {
        if (!Mentions.getInstance().getDataFolder().exists()) {
            Mentions.getInstance().getDataFolder().mkdir();
        }

        Mentions.getInstance().saveDefaultConfig();
        Mentions.getInstance().reloadConfig();
        Mentions.getInstance().fixConfig();
    }

    public static void initStorageConfig() {
        if (!playerConfigYamlFile.exists()) {
            try {
                Mentions.getInstance().saveResource("player-settings.yml", false);
                if (Mentions.getInstance().isFolia()) {
                    try {
                        Object scheduler = Mentions.getInstance().getServer().getClass().getMethod("getGlobalRegionScheduler").invoke(Mentions.getInstance().getServer());
                        scheduler.getClass().getMethod("runDelayed", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class)
                                .invoke(scheduler, Mentions.getInstance(), (java.util.function.Consumer<?>) (task) -> ConfigManager.initStorageConfig(), 40L);
                    } catch (Exception ex) {
                        Bukkit.getScheduler().runTaskLater(Mentions.getInstance(), ConfigManager::initStorageConfig, 40L);
                    }
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

    public static void setSettingSound(UUID playerUUID, String soundName) {
        String path = playerUUID.toString() + ".sound";
        playerConfig.set(path, soundName);
        savePlayerConfig();
    }

    @Nullable
    public static String getSettingSound(UUID playerUUID) {
        String path = playerUUID.toString() + ".sound";
        return playerConfig.getString(path);
    }

    public static boolean getSetting(SettingType type, UUID playerUUID) {
        String path = playerUUID.toString() + "." + type.name().toLowerCase();
        return playerConfig.getBoolean(path, true);
    }

    public static boolean isAbleToBeMentionedFromConfig(String playerName) {
        boolean enabled = Mentions.getInstance().getConfig().getBoolean("exclusion.enabled", false);
        if (!enabled) return true;

        boolean caseSensitive = Mentions.getInstance().getConfig().getBoolean("exclusion.case-sensitive", false);

        List<String> excludedPlayers = Mentions.getInstance().getConfig().getStringList("exclusion.players");

        if (caseSensitive) {
            return !excludedPlayers.contains(playerName);
        } else {
            return excludedPlayers.stream().noneMatch(p -> p.equalsIgnoreCase(playerName));
        }
    }
}
