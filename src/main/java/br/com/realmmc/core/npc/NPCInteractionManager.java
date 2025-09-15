package br.com.realmmc.core.npc;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.npc.actions.ActionRegistry;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia todas as interações (cliques) com os NPCs baseados em pacotes.
 * Substitui a necessidade de um NPCListener baseado em eventos do Bukkit/Citizens.
 */
public class NPCInteractionManager {

    private final NPCManager npcManager;
    private final ActionRegistry actionRegistry;
    private final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1000;

    public NPCInteractionManager(Main plugin, NPCManager npcManager) {
        this.npcManager = npcManager;
        this.actionRegistry = new ActionRegistry();

        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

                WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
                Player player = (Player) event.getPlayer();
                if (player == null) return;

                if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.INTERACT) {
                    int entityId = wrapper.getEntityId();
                    npcManager.getNpcByEntityId(entityId).ifPresent(npc ->
                            Bukkit.getScheduler().runTask(plugin, () -> handleClick(player, npc))
                    );
                }
            }
        });
    }

    private void handleClick(Player player, NPC npc) {
        long now = System.currentTimeMillis();
        long lastClick = clickCooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (now - lastClick < COOLDOWN_MS) {
            return;
        }
        clickCooldowns.put(player.getUniqueId(), now);

        CoreAPI.getInstance().getSoundManager().playClick(player);

        actionRegistry.getAction(npc.getActionType()).ifPresent(action ->
                action.execute(player, npc)
        );
    }
}
