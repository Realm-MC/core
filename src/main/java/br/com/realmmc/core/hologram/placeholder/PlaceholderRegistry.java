package br.com.realmmc.core.hologram.placeholder;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.utils.ColorAPI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PlaceholderRegistry {

    private static final Map<String, Supplier<String>> placeholders = new ConcurrentHashMap<>();

    /**
     * Inicia os placeholders padrão, recebendo a instância do plugin para acesso seguro.
     * @param plugin A instância da classe Main do Core.
     */
    public static void initializeDefaults(Main plugin) {
        // Agora, os placeholders usam a instância do plugin passada diretamente.
        registerPlaceholder("{online_players}", () -> String.valueOf(Bukkit.getOnlinePlayers().size()));
        registerPlaceholder("{server_name}", plugin::getServerName);

        MongoCollection<Document> serversCollection = plugin.getDatabaseManager().getDatabase().getCollection("servers");
        if (serversCollection != null) {
            for (Document doc : serversCollection.find()) {
                String serverName = doc.getString("_id");
                if (serverName != null && !serverName.isEmpty()) {
                    registerPlaceholder("{server_" + serverName.toLowerCase() + "_online}", () -> {
                        Document serverDoc = serversCollection.find(Filters.eq("_id", serverName)).first();
                        if (serverDoc != null) {
                            int playerCount = serverDoc.getInteger("player_count", 0);
                            return String.valueOf(playerCount >= 0 ? playerCount : 0);
                        }
                        return "0";
                    });
                }
            }
        }
    }

    public static void registerPlaceholder(String key, Supplier<String> function) {
        placeholders.put(key, function);
    }

    public static String replacePlaceholders(String text) {
        for (Map.Entry<String, Supplier<String>> entry : placeholders.entrySet()) {
            if (text.contains(entry.getKey())) {
                text = text.replace(entry.getKey(), entry.getValue().get());
            }
        }
        return ColorAPI.format(text);
    }
}