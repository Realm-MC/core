package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.profile.PreferencesGUI;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class OpenGuiListener implements PluginMessageListener {

    private final Main plugin;

    public OpenGuiListener(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals("proxy:opengui")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("OpenPreferences")) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                CoreAPI.getInstance().getSoundManager().playClick(player);
                new PreferencesGUI(player).open();
            });
        }
    }
}