package eu.xap3y.mentions.listeners;

import eu.xap3y.mentions.api.gui.SoundGui;
import eu.xap3y.mentions.manager.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class PlayerListener implements Listener {

    private List<SoundGui.MentionSound> getSounds() {
        String serverVer = eu.xap3y.mentions.Mentions.getInstance().getServer().getVersion();
        boolean isOld = serverVer.contains("1.8");

        boolean isMid = false;
        try {
            String mcVersion = serverVer.substring(
                    serverVer.indexOf("MC: ") + 4,
                    serverVer.indexOf(")")
            );
            String[] parts = mcVersion.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            isMid = major == 1 && minor >= 9 && minor <= 12;
        } catch (Exception ignored) {
        }

        if (isOld) {
            return eu.xap3y.mentions.api.gui.LegacySoundGui.getSounds();
        } else if (isMid) {
            return eu.xap3y.mentions.api.gui.MidSoundGui.getSounds();
        } else {
            return eu.xap3y.mentions.api.gui.SoundGui.getSounds();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        String soundName = ConfigManager.getSettingSound(event.getPlayer().getUniqueId());
        if (soundName != null) {
            for (SoundGui.MentionSound sound : getSounds()) {
                if (sound.name().equals(soundName)) {
                    ConfigManager.getPlayerSoundMap().put(event.getPlayer().getUniqueId(), sound.sound());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        ConfigManager.getPlayerSoundMap().remove(event.getPlayer().getUniqueId());
    }
}
