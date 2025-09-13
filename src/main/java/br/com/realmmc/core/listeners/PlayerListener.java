package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.npc.NPC; // Import necessário
import br.com.realmmc.core.npc.NPCManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null);

        CoreAPI.getInstance().getSoundManager().playSuccess(player);

        if (!CoreAPI.getInstance().getModuleManager().isClaimed(br.com.realmmc.core.modules.SystemType.TAGS)) {
            CoreAPI.getInstance().getTagManager().updatePlayerTag(player);
        }

        NPCManager npcManager = CoreAPI.getInstance().getNpcManager();
        if (npcManager != null) {
            npcManager.loadPlayerClicks(player);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (NPC npc : npcManager.getAllNpcs()) {
                    npcManager.hideAlertIfClicked(player, npc);
                }
            }, 20L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(null);

        CoreAPI.getInstance().getDelayManager().clearDelays(player.getUniqueId());

        NPCManager npcManager = CoreAPI.getInstance().getNpcManager();
        if (npcManager != null) {
            npcManager.handlePlayerQuit(player);
        }
    }
}