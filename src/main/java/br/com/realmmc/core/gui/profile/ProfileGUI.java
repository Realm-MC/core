package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class ProfileGUI extends BaseProfileMenuGUI {

    public ProfileGUI(Player player) {
        super(player);
    }

    @Override
    public String getTitle() {
        return translations.getMessage("gui.profile.title");
    }

    @Override
    public int getSize() {
        return 5 * 9;
    }

    @Override
    public void setupItems() {
        setupHeader();

        ItemStack separatorPane = createItem(
                Material.BLACK_STAINED_GLASS_PANE,
                translations.getMessage("gui.profile.separator-item.name"),
                getLoreFromConfig("gui.profile.separator-item.lore")
        );
        for (int i = 9; i <= 17; i++) {
            setItem(i, separatorPane);
        }

        setItem(29, createPreferencesItem());
        setItem(33, createStatisticsItem());
    }

    private GuiItem createPreferencesItem() {
        String name = translations.getMessage("gui.profile.preferences-item.name");
        List<String> lore = getLoreFromConfig("gui.profile.preferences-item.lore");
        Material material = Material.getMaterial(translations.getConfig().getString("gui.profile.preferences-item.material", "COMPARATOR"));
        return new GuiItem(createItem(material, name, lore), event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PreferencesGUI(player).open();
        });
    }

    private GuiItem createStatisticsItem() {
        String name = translations.getMessage("gui.profile.statistics-item.name");
        List<String> lore = getLoreFromConfig("gui.profile.statistics-item.lore");
        Material material = Material.getMaterial(translations.getConfig().getString("gui.profile.statistics-item.material", "PAPER"));

        return new GuiItem(createItem(material, name, lore), event -> showComingSoon());
    }
}