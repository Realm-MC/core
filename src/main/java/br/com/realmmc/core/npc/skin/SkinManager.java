package br.com.realmmc.core.npc.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o cache e a busca de skins de jogadores da API da Mojang.
 * Essencial para evitar sobrecarga de requisições e acelerar a criação de NPCs.
 */
public class SkinManager {

    private final Map<String, Skin> skinCacheByName = new ConcurrentHashMap<>();
    private final Map<UUID, Skin> skinCacheByUUID = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CompletableFuture<Optional<Skin>> getSkin(String username) {
        if (username == null || username.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.<Skin>empty());
        }

        String key = username.toLowerCase();
        Skin cached = skinCacheByName.get(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return fetchUUID(username).thenCompose(uuidOptional -> {
            if (uuidOptional.isPresent()) {
                return getSkin(uuidOptional.get());
            } else {
                return CompletableFuture.completedFuture(Optional.<Skin>empty());
            }
        });
    }

    public CompletableFuture<Optional<Skin>> getSkin(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(Optional.<Skin>empty());
        }

        Skin cached = skinCacheByUUID.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return fetchSkinData(uuid);
    }

    private CompletableFuture<Optional<UUID>> fetchUUID(String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                        try {
                            JsonObject json = new JsonParser().parse(response.body()).getAsJsonObject();
                            String id = json.get("id").getAsString();
                            UUID uuid = UUID.fromString(id.replaceFirst(
                                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                                    "$1-$2-$3-$4-$5"
                            ));
                            return Optional.of(uuid);
                        } catch (Exception e) {
                            return Optional.<UUID>empty();
                        }
                    }
                    return Optional.<UUID>empty();
                }).exceptionally(ex -> Optional.<UUID>empty());
    }

    private CompletableFuture<Optional<Skin>> fetchSkinData(UUID uuid) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false"))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                        try {
                            JsonObject json = new JsonParser().parse(response.body()).getAsJsonObject();
                            JsonArray propertiesArray = json.getAsJsonArray("properties");
                            if (propertiesArray == null || propertiesArray.size() == 0) {
                                return Optional.<Skin>empty();
                            }
                            JsonObject properties = propertiesArray.get(0).getAsJsonObject();
                            String value = properties.get("value").getAsString();
                            String signature = properties.has("signature") && !properties.get("signature").isJsonNull()
                                    ? properties.get("signature").getAsString()
                                    : null;

                            Skin skin = new Skin(value, signature);

                            skinCacheByUUID.put(uuid, skin);
                            if (json.has("name") && !json.get("name").isJsonNull()) {
                                String username = json.get("name").getAsString();
                                skinCacheByName.put(username.toLowerCase(), skin);
                            }

                            return Optional.of(skin);
                        } catch (Exception e) {
                            return Optional.<Skin>empty();
                        }
                    }
                    return Optional.<Skin>empty();
                }).exceptionally(ex -> Optional.<Skin>empty());
    }
}
