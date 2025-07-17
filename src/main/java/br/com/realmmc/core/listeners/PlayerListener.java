package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null); // Remove a mensagem de entrada padrão do Minecraft

        // Define o estado inicial do jogador no lobby
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);

        // Garante que o perfil de preferências seja criado
        CoreAPI.getInstance().getUserPreferenceManager().ensurePreferenceProfile(player);

        // Atualiza a tag do jogador (prefixo/cor no TAB e nametag)
        CoreAPI.getInstance().getTagManager().updatePlayerTag(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(null); // Remove a mensagem de saída padrão

        // A limpeza do GodMode é feita pelo próprio GodManager, que é um Listener.
        // Limpamos apenas os cooldowns aqui.
        CoreAPI.getInstance().getDelayManager().clearCooldowns(player.getUniqueId());
    }
}