package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.utils.ColorAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Objects;
import java.util.Optional;

public class TagManager {

    private final Main plugin;
    private final LuckPerms luckPerms;
    private final Scoreboard mainScoreboard;

    public TagManager(Main plugin) {
        this.plugin = plugin;
        this.luckPerms = plugin.getLuckPerms();
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        initializeTeams();
    }

    /**
     * Carrega todos os grupos do LuckPerms e cria/atualiza os times na scoreboard na inicialização.
     */
    private void initializeTeams() {
        luckPerms.getGroupManager().loadAllGroups().thenAcceptAsync(v -> {
            plugin.getLogger().info("Sincronizando grupos do LuckPerms com os times da scoreboard...");
            for (Group group : luckPerms.getGroupManager().getLoadedGroups()) {
                updateTeamFromGroup(group);
            }
            // Garante que todos os jogadores online tenham suas tags atualizadas após a sincronização
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerTag(player);
                }
            });
        });
    }

    /**
     * Cria ou atualiza um time na scoreboard baseado em um grupo do LuckPerms.
     * @param group O grupo do LuckPerms.
     */
    public void updateTeamFromGroup(Group group) {
        String teamName = getTeamName(group);
        Team team = mainScoreboard.getTeam(teamName);
        if (team == null) {
            team = mainScoreboard.registerNewTeam(teamName);
        }

        String prefix = group.getCachedData().getMetaData().getPrefix();
        if (prefix != null) {
            team.setPrefix(ColorAPI.format(prefix + ""));
        }

        ChatColor teamColor = getFirstColor(prefix);
        team.setColor(teamColor);
    }

    /**
     * Atualiza a tag (time da scoreboard) de um jogador específico.
     * Chamado no login do jogador ou quando seu grupo muda.
     */
    public void updatePlayerTag(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        // Encontra o grupo primário do jogador
        Group primaryGroup = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
        if (primaryGroup == null) return;

        String teamName = getTeamName(primaryGroup);
        Team team = mainScoreboard.getTeam(teamName);

        // Se o time não existir por algum motivo, tenta criá-lo na hora
        if (team == null) {
            updateTeamFromGroup(primaryGroup);
            team = mainScoreboard.getTeam(teamName);
            if (team == null) {
                plugin.getLogger().warning("Não foi possível criar ou encontrar o time para o grupo: " + primaryGroup.getName());
                return;
            }
        }

        // Adiciona o jogador ao time, atualizando sua tag no TAB e no nametag
        team.addEntry(player.getName());
        player.setDisplayName(team.getPrefix() + team.getColor() + player.getName());
    }

    /**
     * Cria um nome de time único e ordenável a partir do peso do grupo.
     * Ex: Grupo 'master' com peso 100 -> "0100-master"
     * O TAB ordena alfabeticamente, então pesos menores (ranks mais altos) vêm primeiro.
     */
    private String getTeamName(Group group) {
        int weight = group.getWeight().orElse(0);
        // Invertemos o peso para que ranks mais altos (maior peso) tenham um prefixo menor
        String formattedWeight = String.format("%04d", 9999 - weight);
        return formattedWeight + "_" + group.getName();
    }

    /**
     * Extrai a primeira cor de um prefixo para usar como cor do nome do jogador.
     */
    private ChatColor getFirstColor(String prefix) {
        if (prefix == null || prefix.isEmpty()) return ChatColor.WHITE;

        int legacyIndex = prefix.lastIndexOf('&');
        if (legacyIndex != -1 && legacyIndex + 1 < prefix.length()) {
            ChatColor color = ChatColor.getByChar(prefix.charAt(legacyIndex + 1));
            if (color != null && color.isColor()) {
                return color;
            }
        }
        return ChatColor.WHITE;
    }
}