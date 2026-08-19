package eu.xap3y.mentions.service;

import org.bukkit.entity.Player;

public class TitleService {
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Object chatTitle = net.md_5.bungee.api.chat.TextComponent.fromLegacyText(title)[0];
            Object chatSubtitle = net.md_5.bungee.api.chat.TextComponent.fromLegacyText(subtitle)[0];

            Class<?> chatBaseComponent = Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent");
            Class<?> packetPlayOutTitle = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutTitle");
            Class<?> enumTitleAction = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutTitle$EnumTitleAction");

            Object TIMES = Enum.valueOf((Class<Enum>) enumTitleAction, "TIMES");
            Object TITLE  = Enum.valueOf((Class<Enum>) enumTitleAction, "TITLE");
            Object SUBTITLE = Enum.valueOf((Class<Enum>) enumTitleAction, "SUBTITLE");

            Object timesPacket = packetPlayOutTitle
                    .getConstructor(enumTitleAction, chatBaseComponent, int.class, int.class, int.class)
                    .newInstance(TIMES, null, fadeIn, stay, fadeOut);

            Object titlePacket = packetPlayOutTitle
                    .getConstructor(enumTitleAction, chatBaseComponent)
                    .newInstance(TITLE, chatTitle);

            Object subtitlePacket = packetPlayOutTitle
                    .getConstructor(enumTitleAction, chatBaseComponent)
                    .newInstance(SUBTITLE, chatSubtitle);

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);

            Class<?> packetClass = Class.forName("net.minecraft.server.v1_8_R3.Packet");

            connection.getClass().getMethod("sendPacket", packetClass).invoke(connection, timesPacket);
            connection.getClass().getMethod("sendPacket", packetClass).invoke(connection, titlePacket);
            connection.getClass().getMethod("sendPacket", packetClass).invoke(connection, subtitlePacket);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
