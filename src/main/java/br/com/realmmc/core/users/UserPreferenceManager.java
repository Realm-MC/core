// PASTA: core/src/main/java/br/com/realmmc/core/users/UserPreferenceManager.java
package br.com.realmmc.core.users;

import br.com.realmmc.core.Main;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserPreferenceManager {

    private final MongoCollection<Document> preferencesCollection;
    private final UserProfileReader userProfileReader;

    public UserPreferenceManager(Main plugin) {
        this.preferencesCollection = plugin.getDatabaseManager().getDatabase().getCollection("UserPreference");
        this.userProfileReader = plugin.getUserProfileReader();
    }

    public void ensurePreferenceProfile(Player player) {
        userProfileReader.findUserByUUIDAsync(player.getUniqueId()).thenAccept(userDoc -> {
            if (userDoc == null) return;
            long playerId = userDoc.getLong("_id");
            if (preferencesCollection.countDocuments(Filters.eq("_id", playerId)) > 0) {
                return;
            }
            Document doc = new Document("_id", playerId)
                    .append("uuid", player.getUniqueId().toString())
                    .append("username", player.getName())
                    .append("vanish_status", false)
                    .append("private_messages_status", true)
                    .append("lobby_protection_status", false); // Padrão é false (proteção desativada)
            preferencesCollection.insertOne(doc);
        });
    }

    public CompletableFuture<Boolean> canReceiveTell(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = preferencesCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return (doc != null) ? doc.getBoolean("private_messages_status", true) : true;
        });
    }

    /**
     * --- MÉTODO CORRIGIDO ---
     * Renomeado de isOneClickLobbyEnabled para hasLobbyProtection.
     * Lógica atualizada para usar a nova chave "lobby_protection_status".
     * O valor padrão agora é 'false' (proteção desativada).
     */
    public CompletableFuture<Boolean> hasLobbyProtection(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = preferencesCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return (doc != null) && doc.getBoolean("lobby_protection_status", false);
        });
    }

    /**
     * Busca o documento de preferências inteiro de um jogador.
     * @param uuid O UUID do jogador.
     * @return Um Futuro com o Documento ou um Documento vazio se não existir.
     */
    public CompletableFuture<Document> getPreferencesAsDocumentAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = preferencesCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return doc != null ? doc : new Document(); // Retorna um doc vazio para evitar NullPointerException
        });
    }
}