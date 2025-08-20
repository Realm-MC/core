package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.utils.ItemBuilder;
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

        ItemStack separatorPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(translations.getMessage("gui.preferences.separator-item.name"))
                .setLore(getLoreFromConfig("gui.preferences.separator-item.lore"))
                .build();

        for (int i = 9; i <= 17; i++) {
            setItem(i, separatorPane);
        }

        setItem(28, createLobbyPrefsItem());
        setItem(29, createChatPrefsItem());
        setItem(30, createRankupPrefsItem());
        setItem(31, createSurvivalPrefsItem());
        setItem(32, createPrivacyPrefsItem());

        setItem(49, createBackItem());
    }

    private GuiItem createLobbyPrefsItem() {
        String name = translations.getMessage("gui.preferences.lobby-prefs-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.lobby-prefs-item.lore");
        ItemStack item = new ItemBuilder(Material.COMPASS).setName(name).setLore(lore).hideFlags().build();
        return new GuiItem(item, event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new LobbyPreferencesGUI(player).open();
        });
    }

    private GuiItem createChatPrefsItem() {
        String name = translations.getMessage("gui.preferences.chat-prefs-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.chat-prefs-item.lore");
        ItemStack item = new ItemBuilder(Material.WRITABLE_BOOK).setName(name).setLore(lore).hideFlags().build();
        return new GuiItem(item, event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new ChatPreferencesGUI(player).open();
        });
    }

    private GuiItem createRankupPrefsItem() {
        String name = translations.getMessage("gui.preferences.rankup-prefs-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.rankup-prefs-item.lore");
        ItemStack item = new ItemBuilder(Material.DIAMOND_PICKAXE).setName(name).setLore(lore).hideFlags().build();
        return new GuiItem(item, event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new RankupPreferencesGUI(player).open();
        });
    }

    private GuiItem createSurvivalPrefsItem() {
        String name = translations.getMessage("gui.preferences.survival-prefs-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.survival-prefs-item.lore");
        ItemStack item = new ItemBuilder(Material.GRASS_BLOCK).setName(name).setLore(lore).hideFlags().build();
        return new GuiItem(item, event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new SurvivalPreferencesGUI(player).open();
        });
    }

    private GuiItem createPrivacyPrefsItem() {
        String name = translations.getMessage("gui.preferences.privacy-prefs-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.privacy-prefs-item.lore");
        ItemStack item = new ItemBuilder(Material.ENDER_EYE).setName(name).setLore(lore).hideFlags().build();
        return new GuiItem(item, event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PrivacyPreferencesGUI(player).open();
        });
    }

    private GuiItem createBackItem() {
        String name = translations.getMessage("gui.preferences.back-item.name");
        List<String> lore = getLoreFromConfig("gui.preferences.back-item.lore");
        ItemStack item = new ItemBuilder(Material.ARROW).setName(name).setLore(lore).hideFlags().build();
        return new GuiItem(item, event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new ProfileGUI(player).open();
        });
    }
}