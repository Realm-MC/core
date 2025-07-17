package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class SoundListener implements PluginMessageListener {

    private final Main plugin;

    public SoundListener(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player anyPlayer, byte[] message) {
        if (!channel.equals("proxy:sounds")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("PlaySound")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    String soundTypeName = in.readUTF();
                    if (anyPlayer == null || !anyPlayer.isOnline()) return;

                    switch (soundTypeName) {
                        case "SUCCESS":
                            CoreAPI.getInstance().getSoundManager().playSuccess(anyPlayer);
                            break;
                        case "ERROR":
                            CoreAPI.getInstance().getSoundManager().playError(anyPlayer);
                            break;
                        case "CLICK":
                            CoreAPI.getInstance().getSoundManager().playClick(anyPlayer);
                            break;
                        case "TELEPORT":
                            CoreAPI.getInstance().getSoundManager().playTeleport(anyPlayer);
                            break;
                        case "LEVEL_UP":
                            CoreAPI.getInstance().getSoundManager().playLevelUp(anyPlayer);
                            break;
                        case "NOTIFICATION":
                            CoreAPI.getInstance().getSoundManager().playNotification(anyPlayer);
                            break;
                        default:
                            plugin.getLogger().warning("Recebido tipo de som desconhecido do Proxy: " + soundTypeName);
                            break;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Recebida uma mensagem de som mal formatada do Proxy.");
                }
            });
        }
    }
}