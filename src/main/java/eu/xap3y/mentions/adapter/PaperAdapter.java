package eu.xap3y.mentions.adapter;

import eu.xap3y.xagui.GuiMenu;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PaperAdapter {

    public static void setName(ItemMeta meta, String name) {
        meta.displayName(HexUtil.parseText(name));
    }

    public static void setLoreList(ItemMeta meta, List<String> lore) {
        meta.lore(lore.stream().map(HexUtil::parseText).toList());
    }

    public static @NotNull Inventory createInventory(GuiMenu holder, int size, String title) {
        return Bukkit.createInventory(holder, size, HexUtil.parseText(title));
    }

    public static void sendTitle(Player player, String titleText, String subtitle, int fadeIn, int stay, int fadeOut) {
        Title title = Title.title(
                HexUtil.parseText(titleText),
                HexUtil.parseText(subtitle),
                Title.Times.times(
                        java.time.Duration.ofMillis(fadeIn * 50L),
                        java.time.Duration.ofMillis(stay * 50L),
                        java.time.Duration.ofMillis(fadeOut * 50L)
                )
        );
        player.showTitle(title);
    }

    // action bar
    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(HexUtil.parseText(message));
    }
}
