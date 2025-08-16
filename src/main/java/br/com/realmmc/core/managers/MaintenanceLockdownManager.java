package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.List;

public class MaintenanceLockdownManager {

    private final Main plugin;
    private boolean globalLockdown = false;
    private boolean localLockdown = false;
    private final List<String> allowedCommands = List.of("tell", "r", "g", "ajuda", "lobby", "s", "yt", "staff", "procurar", "servidor", "grupo");
    private BukkitTask networkActionBarTask;
    private BukkitTask localActionBarTask;

    public MaintenanceLockdownManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isLockdownActive() {
        return globalLockdown || localLockdown;
    }

    public void setGlobalLockdown(boolean active) {
        this.globalLockdown = active;
    }

    public void setLocalLockdown(boolean active) {
        this.localLockdown = active;
    }

    public boolean isCommandAllowed(String command) {
        String baseCommand = command.startsWith("/") ? command.substring(1).split(" ")[0].toLowerCase() : command.split(" ")[0].toLowerCase();
        return allowedCommands.contains(baseCommand);
    }

    public void startActionBarTask(boolean isGlobal) {
        stopActionBarTask(isGlobal);

        String messageKey = isGlobal ? "maintenance.actionbar-active-network" : "maintenance.actionbar-active-local";
        String message = CoreAPI.getInstance().getTranslationsManager().getRawMessage(messageKey, "server", plugin.getServerName());
        String taskId = isGlobal ? "maint_global_active" : "maint_local_active";

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("proxy.maintenance.bypass")) {
                    CoreAPI.getInstance().getActionBarManager().setMessage(player, ActionBarManager.MessagePriority.HIGH, taskId, message, 3);
                }
            }
        }, 0L, 40L);

        if (isGlobal) {
            networkActionBarTask = task;
        } else {
            localActionBarTask = task;
        }
    }

    public void stopActionBarTask(boolean isGlobal) {
        String taskId = isGlobal ? "maint_global_active" : "maint_local_active";
        BukkitTask task = isGlobal ? networkActionBarTask : localActionBarTask;

        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        if (isGlobal) {
            networkActionBarTask = null;
        } else {
            localActionBarTask = null;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            CoreAPI.getInstance().getActionBarManager().clearMessage(player, taskId);
        }
    }
}