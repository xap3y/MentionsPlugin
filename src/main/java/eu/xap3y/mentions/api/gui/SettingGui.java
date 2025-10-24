package eu.xap3y.mentions.api.gui;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.api.enums.SettingType;
import eu.xap3y.mentions.manager.ConfigManager;
import eu.xap3y.xagui.VirtualMenu;
import eu.xap3y.xagui.interfaces.GuiInterface;
import eu.xap3y.xagui.models.GuiButton;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class SettingGui extends VirtualMenu<Player> {

    public SettingGui() {
        super("&cMentions", 6, 1, Mentions.getXagui());
    }

    @Override
    public @Nullable GuiInterface build(Player player) {

        GuiInterface gui = getGui();

        gui.fillBorder();
        gui.addCloseButton();

        gui.setSlot(4, new GuiButton(Material.NAME_TAG).setName("&a&lMentions Nastavení"));

        // 20, 22, 24
        // 29, 31, 33
        gui.setSlot(20, new GuiButton(Material.NOTE_BLOCK).setName("&aZvuk upozornění").setLore(
                "&7Zapnout/vypnout zvukové upozornění",
                "&7při zmínce ve chatu."
        ));

        gui.setSlot(22, new GuiButton(Material.OAK_SIGN).setName("&aTitle").setLore(
                "&7Zapnout/vypnout zobrazování title",
                "&7při zmínce ve chatu."
        ));

        gui.setSlot(24, new GuiButton(Material.PAINTING).setName("&aActionBar").setLore(
                "&7Zapnout/vypnout ActionBar upozornění",
                "&7při zmínce ve chatu."
        ));

        gui.setSlot(29, createToggleButton(
                ConfigManager.getSetting(SettingType.SOUND, player.getUniqueId()),
                (newValue) -> {
                    ConfigManager.setSetting(SettingType.SOUND, player.getUniqueId(), newValue);
                }
        ));

        gui.setSlot(31, createToggleButton(
                ConfigManager.getSetting(SettingType.TITLE, player.getUniqueId()),
                (newValue) -> {
                    ConfigManager.setSetting(SettingType.TITLE, player.getUniqueId(), newValue);
                }
        ));

        gui.setSlot(33, createToggleButton(
                ConfigManager.getSetting(SettingType.ACTIONBAR, player.getUniqueId()),
                (newValue) -> {
                    ConfigManager.setSetting(SettingType.ACTIONBAR, player.getUniqueId(), newValue);
                }
        ));

        if (player.hasPermission("mentions.admin")) {
            gui.setSlot(25, new GuiButton(Material.REDSTONE_TORCH).setName("&cMožnost být zmíněn").setLore(
                    "&7Zapnout/vypnout možnost být",
                    "&7zmíněn ostatními hráči.",
                    "&8(Jen pro adminy)"
            ));

            gui.setSlot(34, createToggleButton(
                    ConfigManager.getSetting(SettingType.ABLE_TO_MENTION, player.getUniqueId()),
                    (newValue) -> {
                        ConfigManager.setSetting(SettingType.ABLE_TO_MENTION, player.getUniqueId(), newValue);
                    }
            ));
        }

        return gui;
    }

    private GuiButton createToggleButton(boolean isEnabled, Consumer<Boolean> callBack) {
        Material material = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = isEnabled ? "&aZapnuto" : "&cVypnuto";

        return new GuiButton(material)
                .setName(status)
                .setLore(" ", "&eKlikni pro přepnutí!")
                .withClickSound(Sound.BLOCK_LAVA_POP)
                .withListener((e) -> {
                    boolean newValue = !isEnabled;
                    callBack.accept(newValue);
                    e.getWhoClicked().closeInventory();
                    Player p = (Player) e.getWhoClicked();
                    this.build(p).open(p);
                });
    }
}
