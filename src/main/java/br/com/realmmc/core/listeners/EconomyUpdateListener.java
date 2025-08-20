package br.com.realmmc.core.listeners;

import br.com.realmmc.core.api.CoreAPI;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EconomyUpdateListener implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals("proxy:economy_update")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("BalanceUpdate")) {
            UUID playerUuid = UUID.fromString(in.readUTF());
            String currency = in.readUTF();
            long newBalance = in.readLong();

            CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(playerUuid).ifPresent(realmPlayer -> {
                if (currency.equalsIgnoreCase("CASH")) {
                    realmPlayer.setCash(newBalance);
                }
            });
        }
    }
}