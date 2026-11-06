package eu.xap3y.mentions.service;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.SimpleServicesManager;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.Assert.*;

public class PingFeaturesTest {

    @Test
    public void pingReplacementUsesConfiguredTextAndKeepsColorMarkupForTheRenderer() {
        YamlConfiguration preset = new YamlConfiguration();
        assertEquals("&a&l$all&r", ChatMentionService.pingReplacement(preset, "$all"));
        preset.set("replace", "<gradient:#ff0000:#0000ff>STAFF</gradient>");
        assertEquals("<gradient:#ff0000:#0000ff>STAFF</gradient>",
                ChatMentionService.pingReplacement(preset, "@staff"));
        preset.set("replace", "&#aabbcc%text%");
        assertEquals("&#aabbcc@staff", ChatMentionService.pingReplacement(preset, "@staff"));
    }

    @Test
    public void clickSuggestionResolvesAllSixVariablesForEachViewer() {
        Player sender = player("Sender", 1);
        Player target = player("Target", 2);
        Player viewer = player("Viewer", 3);
        String template = "%target_name% %target_uuid% %sender_name% %sender_uuid% %clicker_name% %clicker_uuid%";
        String command = ChatMentionService.clickVariables(template, sender, target, viewer);
        assertEquals("Target " + target.getUniqueId() + " Sender " + sender.getUniqueId()
                + " Viewer " + viewer.getUniqueId(), command);
        assertEquals("/msg Target ", ChatMentionService.clickVariables("/msg %target_name% ", sender, target, viewer));
        assertEquals("Other", ChatMentionService.clickVariables("%clicker_name%", sender, target, player("Other", 4)));
    }

    @Test
    public void configurableTokensAreLiteralAndRespectBoundaries() {
        assertTrue(ChatMentionService.mentionPattern("", "$all").matcher("Hello $all!").find());
        assertTrue(ChatMentionService.mentionPattern("", "@staff").matcher("@STAFF help").find());
        assertFalse(ChatMentionService.mentionPattern("", "@staff").matcher("@staffing").find());
        assertFalse(ChatMentionService.mentionPattern("", "@everyone").matcher("mail@everyone").find());
    }

    @Test
    public void vaultSupportsPermissionGroupAndCombinedRequirementsAndMissingProvider() throws Exception {
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        Object original = field.get(null);
        SimpleServicesManager services = new SimpleServicesManager();
        boolean[] vaultEnabled = {true};
        PluginManager manager = (PluginManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{PluginManager.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isPluginEnabled")) {
                        return vaultEnabled[0];
                    }
                    return null;
                });
        Server server = (Server) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getPluginManager")) {
                        return manager;
                    }
                    if (method.getName().equals("getServicesManager")) {
                        return services;
                    }
                    return null;
                });
        try {
            field.set(null, server);
            YamlConfiguration preset = new YamlConfiguration();
            preset.set("permission", "pings.staff");
            Player player = player("Staff", 5);
            assertFalse(VaultPingService.matches(player, preset));
            Permission provider = new Permission();
            services.register(Permission.class, provider, null, ServicePriority.Normal);
            assertFalse(VaultPingService.matches(player, preset));
            provider.permission = true;
            assertTrue(VaultPingService.matches(player, preset));
            preset.set("group", "staff");
            assertFalse(VaultPingService.matches(player, preset));
            provider.group = true;
            assertTrue(VaultPingService.matches(player, preset));
            preset.set("permission", null);
            provider.permission = false;
            assertTrue(VaultPingService.matches(player, preset));
            vaultEnabled[0] = false;
            assertFalse(VaultPingService.matches(player, preset));
        } finally {
            field.set(null, original);
        }
    }

    private static Player player(String name, long id) {
        return (Player) Proxy.newProxyInstance(PingFeaturesTest.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) {
                        return name;
                    }
                    if (method.getName().equals("getUniqueId")) {
                        return new UUID(0, id);
                    }
                    return null;
                });
    }
}
