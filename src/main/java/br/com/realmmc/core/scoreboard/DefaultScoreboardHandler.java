// PASTA: core/src/main/java/br/com/realmmc/core/scoreboard/DefaultScoreboardHandler.java
package br.com.realmmc.core.scoreboard;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.player.RealmPlayer;
import br.com.realmmc.core.utils.ColorAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class DefaultScoreboardHandler {

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;

    public DefaultScoreboardHandler(Player player) {
        this.player = player;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        String title = CoreAPI.getInstance().getTranslationsManager().getRawMessage("scoreboard.title");
        this.objective = scoreboard.registerNewObjective("Realm_Core", "dummy", ColorAPI.format(title));
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        createLine(7, "§f");
        createLine(6, "§fCargo:");
        createLine(5, "§fOnline:");
        createLine(4, "§f ");

        String serverName = CoreAPI.getInstance().getPlugin().getServerName();
        createLine(3, "§fServidor: §a" + serverName);

        createLine(2, "§f  ");
        createLine(1, CoreAPI.getInstance().getTranslationsManager().getMessage("scoreboard.site"));

        player.setScoreboard(this.scoreboard);
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * O método update agora recebe o objeto RealmPlayer, tornando a lógica síncrona e instantânea.
     * @param realmPlayer O objeto com todos os dados do jogador já em cache.
     */
    public void update(RealmPlayer realmPlayer) {
        if (!player.isOnline()) return;

        // Acesso direto e instantâneo aos dados, sem chamadas assíncronas!
        String groupName = realmPlayer.getPrimaryGroup();

        setLine(6, "§fCargo: " + groupName);
        setLine(5, "§fOnline: §a" + Bukkit.getOnlinePlayers().size());
    }

    public void destroy() {
        if (player.isOnline() && player.getScoreboard() == this.scoreboard) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void createLine(int score, String text) {
        Team team = scoreboard.registerNewTeam("line-" + score);
        String entry = ChatColor.values()[score].toString();
        team.addEntry(entry);
        team.setPrefix(text);
        objective.getScore(entry).setScore(score);
    }

    private void setLine(int score, String text) {
        Team team = scoreboard.getTeam("line-" + score);
        if (team != null) {
            String limitedText = text.length() > 64 ? text.substring(0, 64) : text;
            if (!team.getPrefix().equals(limitedText)) {
                team.setPrefix(ColorAPI.format(limitedText));
            }
        }
    }
}