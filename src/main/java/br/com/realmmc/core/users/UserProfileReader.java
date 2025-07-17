package br.com.realmmc.core.users;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.managers.TranslationsManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Classe responsável APENAS pela LEITURA de perfis de usuário do banco de dados.
 * A criação e escrita são gerenciadas pelo plugin do Proxy.
 */
public class UserProfileReader {

    private final TranslationsManager translationsManager;
    private final MongoCollection<Document> profilesCollection;

    public UserProfileReader(Main plugin) {
        this.translationsManager = plugin.getTranslationsManager();
        this.profilesCollection = plugin.getDatabaseManager().getDatabase().getCollection("UserProfile");
    }

    /**
     * Busca um perfil de usuário pelo seu UUID de forma assíncrona.
     * @param uuid O UUID do usuário.
     * @return Um CompletableFuture contendo o Documento do usuário ou null se não encontrado/erro.
     */
    public CompletableFuture<Document> findUserByUUIDAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                profilesCollection.find(Filters.eq("uuid", uuid.toString())).first()
        ).exceptionally(ex -> {
            translationsManager.log(Level.SEVERE, "logs.user-profile.error-get-by-uuid", ex, "{uuid}", uuid.toString());
            return null;
        });
    }

    /**
     * Busca um perfil de usuário pelo seu ID sequencial de forma assíncrona.
     * @param id O ID do usuário.
     * @return Um CompletableFuture contendo o Documento do usuário ou null se não encontrado/erro.
     */
    public CompletableFuture<Document> findUserByIdAsync(long id) {
        return CompletableFuture.supplyAsync(() ->
                profilesCollection.find(Filters.eq("_id", id)).first()
        ).exceptionally(ex -> {
            translationsManager.log(Level.SEVERE, "logs.user-profile.error-get-by-id", ex, "{id}", String.valueOf(id));
            return null;
        });
    }

    /**
     * Busca um perfil de usuário pelo seu nome de forma assíncrona (case-insensitive).
     * @param username O nome de usuário.
     * @return Um CompletableFuture contendo o Documento do usuário ou null se não encontrado/erro.
     */
    public CompletableFuture<Document> findUserByUsernameAsync(String username) {
        return CompletableFuture.supplyAsync(() ->
                profilesCollection.find(Filters.eq("username", username))
                        .collation(Collation.builder().locale("en").collationStrength(CollationStrength.SECONDARY).build())
                        .first()
        ).exceptionally(ex -> {
            translationsManager.log(Level.SEVERE, "logs.user-profile.error-get-by-name", ex, "{name}", username);
            return null;
        });
    }
}