package br.com.realmmc.core.tablist;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.TranslationsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

public class TablistManager {

    private final JavaPlugin plugin;
    private final TranslationsManager translations;
    private final DecimalFormat tpsFormat = new DecimalFormat("0.00");

    public TablistManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.translations = CoreAPI.getInstance().getTranslationsManager();
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateTablist(player);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 40L); // Atualiza a cada 2 segundos
    }

    private void updateTablist(Player player) {
        String serverDisplayName = CoreAPI.getInstance().getServerConfigManager().getServerDisplayName();

        List<String> headerLines = translations.getConfig().getStringList("tablist.header");
        String header = headerLines.stream()
                .map(line -> line.replace("{server_display_name}", serverDisplayName))
                .map(this::formatColors)
                .collect(Collectors.joining("\n"));

        List<String> footerLines = translations.getConfig().getStringList("tablist.footer.normal");
        String footer = footerLines.stream()
                .map(this::formatColors)
                .collect(Collectors.joining("\n"));

        if (player.hasPermission("core.manager")) {
            footer += "\n" + buildStaffFooter(player);
        }

        player.setPlayerListHeaderFooter(header, footer);
    }

    private String buildStaffFooter(Player player) {
        double tps = Bukkit.getServer().getTPS()[0];
        String formattedTps = tpsFormat.format(tps);
        String tpsColor = (tps >= 18) ? "§a" : (tps >= 15) ? "§e" : "§c";

        int ping = player.getPing();
        String pingColor = (ping <= 100) ? "§a" : (ping <= 200) ? "§e" : "§c";

        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576; // Em MB

        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        String playerString = (onlinePlayers == 1) ? "jogador" : "jogadores";

        List<String> staffFooterLines = translations.getConfig().getStringList("tablist.footer.staff");
        return staffFooterLines.stream()
                .map(line -> line.replace("{tps}", tpsColor + formattedTps)
                        .replace("{ping}", pingColor + ping + "ms")
                        .replace("{memory_usage}", usedMemory + "MB")
                        .replace("{online_players}", String.valueOf(onlinePlayers))
                        .replace("{player_string}", playerString))
                .map(this::formatColors)
                .collect(Collectors.joining("\n"));
    }

    private String formatColors(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}