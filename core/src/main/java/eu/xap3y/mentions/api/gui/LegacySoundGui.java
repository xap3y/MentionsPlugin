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

public class LegacySoundGui extends VirtualMenu<Player> {

    public LegacySoundGui() {
        super(
                Mentions.getInstance().getMessageConfig().getString(
                        "sound-gui.title",
                        "&aMention sound"
                ),
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
                .setName(Mentions.getInstance().getMessageConfig().getString(
                        "sound-gui.main_item",
                        "&aSound Choice"
                ))
        );

        gui.setSlot(27, new GuiButton(Material.ARROW)
                .setName(Mentions.getInstance().getMessageConfig().getString(
                        "sound-gui.back_button",
                        "&aBack"
                ))
                .withListener(event -> {
                    gui.close(event.getPlayer());
                    new SettingGui()
                            .build(event.getPlayer())
                            .open(event.getPlayer());
                }));

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
                lore.add(Mentions.getInstance()
                        .getMessageConfig()
                        .getString(
                                "sound-gui.choice.selected",
                                "&aSelected"
                        ));
            }

            int slot = 10 + i + (i > 6 ? 2 : 0);

            gui.setSlot(slot, new GuiButton(sound.material())
                    .setName(Mentions.getInstance()
                            .getMessageConfig()
                            .getString(
                                    "sound-gui.choice.name",
                                    "&aSound: &b" + sound.name()
                            )
                            .replace("{number}", String.valueOf(i + 1)))
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
                    }));
        }

        return gui;
    }

    public static List<SoundGui.MentionSound> getSounds() {
        List<SoundGui.MentionSound> sounds = new ArrayList<>();

        addSound(sounds, "level_up", "LEVEL_UP");
        addSound(sounds, "xp", "ORB_PICKUP");
        addSound(sounds, "lava_pop", "LAVA_POP");
        addSound(sounds, "anvil", "ANVIL_LAND");
        addSound(sounds, "fizz", "FIZZ");
        addSound(sounds, "wood_click", "WOOD_CLICK");
        addSound(sounds, "villager", "VILLAGER_HAGGLE");

        // Noteblocks
        addSound(sounds, "noteblock_bass", "NOTE_BASS");
        addSound(sounds, "noteblock_bass_drum", "NOTE_BASS_DRUM");
        addSound(sounds, "noteblock_bass_guitar", "NOTE_BASS_GUITAR");
        addSound(sounds, "noteblock_piano", "NOTE_PIANO");
        addSound(sounds, "noteblock_pling", "NOTE_PLING");
        addSound(sounds, "noteblock_snare", "NOTE_SNARE_DRUM");
        addSound(sounds, "noteblock_sticks", "NOTE_STICKS");

        return sounds;
    }

    private static void addSound(
            List<SoundGui.MentionSound> sounds,
            String name,
            String soundName
    ) {
        try {
            Sound sound = Sound.valueOf(soundName);

            sounds.add(new SoundGui.MentionSound(
                    Material.NOTE_BLOCK,
                    name,
                    sound
            ));
        } catch (IllegalArgumentException ignored) {
            Mentions.getInstance()
                    .getLogger()
                    .warning("Sound '" + soundName + "' is not available on this server version.");
        }
    }
}