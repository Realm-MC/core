package br.com.realmmc.core.api;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.gui.GuiManager;
import br.com.realmmc.core.hologram.HologramManager;
import br.com.realmmc.core.managers.*;
import br.com.realmmc.core.modules.ModuleManager;
import br.com.realmmc.core.npc.NPCManager;
import br.com.realmmc.core.player.PlayerManager;
import br.com.realmmc.core.punishments.PunishmentReader;
import br.com.realmmc.core.users.GroupInfoReader;
import br.com.realmmc.core.users.PurchaseHistoryReader;
import br.com.realmmc.core.users.UserProfileReader;
import br.com.realmmc.core.users.UserPreferenceManager;
import br.com.realmmc.core.users.UserPreferenceReader;
import br.com.realmmc.core.utils.PlayerResolver;

/**
 * Ponto de acesso central e estático para os principais managers e serviços do plugin Core.
 * Facilita a integração entre os diferentes módulos da rede.
 */
public class CoreAPI {

    private static CoreAPI instance;
    private final Main plugin;

    public CoreAPI(Main main) {
        instance = this;
        this.plugin = main;
    }

    public static CoreAPI getInstance() {
        return instance;
    }

    public Main getPlugin() { return plugin; }
    public ModuleManager getModuleManager() { return plugin.getModuleManager(); }
    public GuiManager getGuiManager() { return plugin.getGuiManager(); }
    public PlayerManager getPlayerManager() { return plugin.getPlayerManager(); }
    public PlayerDataManager getPlayerDataManager() { return plugin.getPlayerDataManager(); }
    public SoundManager getSoundManager() { return plugin.getSoundManager(); }
    public ActionBarManager getActionBarManager() { return plugin.getActionBarManager(); }
    public TranslationsManager getTranslationsManager() { return plugin.getTranslationsManager(); }
    public DatabaseManager getDatabaseManager() { return plugin.getDatabaseManager(); }
    public UserProfileReader getUserProfileReader() { return plugin.getUserProfileReader(); }
    public UserPreferenceReader getUserPreferenceReader() { return plugin.getUserPreferenceReader(); }
    public UserPreferenceManager getUserPreferenceManager() { return plugin.getUserPreferenceManager(); }
    public CooldownManager getCooldownManager() { return plugin.getCooldownManager(); }
    public DelayManager getDelayManager() { return plugin.getDelayManager(); }
    public SpamManager getSpamManager() { return plugin.getSpamManager(); }
    public ServerConfigManager getServerConfigManager() { return plugin.getServerConfigManager(); }
    public PunishmentReader getPunishmentReader() { return plugin.getPunishmentReader(); }
    public GodManager getGodManager() { return plugin.getGodManager(); }
    public TagManager getTagManager() { return plugin.getTagManager(); }
    public PlayerResolver getPlayerResolver() { return plugin.getPlayerResolver(); }
    public MaintenanceLockdownManager getMaintenanceLockdownManager() { return plugin.getMaintenanceLockdownManager(); }
    public PurchaseHistoryReader getPurchaseHistoryReader() { return plugin.getPurchaseHistoryReader(); }
    public GroupInfoReader getGroupInfoReader() { return plugin.getGroupInfoReader(); }
    public HologramManager getHologramManager() { return plugin.getHologramManager(); }
    public NPCManager getNpcManager() { return plugin.getNpcManager(); }
}