package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.ActionBarManager;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

public class ActionBarListener implements PluginMessageListener {

    public ActionBarListener(Main plugin) {
    }

    @Override
    public void onPluginMessageReceived(String channel, Player anyPlayer, byte[] message) {
        if (!channel.equals("proxy:actionbar")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String action = in.readUTF();
        UUID playerUuid = UUID.fromString(in.readUTF());
        String messageId = in.readUTF(); // Lê o ID da mensagem

        Player targetPlayer = Bukkit.getPlayer(playerUuid);
        if (targetPlayer == null || !targetPlayer.isOnline()) return;

        // --- ATUALIZAÇÃO DA LÓGICA ---
        if (action.equals("Show")) {
            String text = in.readUTF();
            // Passa o ID "vanish" para o manager
            CoreAPI.getInstance().getActionBarManager().setMessage(targetPlayer, ActionBarManager.MessagePriority.HIGH, messageId, text, -1);
        } else if (action.equals("Clear")) {
            // Limpa a mensagem com o ID "vanish"
            CoreAPI.getInstance().getActionBarManager().clearMessage(targetPlayer, messageId);
        }
    }
}