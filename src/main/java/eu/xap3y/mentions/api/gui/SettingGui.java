package eu.xap3y.mentions.api.gui;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.api.enums.SettingType;
import eu.xap3y.mentions.manager.ConfigManager;
import eu.xap3y.xagui.VirtualMenu;
import eu.xap3y.xagui.interfaces.GuiMenuInterface;
import eu.xap3y.xagui.models.GuiButton;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class SettingGui extends VirtualMenu<Player> {

    public SettingGui() {
        super(Mentions.getInstance().getMessageConfig().getString("gui.title", "&cMentions"), 6, 1, Mentions.getXagui());
    }

    @Override
    public @NotNull GuiMenuInterface build(Player player) {

        GuiMenuInterface gui = getGui();

        gui.fillBorder();
        gui.addCloseButton();

        gui.setSlot(4, new GuiButton(Material.NAME_TAG).setName(Mentions.getInstance().getMessageConfig().getString("gui.main_item", "&a&lMentions Settings")));

        // 20, 22, 24
        // 29, 31, 33
        List<String> soundLore = Mentions.getInstance().getMessageConfig().getStringList("gui.sound_button.lore");
        gui.setSlot(20, new GuiButton(Material.NOTE_BLOCK).setName(Mentions.getInstance().getMessageConfig().getString("gui.sound_button.name", "&aSound notification")).setLoreList(soundLore));

        List<String> titleLore = Mentions.getInstance().getMessageConfig().getStringList("gui.title_button.lore");
        Material signMaterial;
        try {
            signMaterial = Material.valueOf("OAK_SIGN");
        } catch (IllegalArgumentException e) {
            signMaterial = Material.valueOf("SIGN");
        }
        gui.setSlot(22, new GuiButton(signMaterial).setName(Mentions.getInstance().getMessageConfig().getString("gui.title_button.name", "&aTitle")).setLoreList(titleLore));

        List<String> actionBarLore = Mentions.getInstance().getMessageConfig().getStringList("gui.actionbar_button.lore");
        gui.setSlot(24, new GuiButton(Material.PAINTING).setName(Mentions.getInstance().getMessageConfig().getString("gui.actionbar_button.name", "&aAction Bar")).setLoreList(actionBarLore));

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
            List<String> adminLore = Mentions.getInstance().getMessageConfig().getStringList("gui.admin_button.lore");

            Material redstoneTorchMaterial;
            try {
                redstoneTorchMaterial = Material.valueOf("REDSTONE_TORCH");
            } catch (IllegalArgumentException e) {
                redstoneTorchMaterial = Material.valueOf("REDSTONE_TORCH_ON");
            }
            gui.setSlot(25, new GuiButton(redstoneTorchMaterial).setName(Mentions.getInstance().getMessageConfig().getString("gui.admin_button.name", "&aOption to be mentioned")).setLoreList(adminLore));

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
        ItemStack material = isEnabled ? getLimeDye() : getGrayDye();
        String status = isEnabled ? Mentions.getInstance().getMessageConfig().getString("gui.on_text", "&aEnabled") : Mentions.getInstance().getMessageConfig().getString("gui.off_text", "&cDisabled");

        Sound clickSound;
        try {
            clickSound = Sound.valueOf("BLOCK_LAVA_POP");
        } catch (IllegalArgumentException e) {
            try {
                clickSound = Sound.valueOf("CLICK");
            } catch (IllegalArgumentException ex) {
                clickSound = Sound.valueOf("UI_BUTTON_CLICK");
            }
        }
        Sound finalClickSound = clickSound;
        return new GuiButton(material)
                .setName(status)
                .setLore(" ", Mentions.getInstance().getMessageConfig().getString("gui.click_to_toggle", "&eClick to toggle"))
                .withListener((e) -> {
                    boolean newValue = !isEnabled;
                    callBack.accept(newValue);
                    e.getWhoClicked().closeInventory();
                    this.build(e.getPlayer()).open(e.getPlayer());
                    e.getPlayer().playSound(e.getPlayer().getLocation(), finalClickSound, 1f, 1f);
                });
    }

    private ItemStack getLimeDye() {
        try {
            Material mat = Material.getMaterial("LIME_DYE");
            if (mat == null) {
                throw new ClassNotFoundException();
            }
            return new ItemStack(mat);
        } catch (ClassNotFoundException e) {
            Material mat = Material.getMaterial("INK_SACK");
            if (mat == null) {
                return new ItemStack(Material.LAVA_BUCKET);
            }
            return new ItemStack(mat, 1, (short) 10);
        }
    }

    private ItemStack getGrayDye() {
        try {
            Material mat = Material.getMaterial("GRAY_DYE");
            if (mat == null) {
                throw new ClassNotFoundException();
            }
            return new ItemStack(mat);
        } catch (ClassNotFoundException e) {
            Material mat = Material.getMaterial("INK_SACK");
            if (mat == null) {
                return new ItemStack(Material.BUCKET);
            }
            return new ItemStack(mat, 1, (short) 8);
        }
    }
}
