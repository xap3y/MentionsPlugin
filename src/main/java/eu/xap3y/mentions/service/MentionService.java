package eu.xap3y.mentions.service;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.adapter.PaperAdapter;
import eu.xap3y.mentions.api.enums.SettingType;
import eu.xap3y.mentions.manager.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MentionService {

    public static void run(Player mentioner, Player mentioned) {
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
            sendTitle(mentioner, mentioned);
        }

        if (useActionBar && userActionPreference) {
            sendActionBar(mentioner, mentioned);
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
            PaperAdapter.sendTitle(target, title, subtitle, fadeIn, stay, fadeOut);
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
            PaperAdapter.sendActionBar(target, actionBar);
        } else {
            try {
                Class.forName("org.bukkit.entity.Player.sendActionBar");
                target.sendActionBar(Texter.colored(actionBar));
            } catch (ClassNotFoundException e) {
                ActionBarService.sendActionbar(target, Texter.colored(actionBar));
            }

        }
    }

    private static void sendSound(Player player, Player target) {
        String sound = Mentions.getInstance().getConfig().getString("sound.sound", "entity.player.levelup");

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
        float volume = (float) Mentions.getInstance().getConfig().getDouble("sound.volume", 1.0);
        float pitch = (float) Mentions.getInstance().getConfig().getDouble("sound.pitch", 1.0);

        target.playSound(target.getLocation(), parsedSound, volume, pitch);
    }
}
