package br.com.realmmc.core.utils;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.api.ResolvedPlayer;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level; // <-- ADICIONADO para logs de erro

public class PlayerResolver {

    // <-- MÉTODO RESOLVE MODIFICADO -->
    public void resolve(CommandSender sender, String identifier, Consumer<Optional<ResolvedPlayer>> callback) {
        CompletableFuture<Optional<ResolvedPlayer>> future;

        if (identifier.toLowerCase().startsWith("uuid:")) {
            String uuidString = identifier.substring(5);
            try {
                UUID uuid = UUID.fromString(uuidString);
                future = findByUuid(uuid);
            } catch (IllegalArgumentException e) {
                future = CompletableFuture.completedFuture(Optional.empty());
            }
        } else if (identifier.toLowerCase().startsWith("id:")) {
            String idString = identifier.substring(3);
            try {
                long id = Long.parseLong(idString);
                future = findById(id);
            } catch (NumberFormatException e) {
                future = CompletableFuture.completedFuture(Optional.empty());
            }
        } else {
            future = findByUsername(identifier);
        }

        // A lógica de erro foi removida daqui e agora o callback é sempre chamado.
        future.whenComplete((resolvedPlayerOpt, throwable) -> {
            if (throwable != null) {
                // Se ocorrer um erro grave, regista-o na consola.
                CoreAPI.getInstance().getTranslationsManager().log(Level.SEVERE, "Erro ao resolver jogador: " + identifier, throwable);
                callback.accept(Optional.empty()); // Retorna um Optional vazio em caso de erro.
                return;
            }
            // Chama o callback com o resultado (que pode ser um Optional vazio se não encontrou o jogador).
            callback.accept(resolvedPlayerOpt);
        });
    }

    private CompletableFuture<Optional<ResolvedPlayer>> findByUsername(String username) {
        return CoreAPI.getInstance().getUserProfileReader().findUserByUsernameAsync(username)
                .thenCompose(this::createResolvedPlayer);
    }

    private CompletableFuture<Optional<ResolvedPlayer>> findByUuid(UUID uuid) {
        return CoreAPI.getInstance().getUserProfileReader().findUserByUUIDAsync(uuid)
                .thenCompose(this::createResolvedPlayer);
    }

    private CompletableFuture<Optional<ResolvedPlayer>> findById(long id) {
        return CoreAPI.getInstance().getUserProfileReader().findUserByIdAsync(id)
                .thenCompose(this::createResolvedPlayer);
    }

    private CompletableFuture<Optional<ResolvedPlayer>> createResolvedPlayer(Document userDoc) {
        if (userDoc == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        UUID uuid = UUID.fromString(userDoc.getString("uuid"));
        String plainName = userDoc.getString("username");
        Player onlinePlayer = Bukkit.getPlayer(uuid);

        return CoreAPI.getInstance().getPlayerManager().getFormattedNicknameAsync(plainName)
                .thenApply(formattedNameOpt -> {
                    String formattedName = formattedNameOpt.orElse(plainName);
                    return Optional.of(new ResolvedPlayer(uuid, plainName, formattedName, onlinePlayer));
                });
    }
}