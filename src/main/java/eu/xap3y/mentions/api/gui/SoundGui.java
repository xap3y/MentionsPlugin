package eu.xap3y.mentions.api.gui;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.manager.ConfigManager;
import eu.xap3y.xagui.VirtualMenu;
import eu.xap3y.xagui.interfaces.GuiMenuInterface;
import eu.xap3y.xagui.models.GuiButton;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SoundGui extends VirtualMenu<Player> {

    public SoundGui() {
        super(Mentions.getInstance().getMessageConfig().getString("sound-gui.title", "&aMention sound"), 4, Mentions.getXagui());
    }

    @Override
    public @NotNull GuiMenuInterface build(@NotNull Player ctx) {

        GuiMenuInterface gui = getGui();

        gui.fillBorder();
        gui.addCloseButton();

        gui.setSlot(4, new GuiButton(Material.JUKEBOX)
                .setName(Mentions.getInstance().getMessageConfig().getString("sound-gui.main_item", "&aSound Choice"))
        );

        gui.setSlot(27, new GuiButton(Material.ARROW)
                .setName(Mentions.getInstance().getMessageConfig().getString("sound-gui.back_button", "&aBack"))
                .withListener(event -> {
                    gui.close(event.getPlayer());
                    new eu.xap3y.mentions.api.gui.SettingGui().build(event.getPlayer()).open(event.getPlayer());
        }));

        String selected = ConfigManager.getSettingSound(ctx.getUniqueId()); // may be null
        if (selected == null) {
            selected = "xp"; // default sound
        }

        // fill from 10 - 16 with sounds and left click play and right lick choose
        List<MentionSound> sounds = getSounds();
        for (int i = 0; i < sounds.size(); i++) {
            MentionSound sound = sounds.get(i);

            List<String> lore = Mentions.getInstance().getMessageConfig().getStringList("sound-gui.choice.lore");
            if (selected != null && selected.equals(sound.name)) {
                lore.add("");
                lore.add(Mentions.getInstance().getMessageConfig().getString("sound-gui.choice.selected", "&aSelected"));
            }

            gui.setSlot(10 + i + (i > 6 ? 2 : 0), new GuiButton(sound.material)
                    .setName(Mentions.getInstance().getMessageConfig().getString("sound-gui.choice.name", "&aSound: &b" + sound.name).replace("{number}", String.valueOf(i + 1)))
                    .setLoreList(lore)
                    .withListener(event -> {
                        if (event.getClick().isLeftClick()) {
                            event.getPlayer().playSound(event.getPlayer().getLocation(), sound.sound, 1, 1);
                        } else if (event.getClick().isRightClick()) {
                            ConfigManager.setSettingSound(event.getPlayer().getUniqueId(), sound.name);
                            ConfigManager.getPlayerSoundMap().put(event.getPlayer().getUniqueId(), sound.sound);
                            Mentions.getInstance().saveConfig();
                            Mentions.getTexter().response(event.getPlayer(), Mentions.getInstance().getMessageConfig().getString("sound-gui.sound-chosen", "&aYou have chosen the sound: &b" + sound.name).replace("{sound}", sound.name));
                            gui.close(event.getPlayer());
                        }
                    }));
        }

        return gui;
    }

    public static List<MentionSound> getSounds() {
        return List.of(
                new MentionSound(Material.NOTE_BLOCK, "button", Sound.BLOCK_WOODEN_BUTTON_CLICK_ON),
                new MentionSound(Material.NOTE_BLOCK, "xp", Sound.ENTITY_EXPERIENCE_ORB_PICKUP),
                new MentionSound(Material.NOTE_BLOCK, "villager", Sound.ENTITY_VILLAGER_AMBIENT),
                new MentionSound(Material.NOTE_BLOCK, "lava_pop", Sound.BLOCK_LAVA_POP),
                new MentionSound(Material.NOTE_BLOCK, "pressure_plate", Sound.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON),
                new MentionSound(Material.NOTE_BLOCK, "ui_click", Sound.UI_BUTTON_CLICK),
                new MentionSound(Material.NOTE_BLOCK, "anvil", Sound.BLOCK_ANVIL_LAND),

                // noteblocks

                new MentionSound(Material.NOTE_BLOCK, "noteblock_bass", Sound.BLOCK_NOTE_BLOCK_BASS),
                new MentionSound(Material.NOTE_BLOCK, "noteblock_bell", Sound.BLOCK_NOTE_BLOCK_BELL),
                new MentionSound(Material.NOTE_BLOCK, "noteblock_chime", Sound.BLOCK_NOTE_BLOCK_CHIME),
                new MentionSound(Material.NOTE_BLOCK, "noteblock_flute", Sound.BLOCK_NOTE_BLOCK_FLUTE),
                new MentionSound(Material.NOTE_BLOCK, "noteblock_guitar", Sound.BLOCK_NOTE_BLOCK_GUITAR),
                new MentionSound(Material.NOTE_BLOCK, "noteblock_hat", Sound.BLOCK_NOTE_BLOCK_HAT),
                new MentionSound(Material.NOTE_BLOCK, "noteblock_pling", Sound.BLOCK_NOTE_BLOCK_PLING)
        );
    }

    public record MentionSound(
            Material material,
            String name,
            Sound sound
    ) { }
}
