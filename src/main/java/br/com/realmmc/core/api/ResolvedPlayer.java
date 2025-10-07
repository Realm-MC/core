package br.com.realmmc.core.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class ResolvedPlayer {

    private final UUID uuid;
    private final String plainName;
    private final String formattedName;
    private final Player onlinePlayer; // Pode ser null se o jogador estiver offline

    public ResolvedPlayer(UUID uuid, String plainName, String formattedName, @Nullable Player onlinePlayer) {
        this.uuid = uuid;
        this.plainName = plainName;
        this.formattedName = formattedName;
        this.onlinePlayer = onlinePlayer;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlainName() {
        return plainName;
    }

    public String getFormattedName() {
        return formattedName;
    }

    public Optional<Player> getOnlinePlayer() {
        return Optional.ofNullable(onlinePlayer);
    }

    public boolean isOnline() {
        return onlinePlayer != null && onlinePlayer.isOnline();
    }
}