package br.com.realmmc.core.npc;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NPCListener implements Listener {

    private final Main plugin;

    public NPCListener(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        net.citizensnpcs.api.npc.NPC clickedNpc = event.getNPC();

        if (ClickCooldown.hasCooldown(player.getUniqueId())) {
            return;
        }
        ClickCooldown.setCooldown(player.getUniqueId());

        if (clickedNpc.data().has("npc-id")) {
            String npcId = clickedNpc.data().get("npc-id");
            NPCManager npcManager = CoreAPI.getInstance().getNpcManager();

            npcManager.getNpc(npcId).ifPresent(definition -> {
                // ===================================================================================== //
                //                     CORREÇÃO 2: ATUALIZAÇÃO IMEDIATA DO HOLOGRAMA                     //
                // ===================================================================================== //
                // Agora, esperamos o clique ser incrementado no DB e, QUANDO TERMINAR (.thenRun),
                // forçamos a atualização do holograma para o jogador.
                npcManager.incrementClickCount(npcId, player).thenRun(() -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        CoreAPI.getInstance().getHologramManager().checkVisibilityForPlayer(player);
                    });
                });

                // Executa a ação do NPC (conversa)
                npcManager.getActionRegistry()
                        .getAction(definition.getActionType())
                        .ifPresent(action -> action.execute(player, definition));
            });
        }
    }
}