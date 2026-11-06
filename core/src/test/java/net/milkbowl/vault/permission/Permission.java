package net.milkbowl.vault.permission;

import org.bukkit.entity.Player;

/** Minimal test fixture matching the public Vault methods used by the integration. */
public class Permission {

    public boolean permission;
    public boolean group;

    public boolean playerHas(Player player, String node) {
        return permission;
    }

    public boolean playerInGroup(Player player, String name) {
        return group;
    }
}
