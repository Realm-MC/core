package br.com.realmmc.core.player;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.users.UserProfileReader;
import br.com.realmmc.core.utils.ColorAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlayerManager {

    private final Main plugin;
    private final TranslationsManager translationsManager;
    private final LuckPerms luckPerms;
    private final UserProfileReader userProfileReader;

    // Novo Record para transportar todos os dados necessários de uma vez
    public record PlayerDisplayInfo(String groupName, String prefix, int weight) {}

    public PlayerManager(Main plugin) {
        this.plugin = plugin;
        this.translationsManager = plugin.getTranslationsManager();
        this.luckPerms = plugin.getLuckPerms();
        this.userProfileReader = new UserProfileReader(plugin);
    }

    // Novo método unificado para buscar todas as informações de display
    public CompletableFuture<PlayerDisplayInfo> getPlayerDisplayInfo(UUID uuid) {
        if (uuid == null || luckPerms == null) {
            return CompletableFuture.completedFuture(new PlayerDisplayInfo("§7Membro", "§7", 0));
        }
        return luckPerms.getUserManager().loadUser(uuid)
                .thenApply(user -> {
                    String groupNameId = user.getPrimaryGroup();
                    if (groupNameId == null) {
                        return new PlayerDisplayInfo("§7Membro", "§7", 0);
                    }
                    Group group = luckPerms.getGroupManager().getGroup(groupNameId);
                    if (group == null) {
                        return new PlayerDisplayInfo("§7" + groupNameId, "§7", 0);
                    }

                    String prefix = Optional.ofNullable(group.getCachedData().getMetaData().getPrefix()).orElse("§7");
                    String displayName = group.getFriendlyName() != null ? group.getFriendlyName() : groupNameId;
                    int weight = group.getWeight().orElse(0);

                    String colorCodes = ChatColor.getLastColors(ColorAPI.format(prefix));
                    String formattedGroupName = colorCodes.isEmpty() ? "§7" + displayName : colorCodes + displayName;

                    return new PlayerDisplayInfo(formattedGroupName, prefix, weight);
                }).exceptionally(ex -> new PlayerDisplayInfo("§7Membro", "§7", 0));
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