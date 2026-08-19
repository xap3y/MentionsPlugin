package eu.xap3y.mentions.adapter;

import org.bukkit.entity.Player;

public interface PlatformAdapter {
    void sendTitle(Player player, String titleText, String subtitle, int fadeIn, int stay, int fadeOut);
    void sendMessage(org.bukkit.command.CommandSender sender, String message);
    void sendActionBar(Player player, String message);
}
