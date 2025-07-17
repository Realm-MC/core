package br.com.realmmc.core.users;

import br.com.realmmc.core.Main;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.entity.Player;

public class UserPreferenceManager {

    private final MongoCollection<Document> preferencesCollection;
    private final UserProfileReader userProfileReader;

    public UserPreferenceManager(Main plugin) {
        this.preferencesCollection = plugin.getDatabaseManager().getDatabase().getCollection("UserPreference");
        this.userProfileReader = plugin.getUserProfileReader();
    }

    /**
     * Garante que um perfil de preferências exista para o jogador.
     * Chamado quando o jogador entra no servidor.
     */
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
                    .append("vanish_status", false);

            // Outras preferências futuras podem ser adicionadas aqui.
            preferencesCollection.insertOne(doc);
        });
    }

    // Métodos de GodMode e GameMode foram removidos conforme solicitado.
}