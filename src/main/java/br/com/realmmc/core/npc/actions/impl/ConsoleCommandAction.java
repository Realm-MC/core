package br.com.realmmc.core.npc.actions.impl;

import br.com.realmmc.core.npc.NPC;
import br.com.realmmc.core.npc.actions.NPCAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleCommandAction implements NPCAction {
    @Override
    public void execute(Player player, NPC npc) {
        if (npc.getActionValues().isEmpty()) return;
        String command = npc.getActionValues().get(0).replace("{player}", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}