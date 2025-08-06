package br.com.realmmc.core.utils;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.api.ResolvedPlayer;
import br.com.realmmc.core.users.UserProfileReader;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Utilitário para resolver um identificador (nick, id, uuid) para a identidade de um jogador.
 */
public class PlayerResolver {

    public void resolve(CommandSender sender, String identifier, Consumer<ResolvedPlayer> action) {
        resolvePlayer(identifier).thenAccept(resolvedPlayer -> {
            Bukkit.getScheduler().runTask(CoreAPI.getInstance().getPlugin(), () -> {
                // --- CORREÇÃO FINAL APLICADA AQUI ---
                if (resolvedPlayer.getStatus() == ResolvedPlayer.Status.NOT_FOUND) {
                    CoreAPI.getInstance().getTranslationsManager().sendMessage(sender, "general.invalid-player", "target", identifier);
                    if (sender instanceof Player p) {
                        CoreAPI.getInstance().getSoundManager().playError(p);
                    }
                    return;
                }
                action.accept(resolvedPlayer);
            });
        });
    }

    private CompletableFuture<ResolvedPlayer> resolvePlayer(String identifier) {
        UserProfileReader userProfileReader = CoreAPI.getInstance().getUserProfileReader();
        CompletableFuture<Document> userDocFuture;

        String lowerId = identifier.toLowerCase();

        if (lowerId.startsWith("id:")) {
            try {
                long id = Long.parseLong(identifier.substring(3));
                userDocFuture = userProfileReader.findUserByIdAsync(id);
            } catch (NumberFormatException e) {
                return CompletableFuture.completedFuture(new ResolvedPlayer(null, identifier, identifier, ResolvedPlayer.Status.NOT_FOUND));
            }
        } else if (lowerId.startsWith("uuid:")) {
            try {
                UUID uuid = UUID.fromString(identifier.substring(5));
                userDocFuture = userProfileReader.findUserByUUIDAsync(uuid);
            } catch (IllegalArgumentException e) {
                return CompletableFuture.completedFuture(new ResolvedPlayer(null, identifier, identifier, ResolvedPlayer.Status.NOT_FOUND));
            }
        } else {
            userDocFuture = userProfileReader.findUserByUsernameAsync(identifier);
        }

        return userDocFuture.thenCompose(doc -> {
            if (doc == null) {
                return CompletableFuture.completedFuture(
                        new ResolvedPlayer(null, identifier, identifier, ResolvedPlayer.Status.NOT_FOUND)
                );
            }

            UUID uuid = UUID.fromString(doc.getString("uuid"));
            String plainName = doc.getString("username");
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            ResolvedPlayer.Status status = (onlinePlayer != null && onlinePlayer.isOnline())
                    ? ResolvedPlayer.Status.ONLINE
                    : ResolvedPlayer.Status.OFFLINE;

            return CoreAPI.getInstance().getPlayerManager().getFormattedNicknameAsync(plainName)
                    .thenApply(formattedNameOpt -> {
                        String finalFormattedName = formattedNameOpt.orElse(plainName);
                        return new ResolvedPlayer(uuid, plainName, finalFormattedName, status);
                    });
        });
    }
}