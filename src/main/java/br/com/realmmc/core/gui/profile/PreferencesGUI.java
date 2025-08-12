package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class PreferencesGUI extends BaseProfileMenuGUI {

    public PreferencesGUI(Player player) {
        super(player);
    }

    @Override
    public String getTitle() {
        return translations.getMessage("gui.preferences.title");
    }

    @Override
    public int getSize() {
        return 6 * 9;
    }

    @Override
    public void setupItems() {
        setupHeader();

        ItemStack separatorPane = createItem(
                Material.BLACK_STAINED_GLASS_PANE,
                translations.getMessage("gui.preferences.separator-item.name"),
                getLoreFromConfig("gui.preferences.separator-item.lore")
        );
        for (int i = 9; i <= 17; i++) {
            setItem(i, separatorPane);
        }

        setItem(29, createLobbyPrefsItem());
        setItem(30, createChatPrefsItem());
        setItem(31, createSurvivalPrefsItem());
        setItem(32, createRankupPrefsItem());
        setItem(33, createPrivacyPrefsItem());

        setItem(49, createBackItem());
    }

    private GuiItem createLobbyPrefsItem() {
        String name = translations.getMessage("gui.preferences.lobby-prefs-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.lobby-prefs-item.lore");
        return new GuiItem(createItem(Material.NETHER_STAR, name, lore), event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new LobbyPreferencesGUI(player).open();
        });
    }

    private GuiItem createChatPrefsItem() {
        String name = translations.getMessage("gui.preferences.chat-prefs-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.chat-prefs-item.lore");
        return new GuiItem(createItem(Material.PAPER, name, lore), event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new ChatPreferencesGUI(player).open();
        });
    }

    private GuiItem createSurvivalPrefsItem() {
        String name = translations.getMessage("gui.preferences.survival-prefs-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.survival-prefs-item.lore");
        return new GuiItem(createItem(Material.GRASS_BLOCK, name, lore), event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new SurvivalPreferencesGUI(player).open();
        });
    }

    private GuiItem createRankupPrefsItem() {
        String name = translations.getMessage("gui.preferences.rankup-prefs-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.rankup-prefs-item.lore");
        return new GuiItem(createItem(Material.DIAMOND_PICKAXE, name, lore), event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new RankupPreferencesGUI(player).open();
        });
    }

    private GuiItem createPrivacyPrefsItem() {
        String name = translations.getMessage("gui.preferences.privacy-prefs-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.privacy-prefs-item.lore");
        return new GuiItem(createItem(Material.GOLD_NUGGET, name, lore), event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PrivacyPreferencesGUI(player).open();
        });
    }

    private GuiItem createBackItem() {
        String name = translations.getMessage("gui.preferences.back-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.back-item.lore");
        return new GuiItem(createItem(Material.ARROW, name, lore), event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new ProfileGUI(player).open();
        });
    }
}