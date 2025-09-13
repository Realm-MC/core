package br.com.realmmc.core.npc;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NPCListener implements Listener {

    public NPCListener(Main plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        net.citizensnpcs.api.npc.NPC clickedNpc = event.getNPC();

        CoreAPI.getInstance().getSoundManager().playClick(player);

        if (ClickCooldown.hasCooldown(player.getUniqueId())) {
            return;
        }
        ClickCooldown.setCooldown(player.getUniqueId());

        if (clickedNpc.data().has("npc-id")) {
            String npcId = clickedNpc.data().get("npc-id");
            NPCManager npcManager = CoreAPI.getInstance().getNpcManager();

            npcManager.getNpc(npcId).ifPresent(definition -> {
                npcManager.incrementClickCount(npcId, player);
                npcManager.hideAlertIfClicked(player, definition);

                npcManager.getActionRegistry()
                        .getAction(definition.getActionType())
                        .ifPresent(action -> action.execute(player, definition));
            });
        }
    }
}