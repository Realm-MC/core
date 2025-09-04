package br.com.realmmc.core.npc.actions.impl;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.npc.NPC;
import br.com.realmmc.core.npc.actions.NPCAction;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SendSequencedMessageAction implements NPCAction {

    private final Set<UUID> playersInConversation = new HashSet<>();

    @Override
    public void execute(Player player, NPC npc) {
        if (playersInConversation.contains(player.getUniqueId()) || npc.getActionValues().isEmpty()) {
            return;
        }

        playersInConversation.add(player.getUniqueId());
        Main plugin = CoreAPI.getInstance().getPlugin();

        new BukkitRunnable() {
            private int messageIndex = 0;

            @Override
            public void run() {
                if (!player.isOnline() || messageIndex >= npc.getActionValues().size()) {
                    playersInConversation.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                // A MUDANÇA ESTÁ AQUI: O valor agora é uma CHAVE de tradução
                String messageKey = npc.getActionValues().get(messageIndex);

                CoreAPI.getInstance().getPlayerManager().getFormattedNicknameAsync(player.getName()).thenAccept(formattedName -> {
                    // Usamos o TranslationsManager para obter a mensagem final
                    String finalMessage = CoreAPI.getInstance().getTranslationsManager().getMessage(
                            messageKey,
                            "player_full_name", formattedName.orElse(player.getName())
                    );
                    player.sendMessage(finalMessage);
                });

                CoreAPI.getInstance().getSoundManager().playNotification(player);
                messageIndex++;
            }
        }.runTaskTimer(plugin, 0L, 60L);
    }
}