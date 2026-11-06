package eu.xap3y.mentions.service;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultPingService {

    private VaultPingService() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean matches(Player player, ConfigurationSection preset) {
        String permission = preset.getString("permission", "");
        String group = preset.getString("group", "");
        if (permission.isEmpty() && group.isEmpty()) {
            return false;
        }
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                return false;
            }
            Class api = Class.forName("net.milkbowl.vault.permission.Permission");
            RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration(api);
            if (registration == null) {
                return false;
            }
            Object provider = registration.getProvider();
            boolean hasPermission = permission.isEmpty() || (Boolean) api
                    .getMethod("playerHas", Player.class, String.class).invoke(provider, player, permission);
            boolean hasGroup = group.isEmpty() || (Boolean) api
                    .getMethod("playerInGroup", Player.class, String.class).invoke(provider, player, group);
            return hasPermission && hasGroup;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }
}
