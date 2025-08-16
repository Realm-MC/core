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

    private final Main plugin;
    private final MaintenanceLockdownManager lockdownManager;
    private final String bypassPermission = "proxy.maintenance.bypass";

    public MaintenanceLockdownListener(Main plugin) {
        this.plugin = plugin;
        this.lockdownManager = plugin.getMaintenanceLockdownManager();
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals("proxy:maintenance")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String data = in.readUTF();

        String[] parts = data.split(":");
        String state = parts[0];
        String receivedTarget = parts.length > 1 ? parts[1] : "network";

        boolean isGlobal = receivedTarget.equalsIgnoreCase("network");
        boolean isForThisServer = plugin.getServerName().equalsIgnoreCase(receivedTarget);

        if (isGlobal) {
            switch(state) {
                case "start" -> lockdownManager.setGlobalLockdown(true);
                case "end", "cancel" -> lockdownManager.setGlobalLockdown(false);
                case "maintenance_on" -> lockdownManager.startActionBarTask(true);
                case "maintenance_off" -> lockdownManager.stopActionBarTask(true);
            }
        } else if (isForThisServer) {
            switch(state) {
                case "start" -> lockdownManager.setLocalLockdown(true);
                case "end", "cancel" -> lockdownManager.setLocalLockdown(false);
                case "maintenance_on" -> lockdownManager.startActionBarTask(false);
                case "maintenance_off" -> lockdownManager.stopActionBarTask(false);
            }
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