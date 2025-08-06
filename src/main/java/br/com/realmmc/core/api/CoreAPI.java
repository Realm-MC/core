package br.com.realmmc.core.api;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.gui.GuiManager;
import br.com.realmmc.core.hologram.HologramManager;
import br.com.realmmc.core.managers.*;
import br.com.realmmc.core.modules.ModuleManager;
import br.com.realmmc.core.player.PlayerManager;
import br.com.realmmc.core.punishments.PunishmentReader;
import br.com.realmmc.core.users.UserProfileReader;
import br.com.realmmc.core.users.UserPreferenceReader;
import br.com.realmmc.core.users.UserPreferenceManager; // 1. ADICIONADO

public class CoreAPI {

    private static CoreAPI instance;

    private final Main plugin;
    private final ModuleManager moduleManager;
    private final GuiManager guiManager;
    private final PlayerManager playerManager;
    private final PlayerDataManager playerDataManager;
    private final SoundManager soundManager;
    private final ActionBarManager actionBarManager;
    private final TranslationsManager translationsManager;
    private final DatabaseManager databaseManager;
    private final UserProfileReader userProfileReader;
    private final UserPreferenceReader userPreferenceReader;
    private final UserPreferenceManager userPreferenceManager; // 2. ADICIONADO
    private final CooldownManager cooldownManager;
    private final DelayManager delayManager;
    private final SpamManager spamManager;
    private final GodManager godManager;
    private final TagManager tagManager;
    private final PunishmentReader punishmentReader;
    private final ServerConfigManager serverConfigManager;
    private final HologramManager hologramManager;

    public CoreAPI(Main main) {
        instance = this;
        this.plugin = main;
        this.moduleManager = main.getModuleManager();
        this.guiManager = main.getGuiManager();
        this.playerManager = main.getPlayerManager();
        this.playerDataManager = main.getPlayerDataManager();
        this.soundManager = main.getSoundManager();
        this.actionBarManager = main.getActionBarManager();
        this.translationsManager = main.getTranslationsManager();
        this.databaseManager = main.getDatabaseManager();
        this.userProfileReader = main.getUserProfileReader();
        this.userPreferenceReader = main.getUserPreferenceReader();
        this.userPreferenceManager = main.getUserPreferenceManager(); // 3. ADICIONADO
        this.cooldownManager = main.getCooldownManager();
        this.delayManager = main.getDelayManager();
        this.spamManager = main.getSpamManager();
        this.godManager = main.getGodManager();
        this.tagManager = main.getTagManager();
        this.punishmentReader = main.getPunishmentReader();
        this.serverConfigManager = main.getServerConfigManager();
        this.hologramManager = main.getHologramManager();
    }

    public static CoreAPI getInstance() {
        return instance;
    }

    public Main getPlugin() { return plugin; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public SoundManager getSoundManager() { return soundManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
    public TranslationsManager getTranslationsManager() { return translationsManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public UserProfileReader getUserProfileReader() { return userProfileReader; }
    public UserPreferenceReader getUserPreferenceReader() { return userPreferenceReader; }
    public UserPreferenceManager getUserPreferenceManager() { return userPreferenceManager; } // 4. ADICIONADO
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public DelayManager getDelayManager() { return delayManager; }
    public SpamManager getSpamManager() { return spamManager; }
    public GodManager getGodManager() { return godManager; }
    public TagManager getTagManager() { return tagManager; }
    public PunishmentReader getPunishmentReader() { return punishmentReader; }
    public ServerConfigManager getServerConfigManager() { return serverConfigManager; }
    public HologramManager getHologramManager() { return hologramManager; }
}