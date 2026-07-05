package eu.xap3y.mentions.commands;

import eu.xap3y.mentions.Mentions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;

public class RootCommand {

    @Command("mentions reload")
    @Permission(mode = Permission.Mode.ANY_OF, value = {"mentions.reload", "mentions.*"})
    public void reload(
            Source p0
    ) {
        CommandSender sender = p0.source();
        Mentions.getInstance().reloadConfig();
        Mentions.getInstance().reloadMessages();
        Mentions.getInstance().setGuiCloseButton();
        Mentions.getTexter().setPrefix(Mentions.getInstance().getConfig().getString("prefix", "&7[&bmentions&7] &r"));
        Mentions.getTexter().response(sender, "&aReloaded config!");
    }

    @Command("mentions")
    public void gui(
            Source p0
    ) {
        CommandSender sender = p0.source();

        if (!(sender instanceof Player p1)) {
            Mentions.getTexter().response(sender, "&cThis command can only be executed by a player!");
            return;
        }

        if (!Mentions.getInstance().getConfig().getBoolean("gui-enable", true)) {
            Mentions.getTexter().response(sender, Mentions.getInstance().getMessageConfig().getString("gui-disabled", "&cThe GUI is disabled in the config!"));
            return;
        }

        new eu.xap3y.mentions.api.gui.SettingGui().build(p1).open(p1);
    }
}
