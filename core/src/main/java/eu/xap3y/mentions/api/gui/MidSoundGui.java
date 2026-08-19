package eu.xap3y.mentions.api.gui;

import eu.xap3y.mentions.Mentions;
import eu.xap3y.mentions.manager.ConfigManager;
import eu.xap3y.xagui.VirtualMenu;
import eu.xap3y.xagui.interfaces.GuiMenuInterface;
import eu.xap3y.xagui.models.GuiButton;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MidSoundGui extends VirtualMenu<Player> {

    public MidSoundGui() {
        super(
                Mentions.getInstance()
                        .getMessageConfig()
                        .getString("sound-gui.title", "&aMention sound"),
                4,
                Mentions.getXagui()
        );
    }

    @Override
    public @NotNull GuiMenuInterface build(@NotNull Player ctx) {

        GuiMenuInterface gui = getGui();

        gui.fillBorder();
        gui.addCloseButton();

        gui.setSlot(4, new GuiButton(Material.NOTE_BLOCK)
                .setName(
                        Mentions.getInstance()
                                .getMessageConfig()
                                .getString("sound-gui.main_item", "&aSound Choice")
                )
        );

        gui.setSlot(27, new GuiButton(Material.ARROW)
                .setName(
                        Mentions.getInstance()
                                .getMessageConfig()
                                .getString("sound-gui.back_button", "&aBack")
                )
                .withListener(event -> {
                    gui.close(event.getPlayer());

                    new SettingGui()
                            .build(event.getPlayer())
                            .open(event.getPlayer());
                })
        );

        String selected = ConfigManager.getSettingSound(ctx.getUniqueId());

        if (selected == null) {
            selected = "xp";
        }

        List<SoundGui.MentionSound> sounds = getSounds();

        for (int i = 0; i < sounds.size(); i++) {
            SoundGui.MentionSound sound = sounds.get(i);

            List<String> lore = new ArrayList<>(
                    Mentions.getInstance()
                            .getMessageConfig()
                            .getStringList("sound-gui.choice.lore")
            );

            if (selected.equals(sound.name())) {
                lore.add("");
                lore.add(
                        Mentions.getInstance()
                                .getMessageConfig()
                                .getString("sound-gui.choice.selected", "&aSelected")
                );
            }

            int slot = 10 + i + (i > 6 ? 2 : 0);

            gui.setSlot(slot, new GuiButton(sound.material())
                    .setName(
                            Mentions.getInstance()
                                    .getMessageConfig()
                                    .getString(
                                            "sound-gui.choice.name",
                                            "&aSound: &b" + sound.name()
                                    )
                                    .replace("{number}", String.valueOf(i + 1))
                    )
                    .setLoreList(lore)
                    .withListener(event -> {

                        if (event.getClick().isLeftClick()) {
                            event.getPlayer().playSound(
                                    event.getPlayer().getLocation(),
                                    sound.sound(),
                                    1.0F,
                                    1.0F
                            );

                        } else if (event.getClick().isRightClick()) {

                            ConfigManager.setSettingSound(
                                    event.getPlayer().getUniqueId(),
                                    sound.name()
                            );

                            ConfigManager.getPlayerSoundMap().put(
                                    event.getPlayer().getUniqueId(),
                                    sound.sound()
                            );

                            Mentions.getInstance().saveConfig();

                            Mentions.getTexter().response(
                                    event.getPlayer(),
                                    Mentions.getInstance()
                                            .getMessageConfig()
                                            .getString(
                                                    "sound-gui.sound-chosen",
                                                    "&aYou have chosen the sound: &b" + sound.name()
                                            )
                                            .replace("{sound}", sound.name())
                            );

                            gui.close(event.getPlayer());
                        }
                    })
            );
        }

        return gui;
    }

    public static List<SoundGui.MentionSound> getSounds() {
        List<SoundGui.MentionSound> sounds = new ArrayList<>();

        /*
         * 1.9 - 1.12.2 compatible names
         *
         * We use strings intentionally so this class can still compile
         * against a newer Bukkit/Paper API where some of these enum
         * constants have been renamed.
         */

        addSound(sounds, "button", "BLOCK_WOOD_BUTTON_CLICK_ON");
        addSound(sounds, "xp", "ENTITY_EXPERIENCE_ORB_PICKUP");
        addSound(sounds, "villager", "ENTITY_VILLAGER_AMBIENT");
        addSound(sounds, "lava_pop", "BLOCK_LAVA_POP");
        addSound(sounds, "firework_launch", "ENTITY_FIREWORK_LAUNCH");
        addSound(sounds, "ui_click", "UI_BUTTON_CLICK");
        addSound(sounds, "anvil", "BLOCK_ANVIL_LAND");

        // Noteblocks available throughout 1.9+
        addSound(sounds, "noteblock_bass", "BLOCK_NOTE_BASS");
        addSound(sounds, "noteblock_hat", "BLOCK_NOTE_HAT");
        addSound(sounds, "noteblock_pling", "BLOCK_NOTE_PLING");

        /*
         * These were added later.
         * addSound() simply skips them when running on a version
         * where they don't exist.
         *
         * 1.12+
         */
        addSound(sounds, "noteblock_bell", "BLOCK_NOTE_BELL");
        addSound(sounds, "noteblock_chime", "BLOCK_NOTE_CHIME");
        addSound(sounds, "noteblock_flute", "BLOCK_NOTE_FLUTE");
        addSound(sounds, "noteblock_guitar", "BLOCK_NOTE_GUITAR");

        return sounds;
    }

    private static void addSound(
            List<SoundGui.MentionSound> sounds,
            String name,
            String soundName
    ) {
        try {
            Sound sound = Sound.valueOf(soundName);

            sounds.add(
                    new SoundGui.MentionSound(
                            Material.NOTE_BLOCK,
                            name,
                            sound
                    )
            );

        } catch (IllegalArgumentException ignored) {
            // Sound does not exist on this Minecraft version.
        }
    }
}