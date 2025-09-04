package br.com.realmmc.core.npc.actions;

import br.com.realmmc.core.npc.NPC;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface NPCAction {

    /**
     * Executa a ação do NPC para um jogador.
     * @param player O jogador que interagiu.
     * @param npc A definição do NPC que foi clicado.
     */
    void execute(Player player, NPC npc);
}