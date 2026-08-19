package eu.xap3y.mentions.listeners;

import eu.xap3y.mentions.api.gui.SoundGui;
import eu.xap3y.mentions.manager.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PlayerListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        String soundName = ConfigManager.getSettingSound(event.getPlayer().getUniqueId());
        if (soundName != null) {
            SoundGui.getSounds().forEach(sound -> {
                if (sound.name().equals(soundName)) {
                    ConfigManager.getPlayerSoundMap().put(event.getPlayer().getUniqueId(), sound.sound());
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        ConfigManager.getPlayerSoundMap().remove(event.getPlayer().getUniqueId());
    }
}
