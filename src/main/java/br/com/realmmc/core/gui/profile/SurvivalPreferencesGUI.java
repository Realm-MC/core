package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SurvivalPreferencesGUI extends BaseProfileMenuGUI {

    public SurvivalPreferencesGUI(Player player) {
        super(player);
    }

    @Override
    public String getTitle() {
        return translations.getMessage("gui.survival-preferences.title");
    }

    @Override
    public int getSize() {
        return 6 * 9;
    }

    @Override
    public void setupItems() {
        setupHeader();

        ItemStack separatorPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int i = 9; i <= 17; i++) {
            setItem(i, separatorPane);
        }

        setItem(49, createBackItem());
    }

    private GuiItem createBackItem() {
        String name = translations.getMessage("gui.survival-preferences.back-item.name");
        List<String> lore = getLoreFromConfig("gui.survival-preferences.back-item.lore");

        ItemStack item = new ItemBuilder(Material.ARROW)
                .setName(name)
                .setLore(lore)
                .build();

        return new GuiItem(item, event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PreferencesGUI(player).open();
        });
    }
}