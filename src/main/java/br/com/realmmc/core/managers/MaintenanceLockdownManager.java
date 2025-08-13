package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class MaintenanceLockdownManager {

    private final Main plugin;
    private boolean lockdownActive = false;
    private final List<String> allowedCommands = List.of("tell", "r", "g", "ajuda", "lobby", "s", "yt", "staff", "procurar", "servidor", "grupo");
    private BukkitTask actionBarTask;

    public MaintenanceLockdownManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isLockdownActive() {
        return lockdownActive;
    }

    public void setLockdownActive(boolean lockdownActive) {
        this.lockdownActive = lockdownActive;
    }

    public boolean isCommandAllowed(String command) {
        String baseCommand = command.startsWith("/") ? command.substring(1).split(" ")[0].toLowerCase() : command.split(" ")[0].toLowerCase();
        return allowedCommands.contains(baseCommand);
    }

    public void startActionBarTask() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) {
            return;
        }

        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            String message = CoreAPI.getInstance().getTranslationsManager().getRawMessage("maintenance.actionbar-active");
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("proxy.maintenance.bypass")) {
                    CoreAPI.getInstance().getActionBarManager().setMessage(player, ActionBarManager.MessagePriority.HIGH, "maintenance_active", message, 3);
                }
            }
        }, 0L, 40L); // 40 ticks = 2 segundos
    }

    public void stopActionBarTask() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            CoreAPI.getInstance().getActionBarManager().clearMessage(player, "maintenance_active");
        }
    }
}