package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer; // Importe o Serializer
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class TitleBroadcastListener implements PluginMessageListener {

    private final Main plugin;

    public TitleBroadcastListener(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equalsIgnoreCase("proxy:broadcast")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("GlobalTitle")) {
            String titleString = in.readUTF();
            String subtitleString = in.readUTF();

            Component title = LegacyComponentSerializer.legacyAmpersand().deserialize(titleString);
            Component subtitle = LegacyComponentSerializer.legacyAmpersand().deserialize(subtitleString);

            Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500));
            Title fullTitle = Title.title(title, subtitle, times);

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.showTitle(fullTitle);
            }
        }
    }
}