package br.com.realmmc.core.commands;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.utils.PlayerResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class TeleportHereCommand implements CommandExecutor, TabCompleter {

    private final PlayerResolver resolver = new PlayerResolver();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!player.hasPermission("proxy.moderator")) {
            CoreAPI.getInstance().getTranslationsManager().sendNoPermissionMessage(player, "Moderador");
            return true;
        }

        if (args.length != 1) {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "moderation.teleport.usage-here");
            CoreAPI.getInstance().getSoundManager().playError(player);
            return true;
        }

        resolver.resolve(sender, args[0], resolvedPlayer -> {
            if (!resolvedPlayer.isOnline()) {
                CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.invalid-offline", "target", resolvedPlayer.getFormattedName());
                CoreAPI.getInstance().getSoundManager().playError(player);
                return;
            }
            Player target = resolvedPlayer.getOnlinePlayer().get();
            if (player.equals(target)) {
                CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "moderation.teleport.teleport-here-self");
                CoreAPI.getInstance().getSoundManager().playError(player);
                return;
            }

            target.teleport(player.getLocation());
            // --- SOM ADICIONADO PARA O JOGADOR QUE FOI PUXADO ---
            CoreAPI.getInstance().getSoundManager().playTeleport(target);

            CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "moderation.teleport.success-here", "player", resolvedPlayer.getFormattedName());
            CoreAPI.getInstance().getSoundManager().playSuccess(player);
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("proxy.moderator")) {
            return List.of();
        }
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase() : "";
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}