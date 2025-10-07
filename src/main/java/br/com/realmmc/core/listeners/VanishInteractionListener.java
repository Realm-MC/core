package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent; // <-- MUDANÇA AQUI
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class VanishInteractionListener implements Listener {

    private final VanishListener vanishListener;

    public VanishInteractionListener(Main plugin) {
        this.vanishListener = plugin.getVanishListener();
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (vanishListener.isVanished(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPickupExp(PlayerPickupExperienceEvent event) {
        Player player = event.getPlayer();
        if (vanishListener.isVanished(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (vanishListener.isVanished(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}