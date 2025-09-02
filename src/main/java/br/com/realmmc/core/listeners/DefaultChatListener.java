package br.com.realmmc.core.listeners;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.utils.InteractiveChatFormatter;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.logging.Level;

public class DefaultChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDefaultChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String originalMessage = event.getMessage();

        event.setCancelled(true);

        CoreAPI.getInstance().getPunishmentReader().isActiveMute(player.getUniqueId()).thenAccept(isMuted -> {
            if (isMuted) {
                Bukkit.getScheduler().runTask(CoreAPI.getInstance().getPlugin(), () -> {
                    CoreAPI.getInstance().getSoundManager().playError(player);

                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("RequestMuteMessage");
                    out.writeUTF(player.getUniqueId().toString());
                    player.sendPluginMessage(CoreAPI.getInstance().getPlugin(), "core:punishment_notify", out.toByteArray());
                });
            } else {
                if (CoreAPI.getInstance().getSpamManager().isSpam(player, originalMessage, "core_chat")) {
                    return;

                }

                String messageColor = player.hasPermission("core.champion") ? "&f" : "&7";
                String finalMessage = messageColor + originalMessage;

                TranslationsManager translations = CoreAPI.getInstance().getTranslationsManager();
                ConfigurationSection formatSection = translations.getConfig().getConfigurationSection("chat.default-format");

                Bukkit.getScheduler().runTask(CoreAPI.getInstance().getPlugin(), () -> {
                    CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(player).ifPresent(realmPlayer -> {
                        String formattedName = realmPlayer.getPrefix() + realmPlayer.getUsername();
                        InteractiveChatFormatter.formatAndSend(player, finalMessage, formatSection, formattedName);
                    });
                });
            }
        }).exceptionally(ex -> {
            Bukkit.getLogger().log(Level.WARNING, "Erro ao verificar status de mute para " + player.getName(), ex);
            player.chat(originalMessage);
            return null;
        });
    }
}