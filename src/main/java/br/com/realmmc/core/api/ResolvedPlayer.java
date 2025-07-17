package br.com.realmmc.core.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class ResolvedPlayer {

    public enum Status {
        ONLINE, OFFLINE, NOT_FOUND
    }

    private final UUID uuid;
    private final String plainName;
    private final String formattedName;
    private final Status status;

    public ResolvedPlayer(UUID uuid, String plainName, String formattedName, Status status) {
        this.uuid = uuid;
        this.plainName = plainName;
        this.formattedName = formattedName;
        this.status = status;
    }

    public UUID getUuid() { return uuid; }
    public String getPlainName() { return plainName; }
    public String getFormattedName() { return formattedName; }
    public Status getStatus() { return status; }
    public boolean isOnline() { return status == Status.ONLINE; }
    public Optional<Player> getOnlinePlayer() {
        return Optional.ofNullable(uuid).map(Bukkit::getPlayer);
    }
}