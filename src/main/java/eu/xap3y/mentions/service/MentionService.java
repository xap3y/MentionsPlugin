package eu.xap3y.mentions.service;

import eu.xap3y.mentions.Mentions;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MentionService {

    public static void run(Player mentioner, Player mentioned) {
        FileConfiguration cfg = Mentions.getInstance().getConfig();

        boolean useSound = cfg.getBoolean("sound.enabled", false);

        boolean useTitle = cfg.getBoolean("title.enabled", false);

        boolean useActionBar = cfg.getBoolean("actionbar.enabled", false);

        if (useSound) {
            sendSound(mentioner, mentioned);
        }

        if (useTitle) {
            sendTitle(mentioner, mentioned);
        }

        if (useActionBar) {
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

        target.sendTitle(Texter.colored(title), Texter.colored(subtitle), fadeIn, stay, fadeOut);
    }

    private static void sendActionBar(Player player, Player target) {
        String actionBar = Mentions.getInstance().getConfig().getString("actionbar.message", "");

        if (actionBar.isEmpty()) {
            return;
        }

        actionBar = actionBar.replace("%target%", target.getName());
        actionBar = actionBar.replace("%player%", player.getName());

        target.sendActionBar(Texter.colored(actionBar));

        int delay = Mentions.getInstance().getConfig().getInt("actionbar.delay", 20);

        if (delay > 0) {
            player.getServer().getScheduler().runTaskLater(Mentions.getInstance(), () -> {
                player.sendActionBar(Component.empty());
            }, delay);
        }
    }

    private static void sendSound(Player player, Player target) {
        String sound = Mentions.getInstance().getConfig().getString("sound.sound", "entity.player.levelup");

        Sound parsedSound;
        try {
            parsedSound = Sound.valueOf(sound.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return;
        }
        float volume = (float) Mentions.getInstance().getConfig().getDouble("sound.volume", 1.0);
        float pitch = (float) Mentions.getInstance().getConfig().getDouble("sound.pitch", 1.0);

        target.playSound(target, parsedSound, volume, pitch);
    }
}
