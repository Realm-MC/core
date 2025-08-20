package br.com.realmmc.core.player;

import java.util.Date;
import java.util.UUID;

public class RealmPlayer {

    private final long id;
    private final UUID uuid;
    private final String username;
    private final Date firstLogin;
    private final Date lastLogin;
    private final String primaryGroup;
    private final String prefix;
    private final int groupWeight;

    // CAMPOS DE PREFERÊNCIA AGORA SÃO MUTÁVEIS (NÃO-FINAIS)
    private boolean lobbyProtectionEnabled;
    private boolean privateMessagesEnabled;
    private boolean coinsReceiptEnabled;
    private boolean needsRankupConfirmation;
    private boolean prefersChatRankupAlerts;
    private boolean hasPersonalLight;
    private boolean lobbyFlyEnabled;
    private long cash;
    private double balance;

    public RealmPlayer(long id, UUID uuid, String username, Date firstLogin, Date lastLogin,
                       boolean lobbyProtectionEnabled, boolean privateMessagesEnabled, boolean coinsReceiptEnabled,
                       String primaryGroup, String prefix, int groupWeight,
                       boolean needsRankupConfirmation, boolean prefersChatRankupAlerts,
                       boolean hasPersonalLight, boolean lobbyFlyEnabled, long cash) {
        this.id = id;
        this.uuid = uuid;
        this.username = username;
        this.firstLogin = firstLogin;
        this.lastLogin = lastLogin;
        this.lobbyProtectionEnabled = lobbyProtectionEnabled;
        this.privateMessagesEnabled = privateMessagesEnabled;
        this.coinsReceiptEnabled = coinsReceiptEnabled;
        this.primaryGroup = primaryGroup;
        this.prefix = prefix;
        this.groupWeight = groupWeight;
        this.needsRankupConfirmation = needsRankupConfirmation;
        this.prefersChatRankupAlerts = prefersChatRankupAlerts;
        this.hasPersonalLight = hasPersonalLight;
        this.lobbyFlyEnabled = lobbyFlyEnabled;
        this.cash = cash;
        this.balance = 0.0;
    }

    // Getters
    public long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public Date getFirstLogin() { return firstLogin; }
    public Date getLastLogin() { return lastLogin; }
    public String getPrimaryGroup() { return primaryGroup; }
    public String getPrefix() { return prefix; }
    public int getGroupWeight() { return groupWeight; }
    public boolean hasLobbyProtection() { return lobbyProtectionEnabled; }
    public boolean canReceivePrivateMessages() { return privateMessagesEnabled; }
    public boolean canReceiveCoins() { return coinsReceiptEnabled; }
    public boolean needsRankupConfirmation() { return needsRankupConfirmation; }
    public boolean prefersChatRankupAlerts() { return prefersChatRankupAlerts; }
    public boolean hasPersonalLight() { return hasPersonalLight; }
    public boolean hasLobbyFly() { return lobbyFlyEnabled; }
    public long getCash() { return cash; }
    public double getBalance() { return balance; }

    // Setters
    public void setBalance(double balance) { this.balance = balance; }
    public void setCash(long cash) { this.cash = cash; }

    // NOVOS SETTERS ADICIONADOS
    public void setLobbyProtectionEnabled(boolean lobbyProtectionEnabled) { this.lobbyProtectionEnabled = lobbyProtectionEnabled; }
    public void setPrivateMessagesEnabled(boolean privateMessagesEnabled) { this.privateMessagesEnabled = privateMessagesEnabled; }
    public void setCoinsReceiptEnabled(boolean coinsReceiptEnabled) { this.coinsReceiptEnabled = coinsReceiptEnabled; }
    public void setNeedsRankupConfirmation(boolean needsRankupConfirmation) { this.needsRankupConfirmation = needsRankupConfirmation; }
    public void setPrefersChatRankupAlerts(boolean prefersChatRankupAlerts) { this.prefersChatRankupAlerts = prefersChatRankupAlerts; }
    public void setHasPersonalLight(boolean hasPersonalLight) { this.hasPersonalLight = hasPersonalLight; }
    public void setLobbyFlyEnabled(boolean lobbyFlyEnabled) { this.lobbyFlyEnabled = lobbyFlyEnabled; }
}