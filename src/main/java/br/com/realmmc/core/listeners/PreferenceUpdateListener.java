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
        String messageKey = "";
        switch (key.toLowerCase()) {
            case "lobbyprotection": messageKey = newState ? "toggle.lobby-protection.enabled" : "toggle.lobby-protection.disabled"; break;
            case "playertell": messageKey = newState ? "toggle.tell.enabled" : "toggle.tell.disabled"; break;
            case "coinsreceipt": messageKey = newState ? "toggle.coins-receipt.enabled" : "toggle.coins-receipt.disabled"; break;
            case "rankupconfirmation": messageKey = newState ? "toggle.rankup-confirm.enabled" : "toggle.rankup-confirm.disabled"; break;
            case "rankupalert": messageKey = newState ? "toggle.rankup-alert.enabled" : "toggle.rankup-alert.disabled"; break;
            case "rankuppersonallight": messageKey = newState ? "toggle.personal-light.enabled" : "toggle.personal-light.disabled"; break;
        }
        if (!messageKey.isEmpty()) {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(player, messageKey);
        }
    }

    private void updateBooleanPreference(RealmPlayer realmPlayer, String key, boolean newState) {
        switch (key.toLowerCase()) {
            case "lobbyprotection": realmPlayer.setLobbyProtectionEnabled(newState); break;
            case "playertell": realmPlayer.setPrivateMessagesEnabled(newState); break;
            case "coinsreceipt": realmPlayer.setCoinsReceiptEnabled(newState); break;
            case "rankupconfirmation": realmPlayer.setNeedsRankupConfirmation(newState); break;
            case "rankupalert": realmPlayer.setPrefersChatRankupAlerts(newState); break;
            case "rankuppersonallight": realmPlayer.setHasPersonalLight(newState); break;
            case "lobbyfly": realmPlayer.setLobbyFlyEnabled(newState); break;
        }
    }

    private void refreshOpenGui(Player player) {
        Gui openGui = CoreAPI.getInstance().getGuiManager().getOpenGuis().get(player.getUniqueId());

        if (openGui instanceof LobbyPreferencesGUI ||
                openGui instanceof ChatPreferencesGUI ||
                openGui instanceof RankupPreferencesGUI) {
            Bukkit.getScheduler().runTask(CoreAPI.getInstance().getPlugin(), openGui::open);
        }
    }
}