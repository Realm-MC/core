package br.com.realmmc.core.managers;

import br.com.realmmc.core.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ServerConfigManager {

    private final Main plugin;
    private String serverId;
    private String serverDisplayName;
    private int maxPlayers;
    private boolean isWhitelistEnabled;
    private final File configFile;
    private FileConfiguration config;

    public ServerConfigManager(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "server.yml");
        if (!configFile.exists()) {
            plugin.saveResource("server.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadConfig();
    }

    private void loadConfig() {
        this.serverId = config.getString("server-id", "unknown");
        this.serverDisplayName = config.getString("display-name", "Servidor");
        this.maxPlayers = config.getInt("max-players", 100);
        this.isWhitelistEnabled = config.getBoolean("whitelist-enabled", false);
    }

    public String getServerId() {
        return serverId;
    }

    public String getServerDisplayName() {
        return serverDisplayName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isWhitelistEnabled() {
        return isWhitelistEnabled;
    }

    public void setWhitelistEnabled(boolean enabled) {
        this.isWhitelistEnabled = enabled;
        config.set("whitelist-enabled", enabled);
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Falha ao salvar a configuração do servidor (server.yml)");
        }
    }
}