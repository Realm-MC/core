package br.com.realmmc.core.users;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.model.Purchase;
import br.com.realmmc.core.utils.TimeFormatter;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PurchaseHistoryReader {

    private final MongoCollection<Document> pendingRewardsCollection;
    private final MongoCollection<Document> groupHistoryCollection;
    private final MongoCollection<Document> cashTransactionsCollection;

    public PurchaseHistoryReader(Main plugin) {
        this.pendingRewardsCollection = plugin.getDatabaseManager().getDatabase().getCollection("PendingRewards");
        this.groupHistoryCollection = plugin.getDatabaseManager().getDatabase().getCollection("GroupHistory");
        this.cashTransactionsCollection = plugin.getDatabaseManager().getDatabase().getCollection("CashTransactions");
    }

    public CompletableFuture<List<Purchase>> getPurchaseHistory(UUID uuid) {
        CompletableFuture<List<Purchase>> pendingFuture = fetchPending(uuid);
        CompletableFuture<List<Purchase>> groupFuture = fetchGroupHistory(uuid);
        CompletableFuture<List<Purchase>> cashFuture = fetchCashHistory(uuid);

        return CompletableFuture.allOf(pendingFuture, groupFuture, cashFuture)
                .thenApply(v -> {
                    List<Purchase> allPurchases = new ArrayList<>();
                    allPurchases.addAll(pendingFuture.join());
                    allPurchases.addAll(groupFuture.join());
                    allPurchases.addAll(cashFuture.join());
                    Collections.sort(allPurchases);
                    return allPurchases;
                });
    }

    private CompletableFuture<List<Purchase>> fetchPending(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Purchase> purchases = new ArrayList<>();
            pendingRewardsCollection.find(Filters.eq("target_uuid", uuid.toString()))
                    .forEach(doc -> {
                        String type = doc.getString("type");
                        if (type.equals("GROUP")) {
                            purchases.add(new Purchase("VIP", doc.getString("value"), "Pendente", doc.getDate("created_at"), "Pendente", null));
                        } else if (type.equals("CASH")) {
                            purchases.add(new Purchase("CASH", doc.getString("value"), "Pendente", doc.getDate("created_at"), "Pendente", null));
                        }
                    });
            return purchases;
        });
    }

    private CompletableFuture<List<Purchase>> fetchGroupHistory(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Purchase> purchases = new ArrayList<>();
            groupHistoryCollection.find(Filters.and(
                            Filters.eq("target_uuid", uuid.toString()),
                            Filters.eq("action", "ADD")
                    )).sort(Sorts.descending("_id")).limit(20)
                    .forEach(doc -> {
                        Date activationDate = doc.getDate("timestamp");
                        String duration = doc.getString("duration");
                        Date expirationDate = null;
                        String status = "Ativado";

                        if (duration != null && !duration.equalsIgnoreCase("permanente")) {
                            long durationMillis = TimeFormatter.parseDuration(duration);
                            if (durationMillis > 0) {
                                expirationDate = new Date(activationDate.getTime() + durationMillis);
                                if (expirationDate.before(new Date())) {
                                    status = "Expirado";
                                }
                            }
                        }

                        purchases.add(new Purchase(
                                "VIP", doc.getString("group_name"),
                                String.valueOf(doc.getLong("_id")),
                                activationDate, status, expirationDate
                        ));
                    });
            return purchases;
        });
    }

    private CompletableFuture<List<Purchase>> fetchCashHistory(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Purchase> purchases = new ArrayList<>();
            cashTransactionsCollection.find(Filters.and(
                            Filters.eq("target_uuid", uuid.toString()),
                            Filters.eq("type", "ADMIN_ADD")
                    )).sort(Sorts.descending("_id")).limit(20)
                    .forEach(doc -> {
                        purchases.add(new Purchase(
                                "CASH", String.valueOf(doc.getLong("amount")),
                                String.valueOf(doc.getLong("_id")),
                                doc.getDate("timestamp"), "Ativado", null
                        ));
                    });
            return purchases;
        });
    }
}