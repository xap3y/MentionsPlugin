package eu.xap3y.mentions.adapter;

import eu.xap3y.xagui.GuiMenu;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PaperAdapter implements PlatformAdapter {

    public void setName(ItemMeta meta, String name) {
        meta.displayName(HexUtil.parseText(name));
    }

    public void setLoreList(ItemMeta meta, List<String> lore) {
        meta.lore(lore.stream().map(HexUtil::parseText).toList());
    }

    public @NotNull Inventory createInventory(GuiMenu holder, int size, String title) {
        return Bukkit.createInventory(holder, size, HexUtil.parseText(title));
    }

    @Override
    public void sendMessage(org.bukkit.command.CommandSender sender, String message) {
        sender.sendMessage(HexUtil.parseText(message));
    }
    public void sendTitle(Player player, String titleText, String subtitle, int fadeIn, int stay, int fadeOut) {
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
    public void sendActionBar(Player player, String message) {
        player.sendActionBar(HexUtil.parseText(message));
    }
}
