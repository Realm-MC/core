package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;
import java.util.logging.Level;

public class TeleportListener implements PluginMessageListener {

    private final Main plugin;

    public TeleportListener(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player anyPlayer, byte[] message) {
        if (!channel.equals("proxy:teleport")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("FinalTeleport")) {
            try {
                UUID playerToMoveUUID = UUID.fromString(in.readUTF());
                UUID targetPlayerUUID = UUID.fromString(in.readUTF());

                // Adicionado delay para garantir que o jogador carregou o mundo
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Player playerToMove = Bukkit.getPlayer(playerToMoveUUID);
                    Player targetPlayer = Bukkit.getPlayer(targetPlayerUUID);

                    if (playerToMove != null && targetPlayer != null) {
                        playerToMove.teleport(targetPlayer.getLocation());
                        CoreAPI.getInstance().getSoundManager().playTeleport(playerToMove);
                    } else {
                        // Log aprimorado para depuração
                        plugin.getLogger().warning("[TeleportListener] Falha ao teleportar. PlayerToMove encontrado: " + (playerToMove != null) + ". TargetPlayer encontrado: " + (targetPlayer != null) + ".");
                    }
                }, 20L); // 1 segundo de atraso

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Recebida uma mensagem de teleporte mal formatada do Proxy.", e);
            }
        }
    }
}