package br.com.realmmc.core.users;

import br.com.realmmc.core.Main;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GroupInfoReader {

    private final MongoCollection<Document> groupsCollection;

    public GroupInfoReader(Main plugin) {
        this.groupsCollection = plugin.getDatabaseManager().getDatabase().getCollection("luckperms_groups");
    }

    public CompletableFuture<Optional<String>> getDisplayName(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = groupsCollection.find(Filters.eq("_id", groupName.toLowerCase())).first();
            if (doc != null) {
                String displayName = doc.getString("displayName");
                if (displayName != null) {
                    return Optional.of(displayName);
                }
            }
            return Optional.of(groupName.substring(0, 1).toUpperCase() + groupName.substring(1));
        });
    }
}