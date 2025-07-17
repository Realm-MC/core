package br.com.realmmc.core.commands;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.GodManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GodCommand implements CommandExecutor {

    private final GodManager godManager;
    private static final String PERMISSION = "proxy.moderator";

    public GodCommand(GodManager godManager) {
        this.godManager = godManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            CoreAPI.getInstance().getTranslationsManager().sendNoPermissionMessage(player, "Moderador");
            return true;
        }

        if (godManager.isGodMode(player.getUniqueId())) {
            godManager.disableGodMode(player);
            CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "moderation.god.disabled");
        } else {
            godManager.enableGodMode(player);
            CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "moderation.god.enabled");
        }
        CoreAPI.getInstance().getSoundManager().playSuccess(player);
        return true;
    }
}