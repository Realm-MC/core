package br.com.realmmc.core.commands;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.utils.PlayerResolver;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class HealCommand implements CommandExecutor, TabCompleter {

    private final PlayerResolver resolver = new PlayerResolver();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("proxy.moderator")) {
            CoreAPI.getInstance().getTranslationsManager().sendNoPermissionMessage(sender, "Moderador");
            return true;
        }

        if (args.length == 0) {
            handleSelfHeal(sender);
            return true;
        }

        resolver.resolve(sender, args[0], resolvedPlayer -> {
            // --- CORREÇÃO: Verifica se o alvo é o próprio executor ---
            if (sender instanceof Player && sender.equals(resolvedPlayer.getOnlinePlayer().orElse(null))) {
                handleSelfHeal(sender);
                return;
            }

            if (!resolvedPlayer.isOnline()) {
                CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.invalid-offline", "target", resolvedPlayer.getFormattedName());
                CoreAPI.getInstance().getSoundManager().playError(sender instanceof Player ? (Player) sender : null);
                return;
            }

            Player target = resolvedPlayer.getOnlinePlayer().get();
            healPlayer(target);

            CoreAPI.getInstance().getTranslationsManager().sendMessage(target, "moderation.heal.success-target");
            CoreAPI.getInstance().getSoundManager().playSuccess(target);

            CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "moderation.heal.success-other", "player", resolvedPlayer.getFormattedName());
            if (sender instanceof Player) {
                CoreAPI.getInstance().getSoundManager().playSuccess((Player) sender);
            }
        });

        return true;
    }

    private void handleSelfHeal(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.player-only");
            return;
        }
        healPlayer(player);
        CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "moderation.heal.success-self");
        CoreAPI.getInstance().getSoundManager().playSuccess(player);
    }

    private void healPlayer(Player player) {
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(10.0F);
        player.setFireTicks(0);
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