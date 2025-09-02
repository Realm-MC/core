package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.task.CooldownTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager implements Listener {

    private final Main plugin;
    private final Map<UUID, CooldownTask> activeCooldowns = new ConcurrentHashMap<>();

    public CooldownManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void startCooldown(Player player, int seconds, Runnable onFinish, Runnable onCancel) {
        if (hasCooldown(player.getUniqueId())) {
            return;
        }

        CooldownTask task = new CooldownTask(player, seconds, onFinish, onCancel, this);
        task.runTaskTimer(plugin, 0L, 20L);
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
        if (hasCooldown(player.getUniqueId())) {
            cancelCooldown(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (hasCooldown(event.getPlayer().getUniqueId())) {
            cancelCooldown(event.getPlayer().getUniqueId());
        }
    }

    public boolean hasCooldown(UUID uuid) {
        return activeCooldowns.containsKey(uuid);
    }

    public void cancelCooldown(UUID uuid) {
        CooldownTask task = activeCooldowns.get(uuid);
        if (task != null) {
            task.customCancel(); // Usamos um método customizado para diferenciar de cancelamento por fim
        }
    }

    /**
     * MÉTODO ADICIONADO: Usado pela CooldownTask para se remover da lista quando
     * o tempo acaba ou o jogador desloga.
     */
    public void removeCooldown(UUID uuid) {
        activeCooldowns.remove(uuid);
    }
}