package br.com.realmmc.core.punishments;

import br.com.realmmc.core.Main;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PunishmentReader {

    private final MongoCollection<Document> punishmentsCollection;

    public PunishmentReader(Main plugin) {
        this.punishmentsCollection = plugin.getDatabaseManager().getDatabase().getCollection("punishments");
    }

    /**
     * Verifica no banco de dados se um jogador possui um mute ativo.
     * @param playerUuid O UUID do jogador.
     * @return um Futuro que completa com 'true' se o jogador estiver mutado, 'false' caso contrário.
     */
    public CompletableFuture<Boolean> isActiveMute(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Bson filter = Filters.and(
                    Filters.eq("player_uuid", playerUuid.toString()),
                    Filters.eq("active", true),
                    Filters.or(
                            Filters.eq("type", "MUTE"),
                            Filters.eq("type", "TEMPMUTE")
                    ),
                    Filters.or(
                            Filters.eq("expires_at", null),
                            Filters.gt("expires_at", new Date())
                    )
            );
            return punishmentsCollection.countDocuments(filter) > 0;
        });
    }
}