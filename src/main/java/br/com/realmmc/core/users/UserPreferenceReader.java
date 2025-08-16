package br.com.realmmc.core.users;

import br.com.realmmc.core.Main;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserPreferenceReader {

    private final MongoCollection<Document> preferencesCollection;

    public UserPreferenceReader(Main plugin) {
        this.preferencesCollection = plugin.getDatabaseManager().getDatabase().getCollection("UserPreference");
    }

    public CompletableFuture<Boolean> canReceiveTell(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = preferencesCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return (doc != null) ? doc.getBoolean("private_messages_status", true) : true;
        });
    }

    public CompletableFuture<Boolean> hasLobbyProtection(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = preferencesCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return (doc != null) && doc.getBoolean("lobby_protection_status", false);
        });
    }

    public CompletableFuture<Boolean> canReceiveCoins(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = preferencesCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return (doc != null) ? doc.getBoolean("coins_receipt_status", true) : true;
        });
    }

    public CompletableFuture<Boolean> hasRankupConfirmation(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = preferencesCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return (doc != null) ? doc.getBoolean("rankup_confirmation_status", true) : true;
        });
    }

    public CompletableFuture<Boolean> hasRankupAlert(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = preferencesCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return (doc != null) ? doc.getBoolean("rankup_alert_status", true) : true;
        });
    }

    public CompletableFuture<Boolean> hasRankupPersonalLight(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = preferencesCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return (doc != null) && doc.getBoolean("rankup_personal_light_status", false);
        });
    }

    public CompletableFuture<Boolean> hasLobbyFly(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = preferencesCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return (doc != null) && doc.getBoolean("lobby_fly_status", false);
        });
    }

    public CompletableFuture<Document> getPreferencesAsDocumentAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = preferencesCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return doc != null ? doc : new Document();
        });
    }
}