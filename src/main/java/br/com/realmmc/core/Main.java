package br.com.realmmc.core;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.commands.*;
import br.com.realmmc.core.gui.GuiManager;
import br.com.realmmc.core.hologram.HologramManager;
import br.com.realmmc.core.listeners.*;
import br.com.realmmc.core.managers.*;
import br.com.realmmc.core.modules.ModuleManager;
import br.com.realmmc.core.modules.SystemType;
import br.com.realmmc.core.npc.NPCManager;
import br.com.realmmc.core.player.PlayerManager;
import br.com.realmmc.core.punishments.PunishmentReader;
import br.com.realmmc.core.scoreboard.DefaultScoreboardManager;
import br.com.realmmc.core.users.*;
import br.com.realmmc.core.utils.PlayerResolver;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import net.luckperms.api.LuckPerms;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public final class Main extends JavaPlugin {

    private LuckPerms luckPerms;
    private ViaAPI<?> viaAPI;
    private SkinsRestorer skinsRestorerApi;
    private DatabaseManager databaseManager;
    private TranslationsManager translationsManager;
    private UserProfileReader userProfileReader;
    private UserPreferenceReader userPreferenceReader;
    private UserPreferenceManager userPreferenceManager;
    private PlayerManager playerManager;
    private PlayerDataManager playerDataManager;
    private SoundManager soundManager;
    private ActionBarManager actionBarManager;
    private CooldownManager cooldownManager;
    private DelayManager delayManager;
    private SpamManager spamManager;
    private ServerConfigManager serverConfigManager;
    private PunishmentReader punishmentReader;
    private GodManager godManager;
    private TagManager tagManager;
    private GuiManager guiManager;
    private ModuleManager moduleManager;
    private HologramManager hologramManager;
    private PlayerResolver playerResolver;
    private MaintenanceLockdownManager maintenanceLockdownManager;
    private String serverName;
    private NPCManager npcManager;
    private PurchaseHistoryReader purchaseHistoryReader;
    private GroupInfoReader groupInfoReader;

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
        this.soundManager = new SoundManager();

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
        setupSkinsRestorer();

        this.serverConfigManager = new ServerConfigManager(this);
        this.userProfileReader = new UserProfileReader(this);
        this.userPreferenceReader = new UserPreferenceReader(this);
        this.userPreferenceManager = new UserPreferenceManager(this);
        this.playerManager = new PlayerManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.actionBarManager = new ActionBarManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.delayManager = new DelayManager();
        this.spamManager = new SpamManager(this.delayManager, this.translationsManager);
        this.punishmentReader = new PunishmentReader(this);
        this.godManager = new GodManager();
        this.tagManager = new TagManager(this);
        this.guiManager = new GuiManager(this);
        this.moduleManager = new ModuleManager(this);
        this.hologramManager = new HologramManager(this);
        this.playerResolver = new PlayerResolver();
        this.maintenanceLockdownManager = new MaintenanceLockdownManager(this);
        this.groupInfoReader = new GroupInfoReader(this);
        this.purchaseHistoryReader = new PurchaseHistoryReader(this);

        if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            this.npcManager = new NPCManager(this);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                getLogger().info("Carregando NPCs...");
                npcManager.loadAndSpawnAll();
            }, 20L);
        }

        new CoreAPI(this);

        registerComponents();
        activateDefaultModules();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("Carregando hologramas...");
            hologramManager.loadHolograms();
        }, 1L);

        this.translationsManager.log(Level.INFO, "logs.plugin.enabled");
    }

    // O resto da classe Main.java permanece o mesmo...
    @Override
    public void onDisable() {
        if (this.maintenanceLockdownManager != null) {
            this.maintenanceLockdownManager.stopActionBarTask(true);
            this.maintenanceLockdownManager.stopActionBarTask(false);
        }
        if (this.hologramManager != null) {
            this.hologramManager.despawnAll();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }

        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);

        this.translationsManager.log(Level.INFO, "logs.plugin.disabled");
    }

    private void activateDefaultModules() {
        getServer().getScheduler().runTaskLater(this, () -> {
            getLogger().info("Verificando módulos padrão do Core para ativação...");

            if (!moduleManager.isClaimed(SystemType.SCOREBOARD)) {
                getLogger().info("Nenhum plugin de Scoreboard customizado detectado. Ativando scoreboard padrão do Core.");
                new DefaultScoreboardManager(this).start();
            }
            if (!moduleManager.isClaimed(SystemType.CHAT)) {
                getLogger().info("Nenhum plugin de Chat customizado detectado. Ativando listener de chat padrão do Core.");
                getServer().getPluginManager().registerEvents(new DefaultChatListener(), this);
            }
            if (!moduleManager.isClaimed(SystemType.TAGS)) {
                getLogger().info("Nenhum plugin de Tags customizado detectado. Ativando TagManager padrão do Core.");
                tagManager.start();
            }
        }, 20L);
    }

    private void registerComponents() {
        PluginManager pm = getServer().getPluginManager();

        VanishListener vanishListener = new VanishListener(this);
        MaintenanceLockdownListener maintenanceListener = new MaintenanceLockdownListener(this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(vanishListener, this);
        pm.registerEvents(this.godManager, this);
        pm.registerEvents(maintenanceListener, this);
        pm.registerEvents(new ServerLifecycleListener(this), this);

        Objects.requireNonNull(getCommand("god")).setExecutor(new GodCommand(this.getGodManager()));
        GamemodeCommand gamemodeCommand = new GamemodeCommand();
        PluginCommand gamemode = Objects.requireNonNull(getCommand("gamemode"));
        gamemode.setExecutor(gamemodeCommand);
        gamemode.setTabCompleter(gamemodeCommand);
        HealCommand healCommand = new HealCommand();
        Objects.requireNonNull(getCommand("heal")).setExecutor(healCommand);
        Objects.requireNonNull(getCommand("heal")).setTabCompleter(healCommand);
        FeedCommand feedCommand = new FeedCommand();
        Objects.requireNonNull(getCommand("feed")).setExecutor(feedCommand);
        Objects.requireNonNull(getCommand("feed")).setTabCompleter(feedCommand);
        TeleportCommand teleportCommand = new TeleportCommand();
        Objects.requireNonNull(getCommand("tp")).setExecutor(teleportCommand);
        Objects.requireNonNull(getCommand("tp")).setTabCompleter(teleportCommand);
        TeleportHereCommand teleportHereCommand = new TeleportHereCommand();
        Objects.requireNonNull(getCommand("tphere")).setExecutor(teleportHereCommand);
        Objects.requireNonNull(getCommand("tphere")).setTabCompleter(teleportHereCommand);
        Objects.requireNonNull(getCommand("perfil")).setExecutor(new ProfileCommand());

        NpcCommand npcExecutor = new NpcCommand();
        PluginCommand npcCommand = Objects.requireNonNull(getCommand("npcs"));
        npcCommand.setExecutor(npcExecutor);
        npcCommand.setTabCompleter(npcExecutor);

        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:teleport", new TeleportListener(this));
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:sounds", new SoundListener(this));
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:actionbar", new ActionBarListener(this));
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:vanish", vanishListener);
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:sync", vanishListener);
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:preference_update", new PreferenceUpdateListener());
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:opengui", new OpenGuiListener(this));
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:broadcast", new TitleBroadcastListener(this));
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:maintenance", maintenanceListener);
        getServer().getMessenger().registerIncomingPluginChannel(this, "proxy:economy_update", new EconomyUpdateListener());

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "proxy:kick");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "proxy:sync");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "proxy:preferences");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "core:preference_applied");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "core:punishment_notify");
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

    private void setupSkinsRestorer() {
        if (getServer().getPluginManager().getPlugin("SkinsRestorer") == null) {
            getLogger().warning("SkinsRestorer não encontrado. Funcionalidades de skin estarão desativadas.");
            return;
        }
        try {
            this.skinsRestorerApi = SkinsRestorerProvider.get();
            getLogger().info("API do SkinsRestorer conectada com sucesso.");
        } catch (IllegalStateException e) {
            getLogger().severe("FALHA ao conectar com a API do SkinsRestorer!");
            getLogger().severe("MOTIVO: " + e.getMessage());
            getLogger().severe("Verifique a configuração do SkinsRestorer (ProxyMode, Database). As funções de skin estarão desativadas.");
            this.skinsRestorerApi = null;
        }
    }

    public String getServerName() { return serverName; }
    public LuckPerms getLuckPerms() { return luckPerms; }
    public ViaAPI<?> getViaAPI() { return viaAPI; }
    public SkinsRestorer getSkinsRestorerApi() { return skinsRestorerApi; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public TranslationsManager getTranslationsManager() { return translationsManager; }
    public UserProfileReader getUserProfileReader() { return userProfileReader; }
    public UserPreferenceReader getUserPreferenceReader() { return userPreferenceReader; }
    public UserPreferenceManager getUserPreferenceManager() { return userPreferenceManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public SoundManager getSoundManager() { return soundManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public DelayManager getDelayManager() { return delayManager; }
    public SpamManager getSpamManager() { return spamManager; }
    public ServerConfigManager getServerConfigManager() { return serverConfigManager; }
    public PunishmentReader getPunishmentReader() { return this.punishmentReader; }
    public GodManager getGodManager() { return godManager; }
    public TagManager getTagManager() { return tagManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public PlayerResolver getPlayerResolver() { return playerResolver; }
    public MaintenanceLockdownManager getMaintenanceLockdownManager() { return maintenanceLockdownManager; }
    public NPCManager getNpcManager() { return npcManager; }
    public PurchaseHistoryReader getPurchaseHistoryReader() { return purchaseHistoryReader; }
    public GroupInfoReader getGroupInfoReader() { return groupInfoReader; }
}