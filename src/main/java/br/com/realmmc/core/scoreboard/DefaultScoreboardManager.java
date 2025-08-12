// PASTA: core/src/main/java/br/com/realmmc/core/scoreboard/DefaultScoreboardManager.java
package br.com.realmmc.core.scoreboard;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultScoreboardManager implements Listener {

    private final Main plugin;
    private final Map<UUID, DefaultScoreboardHandler> handlers = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public DefaultScoreboardManager(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            handlers.put(player.getUniqueId(), new DefaultScoreboardHandler(player));
        }
        this.updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateAll, 0L, 40L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        handlers.put(event.getPlayer().getUniqueId(), new DefaultScoreboardHandler(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        DefaultScoreboardHandler handler = handlers.remove(event.getPlayer().getUniqueId());
        if (handler != null) {
            handler.destroy();
        }
    }

    private void updateAll() {
        // Itera sobre todos os handlers da scoreboard
        for (DefaultScoreboardHandler handler : handlers.values()) {
            Player player = handler.getPlayer();
            if (player != null && player.isOnline()) {
                // Para cada jogador, busca o objeto RealmPlayer que já está em cache
                CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(player).ifPresent(realmPlayer -> {
                    // Executa a atualização na thread principal do servidor, passando o objeto com os dados
                    Bukkit.getScheduler().runTask(plugin, () -> handler.update(realmPlayer));
                });
            }
        }
    }
}