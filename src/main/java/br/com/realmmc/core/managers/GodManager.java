package br.com.realmmc.core.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GodManager implements Listener {

    private final Set<UUID> godModePlayers = Collections.synchronizedSet(new HashSet<>());

    public boolean isGodMode(UUID uuid) {
        return godModePlayers.contains(uuid);
    }

    public void enableGodMode(Player player) {
        godModePlayers.add(player.getUniqueId());
    }

    public void disableGodMode(Player player) {
        godModePlayers.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remove o jogador da lista de god mode ao deslogar
        godModePlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isGodMode(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}