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
    private static final int MAX_PREFIX_LENGTH = 64; // Limite de segurança para o tamanho do prefixo
    private static final int MAX_TEAM_NAME_LENGTH = 16; // Limite MÁXIMO para nomes de time

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

        CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(targetPlayer).ifPresent(realmPlayer -> {
            int weight = realmPlayer.getGroupWeight();
            String prefix = realmPlayer.getPrefix();
            // Pega o nome do grupo principal do jogador, já sem cores.
            String rawGroupName = ChatColor.stripColor(realmPlayer.getPrimaryGroup());

            if (prefix != null) {
                if (prefix.length() > MAX_PREFIX_LENGTH) {
                    prefix = prefix.substring(0, MAX_PREFIX_LENGTH);
                }
            } else {
                prefix = "";
            }

            // Cria o nome do time com base no peso e no nome do grupo
            String teamName = String.format("%04d_%s", 9999 - weight, rawGroupName);

            // ===================================================================================== //
            //                                CORREÇÃO FINAL APLICADA AQUI                           //
            // ===================================================================================== //
            // Garante que o nome do time NUNCA exceda o limite de 16 caracteres do Minecraft.
            if (teamName.length() > MAX_TEAM_NAME_LENGTH) {
                teamName = teamName.substring(0, MAX_TEAM_NAME_LENGTH);
            }
            // ===================================================================================== //

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                Scoreboard viewerScoreboard = viewer.getScoreboard();
                Team team = viewerScoreboard.getTeam(teamName);

                if (team == null) {
                    team = viewerScoreboard.registerNewTeam(teamName);
                }

                String formattedPrefix = ColorAPI.format(prefix);
                if (!team.getPrefix().equals(formattedPrefix)) {
                    team.setPrefix(formattedPrefix);
                }

                String lastColors = ChatColor.getLastColors(formattedPrefix);
                if (!lastColors.isEmpty()) {
                    ChatColor nameColor = ChatColor.getByChar(lastColors.charAt(lastColors.length() - 1));
                    if (nameColor != null && nameColor.isColor() && team.getColor() != nameColor) {
                        team.setColor(nameColor);
                    }
                }

                if (!team.hasEntry(targetPlayer.getName())) {
                    team.addEntry(targetPlayer.getName());
                }
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
            Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerTag(player), 20L);
        }
    }

    private class TagListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateAllPlayerViews(), 10L);
        }
    }
}