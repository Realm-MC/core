package br.com.realmmc.core.npc.util;

import br.com.realmmc.core.npc.NPCSkin;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class MineskinClient {

    private static final String MINESKIN_API = "https://api.mineskin.org/generate/url";
    private static final HttpClient client = HttpClient.newHttpClient();

    public CompletableFuture<NPCSkin> getSkinFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String requestBody = "{\"url\":\"" + url + "\"}";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(MINESKIN_API))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonObject texture = json.getAsJsonObject("data").getAsJsonObject("texture");
                    String value = texture.get("value").getAsString();
                    String signature = texture.get("signature").getAsString();
                    return new NPCSkin(value, signature);
                } else {
                    System.err.println("Mineskin API request failed with status " + response.statusCode() + ": " + response.body());
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }
}