package eu.xap3y.mentions.service;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.api.enums.SettingType;
import eu.xap3y.mentions.manager.ConfigManager;
import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MentionService {

    @Getter
    private static int totalMentions = 0;

    public static void run(Player mentioner, Player mentioned) {

        incrementTotalMentions();

        FileConfiguration cfg = Mentions.getInstance().getConfig();

        boolean useSound = cfg.getBoolean("sound.enabled", false);
        boolean userSoundPreference = ConfigManager.getSetting(SettingType.SOUND, mentioned.getUniqueId());

        boolean useTitle = cfg.getBoolean("title.enabled", false);
        boolean userTitlePreference = ConfigManager.getSetting(SettingType.TITLE, mentioned.getUniqueId());

        boolean useActionBar = cfg.getBoolean("actionbar.enabled", false);
        boolean userActionPreference = ConfigManager.getSetting(SettingType.ACTIONBAR, mentioned.getUniqueId());

        if (useSound && userSoundPreference) {
            sendSound(mentioner, mentioned);
        }

        if (useTitle && userTitlePreference) {
            Mentions.getInstance().getCompatibilityGuard().run("Mention titles",
                    () -> sendTitle(mentioner, mentioned));
        }

        if (useActionBar && userActionPreference) {
            Mentions.getInstance().getCompatibilityGuard().run("Mention action bar",
                    () -> sendActionBar(mentioner, mentioned));
        }
    }

    private static void sendTitle(Player player, Player target) {
        String title = Mentions.getInstance().getConfig().getString("title.title", "");
        String subtitle = Mentions.getInstance().getConfig().getString("title.subtitle", "");

        int fadeIn = Mentions.getInstance().getConfig().getInt("title.fade-in", 10);
        int stay = Mentions.getInstance().getConfig().getInt("title.stay", 70);
        int fadeOut = Mentions.getInstance().getConfig().getInt("title.fade-out", 20);

        title = title.replaceAll("%target%", target.getName());
        title = title.replaceAll("%player%", player.getName());
        subtitle = subtitle.replaceAll("%target%", target.getName());
        subtitle = subtitle.replaceAll("%player%", player.getName());

        if (Mentions.getInstance().isUseComponents()) {
            Mentions.getInstance().getPlatformAdapter().sendTitle(target, title, subtitle, fadeIn, stay, fadeOut);
        } else {
            try {
                Class.forName("org.bukkit.entity.Player.sendTitle");
                target.sendTitle(Texter.colored(title), Texter.colored(subtitle), fadeIn, stay, fadeOut);
            } catch (ClassNotFoundException e) {
                target.sendTitle(Texter.colored(title), Texter.colored(subtitle));
            }
        }
    }

    private static void sendActionBar(Player player, Player target) {
        String actionBar = Mentions.getInstance().getConfig().getString("actionbar.message", "");

        if (actionBar.isEmpty()) {
            return;
        }

        actionBar = actionBar.replace("%target%", target.getName());
        actionBar = actionBar.replace("%player%", player.getName());

        if (Mentions.getInstance().isUseComponents()) {
            Mentions.getInstance().getPlatformAdapter().sendActionBar(target, actionBar);
        } else {
            try {
                Class.forName("org.bukkit.entity.Player.sendActionBar");
                Mentions.getInstance().getPlatformAdapter().sendActionBar(target, Texter.colored(actionBar));
            } catch (ClassNotFoundException e) {
                ActionBarService.sendActionbar(target, Texter.colored(actionBar));
            }

        }
    }

    private static void sendSound(Player player, Player target) {

        float volume = (float) Mentions.getInstance().getConfig().getDouble("sound.volume", 1.0);
        float pitch = (float) Mentions.getInstance().getConfig().getDouble("sound.pitch", 1.0);

        if (ConfigManager.getPlayerSoundMap().get(target.getUniqueId()) != null) {
            target.playSound(target.getLocation(), ConfigManager.getPlayerSoundMap().get(target.getUniqueId()), volume, pitch);
            return;
        }

        String sound = Mentions.getInstance().getConfig().getString("sound.sound", "LAVA_POP");

        String serverVer = Mentions.getInstance().getServer().getVersion();
        boolean isLegacy = serverVer.contains("1.8");

        if (isLegacy) {
            switch (sound) {
                case "ENTITY_EXPERIENCE_ORB_PICKUP":
                    sound = "ORB_PICKUP";
                    break;
                case "ENTITY_PLAYER_LEVELUP":
                    sound = "LEVEL_UP";
                    break;
                case "ENTITY_ITEM_PICKUP":
                    sound = "ITEM_PICKUP";
                    break;
                case "ENTITY_ARROW_HIT":
                    sound = "ARROW_HIT";
                    break;
                case "ENTITY_ARROW_SHOOT":
                    sound = "SHOOT_ARROW";
                    break;
                case "ENTITY_FIREWORK_ROCKET_LAUNCH":
                    sound = "FIREWORK_LAUNCH";
                    break;
                case "ENTITY_FIREWORK_ROCKET_BLAST":
                    sound = "FIREWORK_BLAST";
                    break;
                case "ENTITY_FIREWORK_ROCKET_TWINKLE":
                    sound = "FIREWORK_TWINKLE";
                    break;
                default:
                    // do nothing, use the original sound
            }
        }

        Sound parsedSound;
        try {
            parsedSound = Sound.valueOf(sound.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // default 1.8.8 sound
            try {
                parsedSound = Sound.valueOf("LEVEL_UP");
            } catch (IllegalArgumentException ex) {
                return;
            }
        }

        target.playSound(target.getLocation(), parsedSound, volume, pitch);
    }

    private static void incrementTotalMentions() {
        totalMentions++;
    }
}
