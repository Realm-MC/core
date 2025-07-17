package br.com.realmmc.core.commands;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.api.ResolvedPlayer;
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

public class TeleportCommand implements CommandExecutor, TabCompleter {

    private final PlayerResolver resolver = new PlayerResolver();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("proxy.moderator")) {
            CoreAPI.getInstance().getTranslationsManager().sendNoPermissionMessage(sender, "Moderador");
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.player-only");
                return true;
            }
            handleTeleport(player, args[0]);
        } else if (args.length == 2) {
            if (!sender.hasPermission("proxy.administrator")) {
                CoreAPI.getInstance().getTranslationsManager().sendNoPermissionMessage(sender, "Administrador");
                return true;
            }
            handleTeleportOther(sender, args[0], args[1]);
        } else {
            String usageKey = sender.hasPermission("proxy.administrator") ? "moderation.teleport.usage-admin" : "moderation.teleport.usage";
            CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, usageKey);
            CoreAPI.getInstance().getSoundManager().playError(sender instanceof Player ? (Player) sender : null);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("proxy.moderator")) {
            return List.of();
        }
        if (args.length <= 1 || (args.length == 2 && sender.hasPermission("proxy.administrator"))) {
            String prefix = args[args.length - 1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void handleTeleport(Player player, String targetName) {
        resolver.resolve(player, targetName, resolvedTarget -> {
            if (!resolvedTarget.isOnline()) {
                CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "general.invalid-offline", "target", resolvedTarget.getFormattedName());
                CoreAPI.getInstance().getSoundManager().playError(player);
                return;
            }
            Player target = resolvedTarget.getOnlinePlayer().get();
            if (player.equals(target)) {
                CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "moderation.teleport.teleport-self");
                CoreAPI.getInstance().getSoundManager().playError(player);
                return;
            }

            player.teleport(target.getLocation());
            CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "moderation.teleport.success-self", "player", resolvedTarget.getFormattedName());
            CoreAPI.getInstance().getSoundManager().playSuccess(player);
        });
    }

    private void handleTeleportOther(CommandSender sender, String toMoveName, String targetName) {
        resolver.resolve(sender, toMoveName, resolvedToMove -> {
            resolver.resolve(sender, targetName, resolvedTarget -> {
                // --- CORREÇÃO: Lógica para /tp eu <alvo> ---
                if (sender instanceof Player player && player.getUniqueId().equals(resolvedToMove.getUuid())) {
                    handleTeleport(player, targetName);
                    return;
                }

                // --- CORREÇÃO: Lógica para /tp <alvo> eu ---
                if (sender instanceof Player player && player.getUniqueId().equals(resolvedTarget.getUuid())) {
                    handleTeleportHere(player, resolvedToMove);
                    return;
                }

                if (!resolvedToMove.isOnline() || !resolvedTarget.isOnline()) {
                    String offlinePlayer = !resolvedToMove.isOnline() ? resolvedToMove.getFormattedName() : resolvedTarget.getFormattedName();
                    CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.invalid-offline", "target", offlinePlayer);
                    CoreAPI.getInstance().getSoundManager().playError(sender instanceof Player ? (Player) sender : null);
                    return;
                }

                Player toMove = resolvedToMove.getOnlinePlayer().get();
                Player target = resolvedTarget.getOnlinePlayer().get();

                if (toMove.equals(target)) {
                    CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "moderation.teleport.teleport-same");
                    CoreAPI.getInstance().getSoundManager().playError(sender instanceof Player ? (Player) sender : null);
                    return;
                }

                toMove.teleport(target.getLocation());
                CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "moderation.teleport.success-other", "player", resolvedToMove.getFormattedName(), "target", resolvedTarget.getFormattedName());
                CoreAPI.getInstance().getSoundManager().playSuccess(sender instanceof Player ? (Player) sender : null);
            });
        });
    }

    private void handleTeleportHere(Player sender, ResolvedPlayer targetToPull) {
        if (!targetToPull.isOnline()) {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.invalid-offline", "target", targetToPull.getFormattedName());
            CoreAPI.getInstance().getSoundManager().playError(sender);
            return;
        }
        Player target = targetToPull.getOnlinePlayer().get();
        target.teleport(sender.getLocation());
        CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "moderation.teleport.success-here", "player", targetToPull.getFormattedName());
        CoreAPI.getInstance().getSoundManager().playSuccess(sender);
    }
}