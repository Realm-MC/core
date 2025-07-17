package br.com.realmmc.core.utils;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.api.ResolvedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PlayerResolver {

    public void resolve(CommandSender sender, String identifier, Consumer<ResolvedPlayer> action) {
        resolvePlayer(identifier).thenAccept(resolvedPlayer -> {
            Bukkit.getScheduler().runTask(CoreAPI.getInstance().getPlugin(), () -> {
                if (resolvedPlayer.getStatus() == ResolvedPlayer.Status.NOT_FOUND) {
                    CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.invalid-player", "target", identifier);
                    if (sender instanceof Player) {
                        CoreAPI.getInstance().getSoundManager().playError((Player) sender);
                    }
                    return;
                }
                action.accept(resolvedPlayer);
            });
        });
    }

    private CompletableFuture<ResolvedPlayer> resolvePlayer(String identifier) {
        CompletableFuture<Optional<UUID>> uuidFuture;
        String fallbackName = identifier;

        if (identifier.toLowerCase().startsWith("id:")) {
            try {
                long id = Long.parseLong(identifier.substring(3));
                uuidFuture = CoreAPI.getInstance().getUserProfileReader().findUserByIdAsync(id)
                        .thenApply(doc -> Optional.ofNullable(doc).map(d -> UUID.fromString(d.getString("uuid"))));
            } catch (NumberFormatException e) {
                return CompletableFuture.completedFuture(new ResolvedPlayer(null, fallbackName, fallbackName, ResolvedPlayer.Status.NOT_FOUND));
            }
        } else if (identifier.toLowerCase().startsWith("uuid:")) {
            try {
                uuidFuture = CompletableFuture.completedFuture(Optional.of(UUID.fromString(identifier.substring(5))));
            } catch (IllegalArgumentException e) {
                return CompletableFuture.completedFuture(new ResolvedPlayer(null, fallbackName, fallbackName, ResolvedPlayer.Status.NOT_FOUND));
            }
        } else {
            uuidFuture = CoreAPI.getInstance().getUserProfileReader().findUserByUsernameAsync(identifier)
                    .thenApply(doc -> Optional.ofNullable(doc).map(d -> UUID.fromString(d.getString("uuid"))));
        }

        return uuidFuture.thenCompose(uuidOpt -> {
            if (uuidOpt.isEmpty()) {
                return CompletableFuture.completedFuture(new ResolvedPlayer(null, fallbackName, fallbackName, ResolvedPlayer.Status.NOT_FOUND));
            }
            UUID uuid = uuidOpt.get();

            return CoreAPI.getInstance().getPlayerManager().getFormattedNicknameByUuid(uuid).thenApply(formattedNameOpt -> {
                String formattedName = formattedNameOpt.orElse(fallbackName);
                Player onlinePlayer = Bukkit.getPlayer(uuid);

                ResolvedPlayer.Status status = (onlinePlayer != null && onlinePlayer.isOnline()) ? ResolvedPlayer.Status.ONLINE : ResolvedPlayer.Status.OFFLINE;
                return new ResolvedPlayer(uuid, fallbackName, formattedName, status);
            });
        });
    }
}