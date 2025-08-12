// PASTA: core/src/main/java/br/com/realmmc/core/listeners/DefaultChatListener.java
package br.com.realmmc.core.listeners;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.utils.InteractiveChatFormatter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class DefaultChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDefaultChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String originalMessage = event.getMessage();

        if (CoreAPI.getInstance().getSpamManager().isSpam(player, originalMessage, "core_chat")) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        String messageColor = player.hasPermission("core.champion") ? "&f" : "&7";
        String finalMessage = messageColor + originalMessage;

        TranslationsManager translations = CoreAPI.getInstance().getTranslationsManager();
        ConfigurationSection formatSection = translations.getConfig().getConfigurationSection("chat.default-format");

        // --- LÓGICA REATORADA ---
        CoreAPI.getInstance().getPlugin().getServer().getScheduler().runTask(CoreAPI.getInstance().getPlugin(), () -> {
            CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(player).ifPresent(realmPlayer -> {
                String formattedName = realmPlayer.getPrefix() + realmPlayer.getUsername();
                InteractiveChatFormatter.formatAndSend(player, finalMessage, formatSection, formattedName);
            });
        });
    }
}