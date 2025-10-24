package eu.xap3y.mentions.commands;

import eu.xap3y.mentions.Mentions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

public class RootCommand {

    @Command("mentions reload")
    @Permission(mode = Permission.Mode.ANY_OF, value = {"mentions.reload", "mentions.*"})
    public void reload(
            CommandSender p0
    ) {
        Mentions.getInstance().reloadConfig();
        Mentions.getTexter().response(p0, "&aReloaded config!");
    }

    @Command("mentions")
    public void gui(
            CommandSender p0
    ) {

        if (!(p0 instanceof Player p1)) {
            Mentions.getTexter().response(p0, "&cThis command can only be executed by a player!");
            return;
        }

        new eu.xap3y.mentions.api.gui.SettingGui().build(p1).open(p1);
    }
}
