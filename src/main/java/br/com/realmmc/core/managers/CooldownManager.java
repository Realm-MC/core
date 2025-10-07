package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.task.CooldownTask;
import org.bukkit.Bukkit; // IMPORT ADICIONADO
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
            // Apenas remove a tarefa, sem enviar mensagem de cancelamento
            CooldownTask task = activeCooldowns.remove(event.getPlayer().getUniqueId());
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
    }

    public boolean hasCooldown(UUID uuid) {
        return activeCooldowns.containsKey(uuid);
    }

    public void cancelCooldown(UUID uuid) {
        CooldownTask task = activeCooldowns.get(uuid);
        if (task != null) {
            // --- LÓGICA DE NOTIFICAÇÃO ADICIONADA AQUI ---
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Pega a mensagem do arquivo de tradução do Core
                String message = CoreAPI.getInstance().getTranslationsManager().getMessage("general.cooldown-cancelled-teleport");
                // Envia a mensagem para a ActionBar do jogador
                CoreAPI.getInstance().getActionBarManager().setMessage(player, ActionBarManager.MessagePriority.HIGH, "teleport_cancelled", message, 3);
                // Toca o som de erro
                CoreAPI.getInstance().getSoundManager().playError(player);
            }
            // --- FIM DA NOVA LÓGICA ---

            task.customCancel(); // Continua chamando o método para executar o onCancel e cancelar a tarefa
        }
    }

    public void removeCooldown(UUID uuid) {
        activeCooldowns.remove(uuid);
    }
}