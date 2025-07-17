package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.DelayManager;
import br.com.realmmc.core.managers.ServerConfigManager;
import br.com.realmmc.core.managers.TranslationsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final TranslationsManager translationsManager;
    private final DelayManager delayManager;
    private final ServerConfigManager serverConfigManager;
    private final String bypassPermission = "proxy.champion";

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
        this.translationsManager = plugin.getTranslationsManager();
        this.delayManager = plugin.getDelayManager();
        this.serverConfigManager = plugin.getServerConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = serverConfigManager.getMaxPlayers();

        if (onlinePlayers >= maxPlayers) {
            if (!event.getPlayer().hasPermission(this.bypassPermission)) {
                event.setResult(PlayerLoginEvent.Result.KICK_FULL);
                event.setKickMessage(translationsManager.getMessage("general.server-full-no-vip"));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.getMessage().startsWith("/")) return;

        Player player = event.getPlayer();
        // A checagem de mute é assíncrona para não travar o servidor
        plugin.getPunishmentReader().isActiveMute(player.getUniqueId()).thenAccept(isMuted -> {
            if (isMuted) {
                // Roda na thread principal do servidor para cancelar o evento e enviar o som
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    event.setCancelled(true);
                    // Usa o CoreAPI para enviar o som de erro como feedback
                    CoreAPI.getInstance().getSoundManager().playError(player);
                    // A mensagem de "você está mutado" é enviada pelo Proxy
                });
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        delayManager.clearCooldowns(event.getPlayer().getUniqueId());
    }
}