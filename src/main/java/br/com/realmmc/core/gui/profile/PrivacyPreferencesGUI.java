package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PrivacyPreferencesGUI extends BaseProfileMenuGUI {

    public PrivacyPreferencesGUI(Player player) {
        super(player);
    }

    @Override
    public String getTitle() {
        return translations.getMessage("gui.privacy-preferences.title");
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
                translations.getMessage("gui.privacy-preferences.separator-item.name"),
                getLoreFromConfig("gui.privacy-preferences.separator-item.lore")
        );
        for (int i = 9; i <= 17; i++) {
            setItem(i, separatorPane);
        }

        setItem(49, createBackItem());
    }

    private GuiItem createBackItem() {
        String name = translations.getMessage("gui.privacy-preferences.back-item.name");
        List<String> lore = getLoreFromConfig("gui.privacy-preferences.back-item.lore");
        return new GuiItem(createItem(Material.ARROW, name, lore), event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PreferencesGUI(player).open();
        });
    }
}