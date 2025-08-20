package br.com.realmmc.core.npc;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface NPCAction {
    void onInteract(Player player, String actionValue);
}