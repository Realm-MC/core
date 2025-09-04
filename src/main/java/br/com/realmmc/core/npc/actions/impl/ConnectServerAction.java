package br.com.realmmc.core.npc.actions.impl;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.npc.NPC;
import br.com.realmmc.core.npc.actions.NPCAction;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;

public class ConnectServerAction implements NPCAction {
    @Override
    public void execute(Player player, NPC npc) {
        if (npc.getActionValues().isEmpty()) return;

        String serverName = npc.getActionValues().get(0);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);

        player.sendPluginMessage(CoreAPI.getInstance().getPlugin(), "BungeeCord", out.toByteArray());
    }
}