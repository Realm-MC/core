// PASTA: core/src/main/java/br/com/realmmc/core/managers/CooldownManager.java
package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o COOLDOWN (esperar sem se mover) para ações como teleporte.
 */
public class CooldownManager implements Listener {

    private final Main plugin;
    private final Map<UUID, BukkitTask> activeCooldowns = new ConcurrentHashMap<>();

    public CooldownManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void startCooldown(Player player, int seconds, Runnable onComplete, Runnable onCancel) {
        if (player.hasPermission("proxy.champion")) {
            onComplete.run();
            return;
        }

        if (activeCooldowns.containsKey(player.getUniqueId())) {
            return;
        }

        BukkitTask task = new BukkitRunnable() {
            private int countdown = seconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                if (countdown > 0) {
                    // --- ALTERAÇÃO APLICADA AQUI ---
                    String message = CoreAPI.getInstance().getTranslationsManager().getRawMessage("general.cooldown-active-teleport", "time", String.valueOf(countdown));
                    CoreAPI.getInstance().getActionBarManager().setMessage(player, ActionBarManager.MessagePriority.HIGH, "cooldown_teleport", message, 2);
                    CoreAPI.getInstance().getSoundManager().playClick(player);
                    countdown--;
                } else {
                    activeCooldowns.remove(player.getUniqueId());
                    onComplete.run();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        activeCooldowns.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (activeCooldowns.containsKey(player.getUniqueId())) {
            activeCooldowns.remove(player.getUniqueId()).cancel();

            // --- ALTERAÇÃO APLICADA AQUI ---
            String message = CoreAPI.getInstance().getTranslationsManager().getRawMessage("general.cooldown-cancelled-teleport");
            CoreAPI.getInstance().getActionBarManager().setMessage(player, ActionBarManager.MessagePriority.HIGH, "cooldown_teleport", message, 3);
            CoreAPI.getInstance().getSoundManager().playError(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelOnQuit(event.getPlayer());
    }

    public void cancelOnQuit(Player player) {
        if (activeCooldowns.containsKey(player.getUniqueId())) {
            activeCooldowns.remove(player.getUniqueId()).cancel();
        }
    }
}