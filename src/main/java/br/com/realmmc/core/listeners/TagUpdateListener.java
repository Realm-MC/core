package br.com.realmmc.core.listeners;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.managers.TagManager;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.track.UserPromoteEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TagUpdateListener {

    private final Main plugin;
    private final TagManager tagManager;

    public TagUpdateListener(Main plugin) {
        this.plugin = plugin;
        this.tagManager = plugin.getTagManager();

        // Escuta pelos eventos da API do LuckPerms
        EventBus eventBus = plugin.getLuckPerms().getEventBus();
        // Adicione mais eventos se necessário (ex: demote, parent add/remove)
        eventBus.subscribe(plugin, UserPromoteEvent.class, this::onUserRankChange);
    }

    private void onUserRankChange(UserPromoteEvent event) {
        UUID uuid = event.getUser().getUniqueId();
        Player player = Bukkit.getPlayer(uuid);

        if (player != null && player.isOnline()) {
            // Adiciona um pequeno delay para garantir que o LuckPerms salvou os dados
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                tagManager.updatePlayerTag(player);
            }, 20L); // 1 segundo de delay
        }
    }
}