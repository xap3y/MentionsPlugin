package eu.xap3y.mentions;

import eu.xap3y.mentions.api.iface.CommandManagerIface;
import eu.xap3y.mentions.commands.RootCommand;
import eu.xap3y.mentions.listeners.ChatListener;
import eu.xap3y.mentions.manager.CommandManager;
import eu.xap3y.mentions.manager.ConfigManager;
import eu.xap3y.mentions.service.Texter;
import eu.xap3y.xagui.XaGui;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public final class Mentions extends JavaPlugin {

    @Getter
    private static Mentions instance;

    @Getter
    private static Texter texter;

    @Getter
    private static XaGui xagui;

    @Override
    public void onEnable() {
        instance = this;

        xagui = new XaGui(this);
        xagui.setCloseButtonSound(Sound.BLOCK_COPPER_DOOR_CLOSE);
        xagui.setRedirectSound(Sound.BLOCK_COPPER_DOOR_OPEN);

        CommandManagerIface<CommandSender> commandManager = new CommandManager(this);
        commandManager.parse(new RootCommand());

        ConfigManager.reloadConfig();

        String prefix = getConfig().getString("prefix");
        if (prefix == null) prefix = "&7[&bmentions&7] &r";
        texter = new Texter(prefix, false, null);

        PluginManager manager = getServer().getPluginManager();
        registerListeners(manager);

        ConfigManager.initStorageConfig();

        org.bukkit.permissions.Permission perm = new org.bukkit.permissions.Permission("mentions.admin", "Allows access to admin commands", org.bukkit.permissions.PermissionDefault.OP);
        Bukkit.getPluginManager().addPermission(perm);
    }

    private static void registerListeners(PluginManager manager) {
        //  Registering listeners  \\
        Listener[] listeners = new Listener[]{
            new ChatListener()
        };

        for (Listener listener : listeners) {
            manager.registerEvents(listener, instance);
        }
    }
}
