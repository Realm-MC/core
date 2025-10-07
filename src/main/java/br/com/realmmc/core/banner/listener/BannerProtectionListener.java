package br.com.realmmc.core.banner.listener;

import br.com.realmmc.core.Main;
import org.bukkit.GameMode;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class BannerProtectionListener implements Listener {

    private final Main plugin;

    public BannerProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame) {
            if (event.getEntity().hasMetadata("realm_banner_id")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame) {
            if (event.getRightClicked().hasMetadata("realm_banner_id")) {
                if (event.getPlayer().hasPermission("core.manager") && event.getPlayer().getGameMode() == GameMode.CREATIVE) {
                    return;
                }
                event.setCancelled(true);
            }
        }
    }
}