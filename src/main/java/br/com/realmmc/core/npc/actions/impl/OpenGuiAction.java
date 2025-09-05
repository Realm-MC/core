package br.com.realmmc.core.npc.actions.impl;

import br.com.realmmc.core.gui.profile.ProfileGUI;
import br.com.realmmc.core.npc.NPC;
import br.com.realmmc.core.npc.actions.NPCAction;
import org.bukkit.entity.Player;

public class OpenGuiAction implements NPCAction {
    @Override
    public void execute(Player player, NPC npc) {
        if (npc.getActionValues().isEmpty()) return;

        String guiName = npc.getActionValues().get(0).toLowerCase();

        switch (guiName) {
            case "perfil":
                new ProfileGUI(player).open();
                break;
            default:
                player.sendMessage("§c[DEBUG] A GUI '" + guiName + "' não foi encontrada.");
                break;
        }
    }
}