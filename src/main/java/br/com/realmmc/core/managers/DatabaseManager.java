package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.logging.Level;

/**
 * Gerencia a conexão com o banco de dados MongoDB.
 * É responsável por estabelecer, manter e fechar a conexão,
 * além de fornecer acesso ao objeto do banco de dados.
 */
public class DatabaseManager {

    private final Main plugin;
    private final TranslationsManager translationsManager;
    private MongoClient mongoClient;
    private MongoDatabase database;

    /**
     * Constrói e inicializa o gestor do banco de dados.
     * @param plugin A instância principal do plugin.
     * @throws MongoException se a conexão com o banco de dados falhar.
     * @throws IllegalArgumentException se as configurações do banco de dados estiverem ausentes.
     */
    public DatabaseManager(Main plugin) throws MongoException, IllegalArgumentException {
        this.plugin = plugin;
        this.translationsManager = plugin.getTranslationsManager();

        String connectionString = plugin.getConfig().getString("database.mongodb.uri");
        String dbName = plugin.getConfig().getString("database.mongodb.name");

        if (connectionString == null || connectionString.isEmpty() || dbName == null || dbName.isEmpty()) {
            throw new IllegalArgumentException("Database URI or Name not defined in config.yml");
        }

        connect(connectionString, dbName);
    }

    /**
     * Estabelece a conexão com o MongoDB usando as credenciais fornecidas.
     * @param connectionString A URI de conexão.
     * @param dbName O nome do banco de dados.
     * @throws MongoException se a conexão falhar.
     */
    private void connect(String connectionString, String dbName) throws MongoException {
        if (mongoClient != null) {
            close();
        }

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(dbName);

        // Testa a conexão enviando um comando 'ping'
        database.runCommand(new Document("ping", 1));
        translationsManager.log(Level.INFO, "logs.database.connection-success");
    }

    /**
     * Obtém a instância do banco de dados conectado.
     * @return A instância do MongoDatabase.
     * @throws IllegalStateException se o banco de dados não estiver conectado.
     */
    public MongoDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Database connection is not available.");
        }
        return database;
    }

    /**
     * Fecha a conexão com o cliente MongoDB de forma segura.
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            translationsManager.log(Level.INFO, "logs.database.connection-closed");
        }
    }
}