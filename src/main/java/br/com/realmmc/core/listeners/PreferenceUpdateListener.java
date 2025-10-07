package br.com.realmmc.core.listeners;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.Gui;
import br.com.realmmc.core.gui.profile.ChatPreferencesGUI;
import br.com.realmmc.core.gui.profile.LobbyPreferencesGUI;
import br.com.realmmc.core.gui.profile.RankupPreferencesGUI;
import br.com.realmmc.core.player.RealmPlayer;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class PreferenceUpdateListener implements PluginMessageListener {

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player anyPlayer, @NotNull byte[] message) {
        if (!channel.equals("proxy:preference_update")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("PreferenceUpdate")) {
            UUID playerUuid = UUID.fromString(in.readUTF());
            String preferenceKey = in.readUTF();

            Optional<RealmPlayer> realmPlayerOpt = CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(playerUuid);

            realmPlayerOpt.ifPresent(realmPlayer -> {
                Player onlinePlayer = Bukkit.getPlayer(playerUuid);

                if (preferenceKey.equalsIgnoreCase("LobbyTime")) {
                    String newValue = in.readUTF();
                    realmPlayer.setLobbyTimePreference(newValue);
                } else {
                    boolean newValue = in.readBoolean();
                    updateBooleanPreference(realmPlayer, preferenceKey, newValue);
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        sendFeedbackMessage(onlinePlayer, preferenceKey, newValue);
                    }
                }

                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    refreshOpenGui(onlinePlayer);
                }
            });
        }
    }

    private void sendFeedbackMessage(Player player, String key, boolean newState) {
        String messageKey = switch (key.toLowerCase()) {
            case "lobbyprotection" -> newState ? "toggle.lobby-protection.enabled" : "toggle.lobby-protection.disabled";
            case "playertell" -> newState ? "toggle.tell.enabled" : "toggle.tell.disabled";
            case "coinsreceipt" -> newState ? "toggle.coins-receipt.enabled" : "toggle.coins-receipt.disabled";
            case "rankupconfirmation" -> newState ? "toggle.rankup-confirm.enabled" : "toggle.rankup-confirm.disabled";
            case "rankupalert" -> newState ? "toggle.rankup-alert.enabled" : "toggle.rankup-alert.disabled";
            case "rankuppersonallight" -> newState ? "toggle.personal-light.enabled" : "toggle.personal-light.disabled";
            case "showrankprefix" -> newState ? "toggle.show-rank-prefix.enabled" : "toggle.show-rank-prefix.disabled";
            default -> "";
        };
        if (!messageKey.isEmpty()) {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(player, messageKey);
        }
    }

    private void updateBooleanPreference(RealmPlayer realmPlayer, String key, boolean newState) {
        switch (key.toLowerCase()) {
            case "lobbyprotection" -> realmPlayer.setLobbyProtectionEnabled(newState);
            case "playertell" -> realmPlayer.setPrivateMessagesEnabled(newState);
            case "coinsreceipt" -> realmPlayer.setCoinsReceiptEnabled(newState);
            case "rankupconfirmation" -> realmPlayer.setNeedsRankupConfirmation(newState);
            case "rankupalert" -> realmPlayer.setPrefersChatRankupAlerts(newState);
            case "rankuppersonallight" -> realmPlayer.setHasPersonalLight(newState);
            case "lobbyfly" -> realmPlayer.setLobbyFlyEnabled(newState);
            case "showrankprefix" -> realmPlayer.setShowRankPrefixEnabled(newState);
        }
    }

    private void refreshOpenGui(Player player) {
        InventoryView openInventory = player.getOpenInventory();
        Inventory topInventory = openInventory.getTopInventory();

        if (topInventory != null) {
            InventoryHolder holder = topInventory.getHolder();

            if (holder instanceof Gui gui) {
                if (gui instanceof LobbyPreferencesGUI || gui instanceof ChatPreferencesGUI || gui instanceof RankupPreferencesGUI) {
                    Bukkit.getScheduler().runTask(CoreAPI.getInstance().getPlugin(), gui::open);
                }
            }
        }
    }
}