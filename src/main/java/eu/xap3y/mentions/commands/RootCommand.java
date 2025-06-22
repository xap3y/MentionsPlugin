package eu.xap3y.mentions.commands;

import eu.xap3y.mentions.Mentions;
import org.bukkit.command.CommandSender;
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
}
