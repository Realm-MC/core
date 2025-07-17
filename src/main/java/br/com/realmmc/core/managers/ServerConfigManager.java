package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.logging.Level;

public class ServerConfigManager {

    private final Main plugin;
    private int maxPlayers;

    public ServerConfigManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        MongoCollection<Document> serversCollection = plugin.getDatabaseManager().getDatabase().getCollection("servers");
        Document serverDoc = serversCollection.find(Filters.eq("_id", plugin.getServerName())).first();

        if (serverDoc != null) {
            this.maxPlayers = serverDoc.getInteger("max_players", 20);
            plugin.getLogger().info("Limite de jogadores customizado carregado do banco de dados: " + this.maxPlayers);
        } else {
            this.maxPlayers = plugin.getServer().getMaxPlayers();
            plugin.getLogger().log(Level.WARNING, "Nenhuma configuração encontrada no banco de dados para o servidor '" + plugin.getServerName() + "'. Usando o limite padrão do servidor.");
        }
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}