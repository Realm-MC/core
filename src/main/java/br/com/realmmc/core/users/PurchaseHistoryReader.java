package br.com.realmmc.core.users;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.model.Purchase;
import br.com.realmmc.core.utils.TimeFormatter;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PurchaseHistoryReader {

    private final MongoCollection<Document> groupHistoryCollection;
    private final MongoCollection<Document> cashTransactionsCollection;

    public PurchaseHistoryReader(Main plugin) {
        this.groupHistoryCollection = plugin.getDatabaseManager().getDatabase().getCollection("GroupHistory");
        this.cashTransactionsCollection = plugin.getDatabaseManager().getDatabase().getCollection("CashTransactions");
    }

    public CompletableFuture<List<Purchase>> getPurchaseHistory(UUID uuid) {
        CompletableFuture<List<Purchase>> groupFuture = fetchGroupHistory(uuid);
        CompletableFuture<List<Purchase>> cashFuture = fetchCashHistory(uuid);

        return groupFuture.thenCombine(cashFuture, (groupPurchases, cashPurchases) -> {
            List<Purchase> allPurchases = new ArrayList<>();
            allPurchases.addAll(groupPurchases);
            allPurchases.addAll(cashPurchases);
            allPurchases.sort(Comparator.comparing(Purchase::date).reversed());
            return allPurchases;
        });
    }

    private CompletableFuture<List<Purchase>> fetchGroupHistory(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Purchase> purchases = new ArrayList<>();
            try (MongoCursor<Document> cursor = groupHistoryCollection.find(Filters.and(
                    Filters.eq("target_uuid", uuid.toString()),
                    Filters.eq("action", "ADD")
            )).sort(Sorts.descending("_id")).limit(25).iterator()) {

                while (cursor.hasNext()) {
                    Document doc = cursor.next();

                    String purchaseType = "VIP";
                    String name = doc.getString("group_name");
                    String id = String.valueOf(doc.getLong("_id"));
                    Date activationDate = doc.getDate("timestamp");
                    // CORREÇÃO APLICADA AQUI
                    String statusStr = (String) doc.get("status", "DELIVERED");
                    Date expirationDate = null;
                    String status;

                    if ("PENDING".equals(statusStr)) {
                        status = "Pendente";
                    } else {
                        status = "Ativado";
                        String duration = doc.getString("duration");
                        if (duration != null && !duration.equalsIgnoreCase("permanente")) {
                            long durationMillis = TimeFormatter.parseDuration(duration);
                            if (durationMillis > 0) {
                                expirationDate = new Date(activationDate.getTime() + durationMillis);
                                if (expirationDate.before(new Date())) {
                                    status = "Expirado";
                                }
                            }
                        }
                    }

                    purchases.add(new Purchase(purchaseType, name, id, activationDate, status, expirationDate));
                }
            }
            return purchases;
        });
    }

    private CompletableFuture<List<Purchase>> fetchCashHistory(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Purchase> purchases = new ArrayList<>();
            try (MongoCursor<Document> cursor = cashTransactionsCollection.find(Filters.and(
                    Filters.eq("target_uuid", uuid.toString()),
                    Filters.eq("type", "ADMIN_ADD")
            )).sort(Sorts.descending("_id")).limit(25).iterator()) {

                while (cursor.hasNext()) {
                    Document doc = cursor.next();

                    String purchaseType = "CASH";
                    String name = String.valueOf(doc.getLong("amount"));
                    String id = String.valueOf(doc.getLong("_id"));
                    Date date = doc.getDate("timestamp");
                    // CORREÇÃO APLICADA AQUI
                    String statusStr = (String) doc.get("status", "DELIVERED");
                    String status = "PENDING".equals(statusStr) ? "Pendente" : "Ativado";
                    Date expirationDate = null;

                    purchases.add(new Purchase(purchaseType, name, id, date, status, expirationDate));
                }
            }
            return purchases;
        });
    }
}