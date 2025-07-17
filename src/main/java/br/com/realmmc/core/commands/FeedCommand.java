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

public class FeedCommand implements CommandExecutor, TabCompleter {

    private final PlayerResolver resolver = new PlayerResolver();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("proxy.moderator")) {
            CoreAPI.getInstance().getTranslationsManager().sendNoPermissionMessage(sender, "Moderador");
            return true;
        }

        if (args.length == 0) {
            handleSelfFeed(sender);
            return true;
        }

        resolver.resolve(sender, args[0], resolvedPlayer -> {
            // --- CORREÇÃO: Verifica se o alvo é o próprio executor ---
            if (sender instanceof Player && sender.equals(resolvedPlayer.getOnlinePlayer().orElse(null))) {
                handleSelfFeed(sender);
                return;
            }

            if (!resolvedPlayer.isOnline()) {
                CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.invalid-offline", "target", resolvedPlayer.getFormattedName());
                CoreAPI.getInstance().getSoundManager().playError(sender instanceof Player ? (Player) sender : null);
                return;
            }

            Player target = resolvedPlayer.getOnlinePlayer().get();
            feedPlayer(target);

            CoreAPI.getInstance().getTranslationsManager().sendMessage(target, "moderation.feed.success-target");
            CoreAPI.getInstance().getSoundManager().playSuccess(target);

            CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "moderation.feed.success-other", "player", resolvedPlayer.getFormattedName());
            if (sender instanceof Player) {
                CoreAPI.getInstance().getSoundManager().playSuccess((Player) sender);
            }
        });

        return true;
    }

    private void handleSelfFeed(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.player-only");
            return;
        }
        feedPlayer(player);
        CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "moderation.feed.success-self");
        CoreAPI.getInstance().getSoundManager().playSuccess(player);
    }

    private void feedPlayer(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(10.0F);
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