package br.com.realmmc.core.npc;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.npc.NPC;
import br.com.realmmc.core.npc.NPCManager;
import br.com.realmmc.core.npc.actions.ActionType;
import br.com.realmmc.core.utils.ColorAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NPCListener implements Listener {

    private final Main plugin;
    private final NPCManager npcManager;
    private final Set<UUID> playersInConversation = new HashSet<>();

    public NPCListener(Main plugin) {
        this.plugin = plugin;
        this.npcManager = CoreAPI.getInstance().getNpcManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        net.citizensnpcs.api.npc.NPC clickedNpc = event.getNPC();

        if (clickedNpc.data().has("npc-id")) {
            String npcId = clickedNpc.data().get("npc-id");
            NPC definition = npcManager.getDefinition(npcId);
            if (definition != null) {
                executeNpcAction(player, definition);
            }
        }
    }

    private void executeNpcAction(Player player, NPC definition) {
        ActionType type = definition.getActionType();
        List<String> value = definition.getActionValue();

        if (type == ActionType.SEND_SEQUENCED_MESSAGE) {
            handleSequencedMessage(player, value);
        }
    }

    private void handleSequencedMessage(Player player, List<String> messages) {
        if (playersInConversation.contains(player.getUniqueId())) {
            return;
        }

        playersInConversation.add(player.getUniqueId());

        new BukkitRunnable() {
            private int messageIndex = 0;

            @Override
            public void run() {
                if (!player.isOnline() || messageIndex >= messages.size()) {
                    playersInConversation.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                String message = messages.get(messageIndex);
                CoreAPI.getInstance().getPlayerManager().getFormattedNicknameAsync(player.getName()).thenAccept(formattedName -> {
                    String finalMessage = message.replace("{player_full_name}", formattedName.orElse(player.getName()));

                    player.sendMessage(ColorAPI.format(finalMessage));
                });

                CoreAPI.getInstance().getSoundManager().playNotification(player);
                messageIndex++;
            }
        }.runTaskTimer(plugin, 0L, 60L); // 60 ticks = 3 segundos
    }
}