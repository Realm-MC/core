package br.com.realmmc.core.listeners;

import br.com.realmmc.core.api.CoreAPI;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PreferenceUpdateListener implements PluginMessageListener {

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals("proxy:preference_update")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("PreferenceUpdate")) {
            UUID playerUuid = UUID.fromString(in.readUTF());
            String preferenceKey = in.readUTF();
            boolean newState = in.readBoolean();

            CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(playerUuid).ifPresent(realmPlayer -> {
                switch (preferenceKey) {
                    case "rankup_alert_status" -> realmPlayer.setPrefersChatRankupAlerts(newState);
                    case "rankup_confirmation_status" -> realmPlayer.setNeedsRankupConfirmation(newState);
                    // Adicionar outros casos se criar mais toggles no futuro
                }
            });
        }
    }
}