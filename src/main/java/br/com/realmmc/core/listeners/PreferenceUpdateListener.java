package br.com.realmmc.core.listeners;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.player.RealmPlayer;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
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
            boolean newState = in.readBoolean();

            Optional<RealmPlayer> realmPlayerOpt = CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(playerUuid);

            realmPlayerOpt.ifPresent(realmPlayer -> {
                boolean updated = true;
                switch (preferenceKey.toLowerCase()) {
                    case "rankupconfirmation":
                        realmPlayer.setNeedsRankupConfirmation(newState);
                        break;
                    case "rankupalert":
                        realmPlayer.setPrefersChatRankupAlerts(newState);
                        break;
                    case "rankuppersonallight":
                        realmPlayer.setHasPersonalLight(newState);
                        break;
                    case "lobbyfly":
                        realmPlayer.setLobbyFly(newState);
                        break;
                    default:
                        updated = false;
                        break;
                }

                if (updated) {
                    Player onlinePlayer = Bukkit.getPlayer(playerUuid);
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        sendPreferenceAppliedMessage(onlinePlayer, preferenceKey, newState);
                    }
                }
            });
        }
    }

    private void sendPreferenceAppliedMessage(Player player, String preferenceKey, boolean newState) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PreferenceApplied");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(preferenceKey);
        out.writeBoolean(newState);

        player.sendPluginMessage(CoreAPI.getInstance().getPlugin(), "core:preference_applied", out.toByteArray());
    }
}