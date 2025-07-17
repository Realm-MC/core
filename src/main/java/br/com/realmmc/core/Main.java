package br.com.realmmc.core;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.commands.FeedCommand;
import br.com.realmmc.core.commands.GamemodeCommand;
import br.com.realmmc.core.commands.GodCommand;
import br.com.realmmc.core.commands.HealCommand;
import br.com.realmmc.core.commands.TeleportCommand;
import br.com.realmmc.core.commands.TeleportHereCommand;
import br.com.realmmc.core.listeners.*;
import br.com.realmmc.core.managers.*;
import br.com.realmmc.core.managers.GodManager;
import br.com.realmmc.core.player.PlayerManager;
import br.com.realmmc.core.punishments.PunishmentReader;
import br.com.realmmc.core.users.UserProfileReader;
import br.com.realmmc.core.users.UserPreferenceManager;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class Main extends JavaPlugin {

    private LuckPerms luckPerms;
    private ViaAPI<?> viaAPI;
    private DatabaseManager databaseManager;
    private TranslationsManager translationsManager;
    private UserProfileReader userProfileReader;
    private UserPreferenceManager userPreferenceManager;
    private PlayerManager playerManager;
    private SoundManager soundManager;
    private ActionBarManager actionBarManager;
    private DelayManager delayManager;
    private ServerConfigManager serverConfigManager;
    private PunishmentReader punishmentReader;
    private GodManager godManager;
    private String serverName;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            this.serverName = new File(".").getCanonicalFile().getName();
        } catch (IOException e) {
            getLogger().severe("NÃO FOI POSSÍVEL DETECTAR O NOME DO SERVIDOR.");
            this.serverName = "server";
        }

        this.translationsManager = new TranslationsManager(this);

        try {
            this.databaseManager = new DatabaseManager(this);
        } catch (Exception e) {
            this.translationsManager.log(Level.SEVERE, "logs.database.connection-failed", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupLuckPerms()) {
            this.translationsManager.log(Level.SEVERE, "logs.dependencies.luckperms-not-found");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setupViaVersion();

        this.serverConfigManager = new ServerConfigManager(this);
        this.userProfileReader = new UserProfileReader(this);
        this.userPreferenceManager = new UserPreferenceManager(this);
        this.playerManager = new PlayerManager(this);
        this.soundManager = new SoundManager();
        this.actionBarManager = new ActionBarManager(this);
        this.delayManager = new DelayManager(this);
        this.punishmentReader = new PunishmentReader(this);
        this.godManager = new GodManager();

        new CoreAPI(this);

        registerComponents();

        this.translationsManager.log(Level.INFO, "logs.plugin.enabled");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.translationsManager.log(Level.INFO, "logs.plugin.disabled");
    }

    private void registerComponents() {
        PluginManager pm = getServer().getPluginManager();

        // Listeners
        VanishListener vanishListener = new VanishListener(this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(vanishListener, this);
        pm.registerEvents(this.godManager, this);

        // Comandos
        getCommand("god").setExecutor(new GodCommand(this.godManager));
        GamemodeCommand gamemodeCommand = new GamemodeCommand();
        getCommand("gamemode").setExecutor(gamemodeCommand);
        getCommand("gamemode").setTabCompleter(gamemodeCommand);
        HealCommand healCommand = new HealCommand();
        getCommand("heal").setExecutor(healCommand);
        getCommand("heal").setTabCompleter(healCommand);

        FeedCommand feedCommand = new FeedCommand();
        getCommand("feed").setExecutor(feedCommand);
        getCommand("feed").setTabCompleter(feedCommand);
        TeleportCommand teleportCommand = new TeleportCommand();
        getCommand("teleport").setExecutor(teleportCommand);
        getCommand("teleport").setTabCompleter(teleportCommand);
        TeleportHereCommand teleportHereCommand = new TeleportHereCommand();
        getCommand("teleporthere").setExecutor(teleportHereCommand);
        getCommand("teleporthere").setTabCompleter(teleportHereCommand);

        // Canais de Mensagens
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:teleport", new TeleportListener(this));
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:sounds", new SoundListener(this));
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:actionbar", new ActionBarListener(this));
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:vanish", vanishListener);
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:sync", vanishListener);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "proxy:kick");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "proxy:sync");
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
            return true;
        }
        return false;
    }

    private void setupViaVersion() {
        if (getServer().getPluginManager().getPlugin("ViaVersion") != null) {
            this.viaAPI = Via.getAPI();
        }
    }

    // Getters
    public String getServerName() { return serverName; }
    public LuckPerms getLuckPerms() { return luckPerms; }
    public ViaAPI<?> getViaAPI() { return viaAPI; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public TranslationsManager getTranslationsManager() { return translationsManager; }
    public UserProfileReader getUserProfileReader() { return userProfileReader; }
    public UserPreferenceManager getUserPreferenceManager() { return userPreferenceManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public SoundManager getSoundManager() { return soundManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
    public DelayManager getDelayManager() { return delayManager; }
    public ServerConfigManager getServerConfigManager() { return serverConfigManager; }
    public PunishmentReader getPunishmentReader() { return this.punishmentReader; }
    public GodManager getGodManager() { return godManager; }
}