package eu.xap3y.mentions.service;

import eu.xap3y.mentions.Mentions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class VersionCheckService implements Listener {

    private final Mentions plugin;
    private final VersionChecker checker;
    private final ExecutorService executor;
    private volatile CompletableFuture<String> result = CompletableFuture.completedFuture(null);
    private volatile boolean closed;

    public VersionCheckService(Mentions plugin) {
        this.plugin = plugin;
        this.checker = new VersionChecker("xap3y/Mentions/" + plugin.getDescription().getVersion()
                + " (https://github.com/xap3y/MentionsPlugin)");
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "Mentions-version-check");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void check() {
        boolean console = plugin.getConfig().getBoolean("version-check.send-console", true);
        boolean ingame = plugin.getConfig().getBoolean("version-check.send-ingame-op", true);
        if (closed || (!console && !ingame)) {
            result = CompletableFuture.completedFuture(null);
            return;
        }
        String installed = plugin.getDescription().getVersion();
        CompletableFuture<String> pending = CompletableFuture.supplyAsync(() -> checker.check(installed), executor);
        result = pending;
        pending.thenAccept(latest -> {
            if (!closed && result == pending && console && latest != null) {
                plugin.getLogger().info(updateMessage(latest));
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp() || !plugin.getConfig().getBoolean("version-check.send-ingame-op", true)) {
            return;
        }
        long joined = System.nanoTime();
        result.thenAccept(latest -> {
            if (latest == null || closed) {
                return;
            }
            long elapsedTicks = (System.nanoTime() - joined) / 50000000L;
            long delay = Math.max(1L, 20L - elapsedTicks);
            Runnable notify = () -> {
                if (!closed && player.isOnline() && player.isOp()
                        && plugin.getConfig().getBoolean("version-check.send-ingame-op", true)) {
                    player.sendMessage("§8[§aMentions§8] §e" + updateMessage(latest));
                }
            };
            try {
                if (plugin.isFolia()) {
                    Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
                    scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class)
                            .invoke(scheduler, plugin, (Consumer<Object>) task -> notify.run(), null, delay);
                } else {
                    Bukkit.getScheduler().runTaskLater(plugin, notify, delay);
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // The player or plugin may have retired while the HTTP request was running.
            }
        });
    }

    private String updateMessage(String latest) {
        return "Update available: " + latest + " (installed: " + plugin.getDescription().getVersion()
                + "). https://modrinth.com/plugin/chat-mentions";
    }

    public void close() {
        closed = true;
        executor.shutdownNow();
    }
}
