package eu.xap3y.mentions.service;

import eu.xap3y.mentions.api.dto.ChatMention;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class EveryonePermissionTest {

    @Test
    public void senderPermissionOnlyGatesEveryoneWhenExplicitlyRequired() throws Exception {
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object original = field.get(null);
        AtomicInteger scans = new AtomicInteger();
        Server server = (Server) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getOnlinePlayers")) {
                        scans.incrementAndGet();
                        return Collections.emptyList();
                    }
                    return null;
                });
        boolean[] permitted = {false};
        Player sender = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("hasPermission")) {
                        assertEquals("mentions.everyone", args[0]);
                        return permitted[0];
                    }
                    return null;
                });
        Method ping = ChatMentionService.class.getDeclaredMethod("addPing", List.class, Player.class,
                String.class, ConfigurationSection.class, boolean.class);
        ping.setAccessible(true);
        YamlConfiguration preset = new YamlConfiguration();
        preset.set("enabled", true);
        preset.set("text", "@everyone");
        try {
            field.set(null, server);
            ping.invoke(null, new ArrayList<ChatMention>(), sender, "@everyone Hi", preset, true);
            assertEquals(1, scans.get());
            preset.set("require-permisison", true);
            ping.invoke(null, new ArrayList<ChatMention>(), sender, "@everyone Hi", preset, true);
            assertEquals(1, scans.get());
            permitted[0] = true;
            ping.invoke(null, new ArrayList<ChatMention>(), sender, "@everyone Hi", preset, true);
            assertEquals(2, scans.get());
        } finally {
            field.set(null, original);
        }
    }
}
