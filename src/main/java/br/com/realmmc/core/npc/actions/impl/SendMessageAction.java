package br.com.realmmc.core.npc.actions.impl;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.npc.NPC;
import br.com.realmmc.core.npc.actions.NPCAction;
import org.bukkit.entity.Player;

public class SendMessageAction implements NPCAction {
    @Override
    public void execute(Player player, NPC npc) {
        if (npc.getActionValues().isEmpty()) return;
        String message = npc.getActionValues().get(0).replace("{player}", player.getName());
        player.sendMessage(CoreAPI.getInstance().getTranslationsManager().getMessage(message));
    }
}