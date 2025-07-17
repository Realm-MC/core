package br.com.realmmc.core.api;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.managers.ActionBarManager;
import br.com.realmmc.core.managers.DatabaseManager;
import br.com.realmmc.core.managers.DelayManager;
import br.com.realmmc.core.managers.SoundManager;
import br.com.realmmc.core.managers.TagManager;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.managers.GodManager;
import br.com.realmmc.core.player.PlayerManager;
import br.com.realmmc.core.users.UserProfileReader;
import br.com.realmmc.core.users.UserPreferenceManager;

public class CoreAPI {

    private static CoreAPI instance;
    private final Main plugin;

    // --- CAMPOS DOS MANAGERS ---
    private final PlayerManager playerManager;
    private final SoundManager soundManager;
    private final ActionBarManager actionBarManager;
    private final TranslationsManager translationsManager;
    private final DatabaseManager databaseManager;
    private final UserProfileReader userProfileReader;
    private final DelayManager delayManager;
    private final GodManager godManager;
    private final TagManager tagManager;
    private final UserPreferenceManager userPreferenceManager;

    public CoreAPI(Main main) {
        instance = this;
        this.plugin = main;

        // --- INICIALIZAÇÃO DE TODOS OS MANAGERS ---
        this.playerManager = main.getPlayerManager();
        this.soundManager = main.getSoundManager();
        this.actionBarManager = main.getActionBarManager();
        this.translationsManager = main.getTranslationsManager();
        this.databaseManager = main.getDatabaseManager();
        this.userProfileReader = main.getUserProfileReader();
        this.delayManager = main.getDelayManager();
        this.godManager = main.getGodManager();
        this.tagManager = main.getTagManager();
        this.userPreferenceManager = main.getUserPreferenceManager();
    }

    public static CoreAPI getInstance() {
        return instance;
    }

    public Main getPlugin() {
        return plugin;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    public TranslationsManager getTranslationsManager() {
        return translationsManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public UserProfileReader getUserProfileReader() {
        return userProfileReader;
    }

    public DelayManager getDelayManager() {
        return delayManager;
    }

    // --- GETTERS FALTANTES ADICIONADOS ---

    public GodManager getGodManager() {
        return godManager;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public UserPreferenceManager getUserPreferenceManager() {
        return userPreferenceManager;
    }
}