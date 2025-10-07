package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishListener implements PluginMessageListener, Listener {

    private final Main plugin;
    // <-- ADICIONADO: Lista local para saber quem está em vanish
    private final Set<UUID> vanishedPlayers = Collections.synchronizedSet(new HashSet<>());
    private static final String VANISH_SEE_PERMISSION = "proxy.moderator";
    private boolean initialSyncRequested = false;

    public VanishListener(Main plugin) {
        this.plugin = plugin;
    }

    // <-- ADICIONADO: Método público para outros listeners verificarem o status
    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player anyPlayer, byte[] message) {
        if (!channel.equals("proxy:vanish") && !channel.equals("proxy:sync")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("GlobalUpdate")) {
            handleVanishUpdate(in, true);
        } else if (subChannel.equals("SpecificUpdate")) {
            handleVanishUpdate(in, false);
        }
    }

    private void handleVanishUpdate(ByteArrayDataInput in, boolean isGlobal) {
        if (isGlobal) {
            UUID targetUuid = UUID.fromString(in.readUTF());
            boolean shouldHide = in.readBoolean();

            if (shouldHide) {
                vanishedPlayers.add(targetUuid); // <-- ADICIONADO
                Player target = Bukkit.getPlayer(targetUuid);
                if (target != null) hidePlayerFromServer(target);
            } else {
                vanishedPlayers.remove(targetUuid); // <-- ADICIONADO
                Player target = Bukkit.getPlayer(targetUuid);
                if (target != null) showPlayerFromServer(target);
            }
        } else {
            UUID viewerUuid = UUID.fromString(in.readUTF());
            UUID targetUuid = UUID.fromString(in.readUTF());
            Player viewer = Bukkit.getPlayer(viewerUuid);
            Player target = Bukkit.getPlayer(targetUuid);
            if (viewer != null && target != null && !viewer.hasPermission(VANISH_SEE_PERMISSION)) {
                viewer.hidePlayer(plugin, target);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joinedPlayer = event.getPlayer();

        if (!initialSyncRequested && !plugin.getServer().getOnlinePlayers().isEmpty()) {
            if (plugin.getServer().getOnlinePlayers().size() == 1 && plugin.getServer().getOnlinePlayers().contains(joinedPlayer)) {
                initialSyncRequested = true;
                requestVanishSync(joinedPlayer);
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerView(joinedPlayer), 10L);
    }

    // <-- ADICIONADO: Limpa o status de vanish quando o jogador sai
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        vanishedPlayers.remove(event.getPlayer().getUniqueId());
    }

    private void requestVanishSync(Player p) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("VanishSyncRequest");
        p.sendPluginMessage(plugin, "proxy:sync", out.toByteArray());
    }

    private void updatePlayerView(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;
        if (!viewer.hasPermission(VANISH_SEE_PERMISSION)) {
            for (UUID vanishedUuid : vanishedPlayers) {
                Player vanishedPlayer = Bukkit.getPlayer(vanishedUuid);
                if (vanishedPlayer != null && vanishedPlayer.isOnline()) {
                    viewer.hidePlayer(plugin, vanishedPlayer);
                }
            }
        }
    }

    private void hidePlayerFromServer(Player vanishedPlayer) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.hasPermission(VANISH_SEE_PERMISSION)) {
                onlinePlayer.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    private void showPlayerFromServer(Player vanishedPlayer) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.showPlayer(plugin, vanishedPlayer);
        }
    }
}