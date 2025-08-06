// PASTA: core/src/main/java/br/com/realmmc/core/managers/TagManager.java
package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.utils.ColorAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.track.UserPromoteEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TagManager {

    private final Main plugin;
    private final LuckPerms luckPerms;

    public TagManager(Main plugin) {
        this.plugin = plugin;
        this.luckPerms = plugin.getLuckPerms();
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(new TagListener(), plugin);

        EventBus eventBus = luckPerms.getEventBus();
        eventBus.subscribe(plugin, UserPromoteEvent.class, this::onUserRankChange);

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTag(player);
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::updateAllPlayerViews, 5L);
    }

    public void updatePlayerTag(Player targetPlayer) {
        if (!targetPlayer.isOnline()) return;

        // --- LÓGICA REATORADA ---
        // Busca o RealmPlayer do cache
        CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(targetPlayer).ifPresent(realmPlayer -> {
            // Usa os dados já carregados do RealmPlayer
            int weight = realmPlayer.getGroupWeight();
            String prefix = realmPlayer.getPrefix();
            String groupName = realmPlayer.getPrimaryGroup(); // Usado para nome do time

            // O nome do time é formatado para garantir a ordem correta no TAB
            String teamName = String.format("%04d_%s", 9999 - weight, groupName.replaceAll("§.", ""));

            // Itera sobre todos os jogadores online (os "espectadores")
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                Scoreboard viewerScoreboard = viewer.getScoreboard();
                Team team = viewerScoreboard.getTeam(teamName);

                if (team == null) {
                    team = viewerScoreboard.registerNewTeam(teamName);
                }

                if (prefix != null) {
                    team.setPrefix(ColorAPI.format(prefix + ""));
                    String lastColors = ChatColor.getLastColors(ColorAPI.format(prefix));
                    if (!lastColors.isEmpty()) {
                        ChatColor nameColor = ChatColor.getByChar(lastColors.charAt(1));
                        if (nameColor != null && nameColor.isColor()) {
                            team.setColor(nameColor);
                        }
                    }
                }

                // Adiciona o jogador-alvo ao time na scoreboard do espectador
                team.addEntry(targetPlayer.getName());
            }
        });
    }

    private void updateAllPlayerViews() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTag(player);
        }
    }

    private void onUserRankChange(UserPromoteEvent event) {
        Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
        if (player != null && player.isOnline()) {
            // Um delay é importante para garantir que o cache do RealmPlayer seja atualizado primeiro
            Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerTag(player), 20L);
        }
    }

    private class TagListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            // Atualiza a tag de todos para todos, garantindo sincronia total com o novo jogador.
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateAllPlayerViews(), 10L);
        }
    }
}