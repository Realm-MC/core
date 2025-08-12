package br.com.realmmc.core.player;

import java.util.Date;
import java.util.UUID;

public class RealmPlayer {

    private final long id;
    private final UUID uuid;
    private final String username;
    private final Date firstLogin;
    private final Date lastLogin;
    private final boolean lobbyProtectionEnabled;
    private final boolean privateMessagesEnabled;
    private final String primaryGroup;
    private final String prefix;
    private final int groupWeight;
    private boolean needsRankupConfirmation;
    private boolean prefersChatRankupAlerts;
    private double balance;

    public RealmPlayer(long id, UUID uuid, String username, Date firstLogin, Date lastLogin,
                       boolean lobbyProtectionEnabled, boolean privateMessagesEnabled,
                       String primaryGroup, String prefix, int groupWeight,
                       boolean needsRankupConfirmation, boolean prefersChatRankupAlerts) {
        this.id = id;
        this.uuid = uuid;
        this.username = username;
        this.firstLogin = firstLogin;
        this.lastLogin = lastLogin;
        this.lobbyProtectionEnabled = lobbyProtectionEnabled;
        this.privateMessagesEnabled = privateMessagesEnabled;
        this.primaryGroup = primaryGroup;
        this.prefix = prefix;
        this.groupWeight = groupWeight;
        this.needsRankupConfirmation = needsRankupConfirmation;
        this.prefersChatRankupAlerts = prefersChatRankupAlerts;
        this.balance = 0.0;
    }

    public long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public Date getFirstLogin() { return firstLogin; }
    public Date getLastLogin() { return lastLogin; }
    public boolean hasLobbyProtection() { return lobbyProtectionEnabled; }
    public boolean canReceivePrivateMessages() { return privateMessagesEnabled; }
    public String getPrimaryGroup() { return primaryGroup; }
    public String getPrefix() { return prefix; }
    public int getGroupWeight() { return groupWeight; }
    public boolean needsRankupConfirmation() { return needsRankupConfirmation; }
    public boolean prefersChatRankupAlerts() { return prefersChatRankupAlerts; }
    public double getBalance() { return balance; }

    public void setBalance(double balance) { this.balance = balance; }

    public void setNeedsRankupConfirmation(boolean needsRankupConfirmation) {
        this.needsRankupConfirmation = needsRankupConfirmation;
    }
    public void setPrefersChatRankupAlerts(boolean prefersChatRankupAlerts) {
        this.prefersChatRankupAlerts = prefersChatRankupAlerts;
    }
}