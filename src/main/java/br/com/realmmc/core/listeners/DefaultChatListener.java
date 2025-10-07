package br.com.realmmc.core.listeners;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.utils.ColorAPI;
import br.com.realmmc.core.utils.InteractiveChatFormatter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class DefaultChatListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDefaultChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message.startsWith("!!") && player.hasPermission("core.manager")) {
            event.setCancelled(true);
            String alertMessage = message.substring(2).trim();
            if (alertMessage.isEmpty()) return;

            CoreAPI.getInstance().getPlayerManager().getFormattedNicknameAsync(player.getName()).thenAccept(formattedNameOpt -> {
                String formattedName = formattedNameOpt.orElse(player.getName());

                Bukkit.getScheduler().runTask(CoreAPI.getInstance().getPlugin(), () -> {
                    String finalMessage = CoreAPI.getInstance().getTranslationsManager().getRawMessage("chat.alert",
                            "player_name", formattedName, "message", alertMessage);

                    Bukkit.broadcastMessage("§f");
                    Bukkit.broadcastMessage(ColorAPI.format(finalMessage));
                    Bukkit.broadcastMessage("§f");
                    Bukkit.getOnlinePlayers().forEach(p -> CoreAPI.getInstance().getSoundManager().playNotification(p));
                });
            });
            return;
        }

        if(event.isCancelled()) return;

        event.setCancelled(true);

        String finalMessage = player.hasPermission("core.champion") ? message : "§7" + message;

        ConfigurationSection formatSection = CoreAPI.getInstance().getTranslationsManager().getConfig().getConfigurationSection("chat.default-format");

        CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(player).ifPresent(realmPlayer -> {
            String formattedName = realmPlayer.getPrefix() + realmPlayer.getUsername();
            Bukkit.getScheduler().runTask(CoreAPI.getInstance().getPlugin(), () -> {
                InteractiveChatFormatter.formatAndSend(player, finalMessage, formatSection, formattedName);
            });
        });
    }
}