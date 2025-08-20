package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.player.PlayerManager;
import br.com.realmmc.core.player.RealmPlayer;
import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerDataManager implements Listener {

    private final Main plugin;
    private final Map<UUID, RealmPlayer> playerDataCache = new ConcurrentHashMap<>();

    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        unloadPlayer(event.getPlayer());
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        CompletableFuture<Document> profileFuture = CoreAPI.getInstance().getUserProfileReader().findUserByUUIDAsync(uuid);
        CompletableFuture<Document> preferencesFuture = CoreAPI.getInstance().getUserPreferenceReader().getPreferencesAsDocumentAsync(uuid);
        CompletableFuture<PlayerManager.PlayerDisplayInfo> displayInfoFuture = CoreAPI.getInstance().getPlayerManager().getPlayerDisplayInfo(uuid);
        CompletableFuture<Boolean> lightFuture = CoreAPI.getInstance().getUserPreferenceReader().hasRankupPersonalLight(uuid);
        CompletableFuture<Boolean> coinsFuture = CoreAPI.getInstance().getUserPreferenceReader().canReceiveCoins(uuid);
        CompletableFuture<Boolean> lobbyFlyFuture = CoreAPI.getInstance().getUserPreferenceReader().hasLobbyFly(uuid);

        CompletableFuture.allOf(profileFuture, preferencesFuture, displayInfoFuture, lightFuture, coinsFuture, lobbyFlyFuture).thenAccept(v -> {
            Document profileDoc = profileFuture.join();
            Document prefsDoc = preferencesFuture.join();
            PlayerManager.PlayerDisplayInfo displayInfo = displayInfoFuture.join();
            boolean hasPersonalLight = lightFuture.join();
            boolean canReceiveCoins = coinsFuture.join();
            boolean hasLobbyFly = lobbyFlyFuture.join();

            if (profileDoc == null) {
                plugin.getLogger().severe("Falha crítica ao carregar UserProfile para " + player.getName());
                return;
            }

            // Lógica para pegar o cash do documento do perfil
            long cash = profileDoc.get("cash", 0L);

            long id = profileDoc.getLong("_id");
            String username = profileDoc.getString("username");
            Date firstLogin = profileDoc.getDate("first_login");
            Date lastLogin = profileDoc.getDate("last_login");

            boolean lobbyProtection = prefsDoc.getBoolean("lobby_protection_status", true);
            boolean privateMessages = prefsDoc.getBoolean("private_messages_status", true);
            boolean rankupConfirmation = prefsDoc.getBoolean("rankup_confirmation_status", true);
            boolean rankupAlerts = prefsDoc.getBoolean("rankup_alert_status", true);

            RealmPlayer realmPlayer = new RealmPlayer(id, uuid, username, firstLogin, lastLogin,
                    lobbyProtection, privateMessages, canReceiveCoins, displayInfo.groupName(), displayInfo.prefix(),
                    displayInfo.weight(), rankupConfirmation, rankupAlerts,
                    hasPersonalLight, hasLobbyFly, cash); // Passa o valor do cash

            playerDataCache.put(uuid, realmPlayer);
            plugin.getLogger().info("Perfil de " + username + " (ID: " + id + ") carregado e cacheado com sucesso.");

        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Erro ao carregar dados combinados para " + player.getName(), ex);
            return null;
        });
    }

    public void unloadPlayer(Player player) {
        playerDataCache.remove(player.getUniqueId());
        plugin.getLogger().info("Perfil de " + player.getName() + " removido do cache.");
    }

    public Optional<RealmPlayer> getRealmPlayer(UUID uuid) {
        return Optional.ofNullable(playerDataCache.get(uuid));
    }

    public Optional<RealmPlayer> getRealmPlayer(Player player) {
        return getRealmPlayer(player.getUniqueId());
    }
}