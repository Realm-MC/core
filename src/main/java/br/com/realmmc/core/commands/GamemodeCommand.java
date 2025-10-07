package br.com.realmmc.core.commands;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.utils.PlayerResolver;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GamemodeCommand implements CommandExecutor, TabCompleter {

    private final Main plugin; // <-- ADICIONADO
    private final PlayerResolver resolver = new PlayerResolver();
    private static final String PERM_SELF = "proxy.administrator";
    private static final String PERM_OTHER = "proxy.manager";

    // <-- ADICIONADO
    public GamemodeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERM_SELF)) {
            CoreAPI.getInstance().getTranslationsManager().sendNoPermissionMessage(sender, "Administrador");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        Optional<GameMode> optGameMode = parseGameMode(args[0]);
        if (optGameMode.isEmpty()) {
            sendError(sender, "moderation.gamemode.invalid");
            return true;
        }
        GameMode gameMode = optGameMode.get();

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sendError(sender, "general.player-only");
                return true;
            }
            applyAndNotify(player, player, gameMode);
            return true;
        }

        if (args.length == 2) {
            if (!sender.hasPermission(PERM_OTHER)) {
                sendNoPermissionMessageForOther(sender);
                return true;
            }
            resolver.resolve(sender, args[1], resolvedPlayerOpt -> {
                // <-- CÓDIGO AGORA DENTRO DO SCHEDULER -->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (resolvedPlayerOpt.isEmpty()) {
                        sendError(sender, "general.invalid-player", "target", args[1]);
                        return;
                    }
                    var resolvedPlayer = resolvedPlayerOpt.get();
                    if (!resolvedPlayer.isOnline()) {
                        sendError(sender, "general.invalid-offline", "target", resolvedPlayer.getFormattedName());
                        return;
                    }
                    applyAndNotify(sender, resolvedPlayer.getOnlinePlayer().get(), gameMode);
                });
            });
            return true;
        }

        sendUsage(sender);
        return true;
    }

    private void applyAndNotify(CommandSender executor, Player target, GameMode mode) {
        target.setGameMode(mode);
        CoreAPI.getInstance().getSoundManager().playSuccess(target);
        if (executor instanceof Player && !executor.equals(target)) {
            CoreAPI.getInstance().getSoundManager().playSuccess((Player) executor);
        }

        String friendlyGameModeName = mode.name().substring(0, 1).toUpperCase() + mode.name().substring(1).toLowerCase();

        if (executor.equals(target)) {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(executor, "moderation.gamemode.success-self", "gamemode", friendlyGameModeName);
        } else {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(target, "moderation.gamemode.success-self", "gamemode", friendlyGameModeName);
            CoreAPI.getInstance().getPlayerManager().getFormattedNicknameAsync(target.getName()).thenAccept(formattedNameOpt -> {
                String formattedName = formattedNameOpt.orElse(target.getName());
                CoreAPI.getInstance().getTranslationsManager().sendMessage(executor, "moderation.gamemode.success-other", "player", formattedName, "gamemode", friendlyGameModeName);
            });
        }
    }

    private void sendUsage(CommandSender sender) {
        if (sender.hasPermission(PERM_OTHER)) {
            sendError(sender, "moderation.gamemode.usage-manager");
        } else if (sender.hasPermission(PERM_SELF)) {
            sendError(sender, "moderation.gamemode.usage-admin");
        }
    }

    private void sendNoPermissionMessageForOther(CommandSender sender){
        CoreAPI.getInstance().getTranslationsManager().sendNoPermissionMessage(sender, "Gerente");
    }

    private void sendError(CommandSender sender, String key, String... replacements) {
        CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, key, replacements);
        if (sender instanceof Player) {
            CoreAPI.getInstance().getSoundManager().playError((Player) sender);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERM_SELF)) {
            return List.of();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Stream.of("survival", "creative", "adventure", "spectator", "0", "1", "2", "3")
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && sender.hasPermission(PERM_OTHER)) {
            String prefix = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    private Optional<GameMode> parseGameMode(String input) {
        return switch (input.toLowerCase()) {
            case "0", "s", "survival" -> Optional.of(GameMode.SURVIVAL);
            case "1", "c", "creative" -> Optional.of(GameMode.CREATIVE);
            case "2", "a", "adventure" -> Optional.of(GameMode.ADVENTURE);
            case "3", "spec", "spectator" -> Optional.of(GameMode.SPECTATOR);
            default -> Optional.empty();
        };
    }
}