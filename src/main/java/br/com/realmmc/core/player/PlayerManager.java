package br.com.realmmc.core.player;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.users.UserProfileReader;
import br.com.realmmc.core.utils.ColorAPI;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlayerManager {

    private final Main plugin;
    private final TranslationsManager translationsManager;
    private final LuckPerms luckPerms;
    private final UserProfileReader userProfileReader;

    public PlayerManager(Main plugin) {
        this.plugin = plugin;
        this.translationsManager = plugin.getTranslationsManager();
        this.luckPerms = plugin.getLuckPerms();
        this.userProfileReader = new UserProfileReader(plugin);
    }

    public CompletableFuture<Optional<String>> getFormattedNicknameAsync(String playerName) {
        return userProfileReader.findUserByUsernameAsync(playerName)
                .thenCompose(userDoc -> {
                    if (userDoc == null) {
                        UUID offlineUuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                        return formatName(offlineUuid, playerName);
                    }
                    UUID uuid = UUID.fromString(userDoc.getString("uuid"));
                    String correctCaseName = userDoc.getString("username");
                    return formatName(uuid, correctCaseName);
                }).exceptionally(ex -> {
                    translationsManager.log(Level.SEVERE, "logs.player-manager.error-format-nickname", ex, "{player}", playerName);
                    return Optional.of(playerName);
                });
    }

    // --- NOVO MÉTODO ---
    public CompletableFuture<Optional<String>> getFormattedNicknameByUuid(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return userProfileReader.findUserByUUIDAsync(uuid).thenCompose(doc -> {
            if (doc == null) {
                return CompletableFuture.completedFuture(Optional.of(uuid.toString()));
            }
            return getFormattedNicknameAsync(doc.getString("username"));
        });
    }

    private CompletableFuture<Optional<String>> formatName(UUID uuid, String name) {
        if (uuid == null || name == null) {
            return CompletableFuture.completedFuture(Optional.ofNullable(name));
        }
        return luckPerms.getUserManager().loadUser(uuid)
                .thenApply(user -> {
                    String prefix = Optional.ofNullable(user.getCachedData().getMetaData().getPrefix()).orElse("");
                    String suffix = Optional.ofNullable(user.getCachedData().getMetaData().getSuffix()).orElse("");
                    return Optional.of(ColorAPI.format(prefix + name + suffix));
                });
    }
}