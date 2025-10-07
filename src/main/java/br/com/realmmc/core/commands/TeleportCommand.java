package br.com.realmmc.core.commands;

import br.com.realmmc.core.Main; // <-- ADICIONADO
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

    private final Main plugin; // <-- ADICIONADO
    private final PlayerResolver resolver = new PlayerResolver();

    // <-- CONSTRUTOR MODIFICADO -->
    public TeleportCommand(Main plugin) {
        this.plugin = plugin;
    }

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
            if(sender instanceof Player) CoreAPI.getInstance().getSoundManager().playError((Player) sender);
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
        resolver.resolve(player, targetName, resolvedTargetOpt -> {
            // <-- CÓDIGO AGORA DENTRO DO SCHEDULER -->
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (resolvedTargetOpt.isEmpty()) {
                    CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "general.invalid-player", "target", targetName);
                    CoreAPI.getInstance().getSoundManager().playError(player);
                    return;
                }
                ResolvedPlayer resolvedTarget = resolvedTargetOpt.get();

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
                CoreAPI.getInstance().getSoundManager().playTeleport(player);
            });
        });
    }

    private void handleTeleportOther(CommandSender sender, String toMoveName, String targetName) {
        resolver.resolve(sender, toMoveName, resolvedToMoveOpt -> {
            resolver.resolve(sender, targetName, resolvedTargetOpt -> {
                // <-- CÓDIGO AGORA DENTRO DO SCHEDULER -->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (resolvedToMoveOpt.isEmpty()) {
                        CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.invalid-player", "target", toMoveName);
                        if(sender instanceof Player) CoreAPI.getInstance().getSoundManager().playError((Player) sender);
                        return;
                    }
                    if (resolvedTargetOpt.isEmpty()) {
                        CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.invalid-player", "target", targetName);
                        if(sender instanceof Player) CoreAPI.getInstance().getSoundManager().playError((Player) sender);
                        return;
                    }

                    ResolvedPlayer resolvedToMove = resolvedToMoveOpt.get();
                    ResolvedPlayer resolvedTarget = resolvedTargetOpt.get();

                    if (sender instanceof Player player && player.getUniqueId().equals(resolvedToMove.getUuid())) {
                        handleTeleport(player, targetName);
                        return;
                    }

                    if (sender instanceof Player player && player.getUniqueId().equals(resolvedTarget.getUuid())) {
                        handleTeleportHere((Player) sender, resolvedToMove);
                        return;
                    }

                    if (!resolvedToMove.isOnline() || !resolvedTarget.isOnline()) {
                        String offlinePlayer = !resolvedToMove.isOnline() ? resolvedToMove.getFormattedName() : resolvedTarget.getFormattedName();
                        CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.invalid-offline", "target", offlinePlayer);
                        if(sender instanceof Player) CoreAPI.getInstance().getSoundManager().playError((Player) sender);
                        return;
                    }

                    Player toMove = resolvedToMove.getOnlinePlayer().get();
                    Player target = resolvedTarget.getOnlinePlayer().get();

                    if (toMove.equals(target)) {
                        CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "moderation.teleport.teleport-same");
                        if(sender instanceof Player) CoreAPI.getInstance().getSoundManager().playError((Player) sender);
                        return;
                    }

                    toMove.teleport(target.getLocation());
                    CoreAPI.getInstance().getSoundManager().playTeleport(toMove);

                    CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "moderation.teleport.success-other", "player", resolvedToMove.getFormattedName(), "target", resolvedTarget.getFormattedName());
                    if (sender instanceof Player) {
                        CoreAPI.getInstance().getSoundManager().playSuccess((Player) sender);
                    }
                });
            });
        });
    }

    // Este método já é chamado dentro do Scheduler, então está seguro.
    private void handleTeleportHere(Player sender, ResolvedPlayer targetToPull) {
        if (!targetToPull.isOnline()) {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.invalid-offline", "target", targetToPull.getFormattedName());
            CoreAPI.getInstance().getSoundManager().playError(sender);
            return;
        }
        Player target = targetToPull.getOnlinePlayer().get();
        target.teleport(sender.getLocation());
        CoreAPI.getInstance().getSoundManager().playTeleport(target);
        CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "moderation.teleport.success-here", "player", targetToPull.getFormattedName());
        CoreAPI.getInstance().getSoundManager().playSuccess(sender);
    }
}