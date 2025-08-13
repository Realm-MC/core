package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.MaintenanceLockdownManager;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class MaintenanceLockdownListener implements PluginMessageListener, Listener {

    private final MaintenanceLockdownManager lockdownManager;
    private final String bypassPermission = "proxy.maintenance.bypass";

    public MaintenanceLockdownListener(Main plugin) {
        this.lockdownManager = plugin.getMaintenanceLockdownManager();
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals("proxy:maintenance")) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        switch (subChannel) {
            case "start" -> lockdownManager.setLockdownActive(true);
            case "end", "cancel" -> lockdownManager.setLockdownActive(false);
            case "maintenance_on" -> lockdownManager.startActionBarTask();
            case "maintenance_off" -> lockdownManager.stopActionBarTask();
        }
    }

    private void notifyAndCancel(Player player) {
        player.sendMessage(CoreAPI.getInstance().getTranslationsManager().getMessage("maintenance.lockdown-message"));
        CoreAPI.getInstance().getSoundManager().playError(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (lockdownManager.isLockdownActive() && !event.getPlayer().hasPermission(bypassPermission)) {
            if (!lockdownManager.isCommandAllowed(event.getMessage())) {
                event.setCancelled(true);
                notifyAndCancel(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (lockdownManager.isLockdownActive() && !event.getPlayer().hasPermission(bypassPermission)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && lockdownManager.isLockdownActive()) {
            if (!event.getDamager().hasPermission(bypassPermission)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (lockdownManager.isLockdownActive() && !event.getPlayer().hasPermission(bypassPermission)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (lockdownManager.isLockdownActive() && !event.getPlayer().hasPermission(bypassPermission)) {
            event.setCancelled(true);
        }
    }
}