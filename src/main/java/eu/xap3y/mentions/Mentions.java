package eu.xap3y.mentions;

import eu.xap3y.mentions.api.iface.CommandManagerIface;
import eu.xap3y.mentions.commands.RootCommand;
import eu.xap3y.mentions.listeners.ChatListener;
import eu.xap3y.mentions.manager.CommandManager;
import eu.xap3y.mentions.manager.ConfigManager;
import eu.xap3y.mentions.service.Texter;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Mentions extends JavaPlugin {

    @Getter
    private static Mentions instance;

    @Getter
    private static Texter texter;

    @Override
    public void onEnable() {
        instance = this;

        CommandManagerIface<CommandSender> commandManager = new CommandManager(this);
        commandManager.parse(new RootCommand());

        ConfigManager.reloadConfig();

        String prefix = getConfig().getString("prefix");
        if (prefix == null) prefix = "&7[&bmentions&7] &r";
        texter = new Texter(prefix, false, null);

        PluginManager manager = getServer().getPluginManager();
        registerListeners(manager);
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
