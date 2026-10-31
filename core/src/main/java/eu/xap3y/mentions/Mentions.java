package eu.xap3y.mentions;
import eu.xap3y.mentions.adapter.PlatformAdapter;
import eu.xap3y.mentions.adapter.SpigotAdapter;

import eu.xap3y.mentions.api.iface.CommandManagerIface;
import eu.xap3y.mentions.commands.RootCommand;
import eu.xap3y.mentions.listeners.ChatListener;
import eu.xap3y.mentions.manager.CommandManager;
import eu.xap3y.mentions.manager.ConfigManager;
import eu.xap3y.mentions.manager.ConfigurationDefaults;
import eu.xap3y.mentions.service.MentionService;
import eu.xap3y.mentions.service.CompatibilityGuard;
import eu.xap3y.mentions.service.VersionCheckService;
import eu.xap3y.mentions.service.Metrics;
import eu.xap3y.mentions.service.Texter;
import eu.xap3y.xagui.XaGui;
import eu.xap3y.xagui.XaGuiPlugin;
import eu.xap3y.xagui.models.GuiButton;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@SuppressWarnings("UnstableApiUsage")
public final class Mentions extends JavaPlugin {

    @Getter
    private static Mentions instance;

    @Getter
    private static Texter texter;

    @Getter
    private static XaGui xagui;

    @Getter
    private boolean useComponents = false;

    @Getter
    private boolean isFolia = false;

    @Getter
    @Setter
    private YamlConfiguration messageConfig;

    @Getter
    private Metrics metrics;

    @Getter
    private PlatformAdapter platformAdapter;

    @Getter
    private CompatibilityGuard compatibilityGuard;

    @Getter
    private VersionCheckService versionCheckService;

    @Override
    public void onEnable() {
        instance = this;
        compatibilityGuard = new CompatibilityGuard(getLogger());

        if ("@STANDALONE@".equals("true")) {
            xagui = new XaGui(this);
        } else {
            xagui = XaGuiPlugin.getXaGui();
        }

        try {
            Class.forName("net.kyori.adventure.text.Component");
            useComponents = true;
            try {
                platformAdapter = (PlatformAdapter) Class.forName("eu.xap3y.mentions.adapter.PaperAdapter").getDeclaredConstructor().newInstance();
            } catch (Exception | LinkageError e) {
                useComponents = false;
                platformAdapter = new SpigotAdapter();
            }
        } catch (ClassNotFoundException e) {
            useComponents = false;
            platformAdapter = new SpigotAdapter();
        }

        try {
            // check if Sound.BLOCK_WOODEN_DOOR_CLOSE exists (1.16+)
            Sound sound = Sound.valueOf("BLOCK_WOODEN_DOOR_CLOSE");
            xagui.setCloseButtonSound(sound);
        } catch (IllegalArgumentException | IncompatibleClassChangeError e) {
            // fallback to Sound.WOODEN_DOOR_CLOSE for older versions
            //xagui.setCloseButtonSound(Sound.BLOCK_CHEST_CLOSE);
        }

        CommandManagerIface<CommandSender> commandManager = new CommandManager(this);
        commandManager.parse(new RootCommand());

        ConfigManager.reloadConfig();

        String prefix = getConfig().getString("prefix");
        if (prefix == null) prefix = "&7[&bmentions&7] &r";
        texter = new Texter(prefix, false, null);

        PluginManager manager = getServer().getPluginManager();
        registerListeners(manager);

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        ConfigManager.initStorageConfig();

        versionCheckService = new VersionCheckService(this);
        manager.registerEvents(versionCheckService, this);
        versionCheckService.check();

        org.bukkit.permissions.Permission perm = new org.bukkit.permissions.Permission("mentions.admin", "Unlock setting option in GUI to toggle if user can be mentioned or not", org.bukkit.permissions.PermissionDefault.OP);
        Bukkit.getPluginManager().addPermission(perm);


        reloadMessages();
        setGuiCloseButton();

        if (getConfig().getBoolean("metrics", true)) {
            int pluginId = 33042;
            metrics = new Metrics(this, pluginId);

            metrics.addCustomChart(
                new Metrics.SimplePie("use_components", () -> Boolean.toString(useComponents))
            );

            // total mention count
            metrics.addCustomChart(
                new Metrics.SingleLineChart("total_mentions", MentionService::getTotalMentions)
            );
        }

    }

    public void fixConfig() {
        if (getConfig().getDefaults() != null
                && ConfigurationDefaults.mergeMissing(getConfig(), getConfig().getDefaults())) {
            saveConfig();
        }
    }

    @Override
    public void onDisable() {
        if (versionCheckService != null) {
            versionCheckService.close();
        }
    }

    public void reloadMessages() {
        File messageFile = new File(getDataFolder(), "locale.yml");
        if (!messageFile.exists()) {
            saveResource("locale.yml", false);
        }
        File messageFileCz = new File(getDataFolder(), "locale-cz.yml");
        if (!messageFileCz.exists()) {
            saveResource("locale-cz.yml", false);
        }
        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
    }

    public void setGuiCloseButton() {
        GuiButton closeButton = new GuiButton(Material.BARRIER)
                .setName(getMessageConfig().getString("gui.close_button", "&cClose"))
                .withListener((e) -> {
                    e.getWhoClicked().closeInventory();

                    Sound closeSound;
                    try {
                        closeSound = Sound.valueOf("BLOCK_WOODEN_DOOR_CLOSE");
                    } catch (IllegalArgumentException ex) {
                        try {
                            closeSound = Sound.valueOf("DOOR_CLOSE");
                        } catch (IllegalArgumentException exc) {
                            closeSound = null;
                        }
                    }
                    if (closeSound != null) {
                        e.getPlayer().playSound(e.getPlayer().getLocation(), closeSound, 1f, 1f);
                    }
                });

        xagui.setCloseButton(closeButton);
    }

    private static void registerListeners(PluginManager manager) {
        boolean nativeChat = false;
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            Listener paperChat = (Listener) Class.forName("eu.xap3y.mentions.listeners.PaperChatListener")
                    .getDeclaredConstructor().newInstance();
            manager.registerEvents(paperChat, instance);
            nativeChat = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Older servers load only the Java 8 listener, without Adventure dependencies.
        }
        //  Registering listeners  \\
        Listener[] listeners = new Listener[]{
            new ChatListener(nativeChat)
        };

        for (Listener listener : listeners) {
            manager.registerEvents(listener, instance);
        }
    }
}
