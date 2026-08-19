package eu.xap3y.mentions.adapter;

import eu.xap3y.mentions.service.ActionBarService;
import eu.xap3y.mentions.service.TitleService;
import org.bukkit.entity.Player;

public class SpigotAdapter implements PlatformAdapter {

    
    @Override
    
    public void sendMessage(org.bukkit.command.CommandSender sender, String message) {
        sender.sendMessage(message);
    }
    public void sendTitle(Player player, String titleText, String subtitle, int fadeIn, int stay, int fadeOut) {
        TitleService.sendTitle(player, titleText, subtitle, fadeIn, stay, fadeOut);
    }

    
    public void sendActionBar(Player player, String message) {
        ActionBarService.sendActionbar(player, message);
    }
}
