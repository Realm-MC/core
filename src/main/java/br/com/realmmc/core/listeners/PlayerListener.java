package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.npc.NPCManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener para eventos gerais de jogadores, como entrada e saída do servidor.
 */
public class PlayerListener implements Listener {

    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(null);

        CoreAPI.getInstance().getSoundManager().playSuccess(player);

        if (!CoreAPI.getInstance().getModuleManager().isClaimed(br.com.realmmc.core.modules.SystemType.TAGS)) {
            CoreAPI.getInstance().getTagManager().updatePlayerTag(player);
        }

        NPCManager npcManager = CoreAPI.getInstance().getNpcManager();
        if (npcManager != null) {
            npcManager.loadPlayerClicks(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.quitMessage(null);

        CoreAPI.getInstance().getDelayManager().clearDelays(player.getUniqueId());

        NPCManager npcManager = CoreAPI.getInstance().getNpcManager();
        if (npcManager != null) {
            npcManager.savePlayerClicks(player.getUniqueId());
        }
    }
}