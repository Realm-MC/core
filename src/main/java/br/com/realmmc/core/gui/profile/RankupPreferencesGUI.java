package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.player.RealmPlayer;
import br.com.realmmc.core.utils.ItemBuilder;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class RankupPreferencesGUI extends BaseProfileMenuGUI {

    public RankupPreferencesGUI(Player player) {
        super(player);
    }

    @Override
    public String getTitle() {
        return translations.getMessage("gui.rankup-preferences.title");
    }

    @Override
    public int getSize() {
        return 6 * 9;
    }

    @Override
    public void setupItems() {
        setupHeader();

        ItemStack separatorPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(translations.getMessage("gui.rankup-preferences.separator-item.name"))
                .setLore(getLoreFromConfig("gui.rankup-preferences.separator-item.lore"))
                .build();

        for (int i = 9; i <= 17; i++) {
            setItem(i, separatorPane);
        }
        buildDynamicItems();
        setItem(49, createBackItem());
    }

    private void buildDynamicItems() {
        Optional<RealmPlayer> realmPlayerOpt = CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(player);
        if (realmPlayerOpt.isEmpty()) {
            player.closeInventory();
            player.sendMessage("§cOcorreu um erro ao carregar suas preferências.");
            return;
        }

        RealmPlayer realmPlayer = realmPlayerOpt.get();
        boolean hasPermission = player.hasPermission("rankup.champion");

        setItem(19, createCoinsReceiptItem(true, hasPermission)); // Este toggle não está implementado no proxy, assumindo true
        setItem(28, createToggleItem(true, "CoinsReceipt", hasPermission));

        setItem(20, createRankupConfirmItem(realmPlayer.needsRankupConfirmation(), hasPermission));
        setItem(29, createToggleItem(realmPlayer.needsRankupConfirmation(), "RankupConfirmation", hasPermission));

        setItem(21, createRankupAlertItem(realmPlayer.prefersChatRankupAlerts()));
        setItem(30, createRankupAlertToggleItem(realmPlayer.prefersChatRankupAlerts()));

        setItem(22, createPersonalLightItem(realmPlayer.hasPersonalLight(), hasPermission));
        setItem(31, createToggleItem(realmPlayer.hasPersonalLight(), "RankupPersonalLight", hasPermission));
    }

    private GuiItem createCoinsReceiptItem(boolean isEnabled, boolean hasPermission) {
        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.rankup-preferences.coins-receipt-item.name");
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.coins-receipt-item.lore");
        String status = translations.getMessage("gui.rankup-preferences.toggle.name_" + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getMessage("gui.rankup-preferences.status-line", "status", status));
        if (!hasPermission) {
            lore.add("");
            lore.add(translations.getMessage("gui.preferences.toggle.permission-required"));
        }

        ItemStack item = new ItemBuilder(Material.GOLD_INGOT)
                .setName(name)
                .setLore(lore)
                .hideFlags()
                .build();

        return new GuiItem(item);
    }

    private GuiItem createRankupConfirmItem(boolean isEnabled, boolean hasPermission) {
        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.rankup-preferences.rankup-confirm-item.name");
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.rankup-confirm-item.lore");
        String status = translations.getMessage("gui.rankup-preferences.toggle.name_" + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getMessage("gui.rankup-preferences.status-line", "status", status));
        if (!hasPermission) {
            lore.add("");
            lore.add(translations.getMessage("gui.preferences.toggle.permission-required"));
        }

        ItemStack item = new ItemBuilder(Material.EMERALD)
                .setName(name)
                .setLore(lore)
                .hideFlags()
                .build();

        return new GuiItem(item);
    }

    private GuiItem createRankupAlertItem(boolean isChatMode) {
        String name = translations.getRawMessage("gui.rankup-preferences.rankup-alert-item.name");
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.rankup-alert-item.lore");
        String status = translations.getMessage("gui.rankup-preferences.rankup-alert-item.status_" + (isChatMode ? "chat" : "actionbar"));
        lore.add(translations.getMessage("gui.rankup-preferences.status-line", "status", status));

        ItemStack item = new ItemBuilder(Material.BELL)
                .setName(name)
                .setLore(lore)
                .hideFlags()
                .build();

        return new GuiItem(item);
    }

    private GuiItem createPersonalLightItem(boolean isEnabled, boolean hasPermission) {
        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.rankup-preferences.personal-light-item.name");
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.personal-light-item.lore");
        String status = translations.getMessage("gui.rankup-preferences.toggle.name_" + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getMessage("gui.rankup-preferences.personal-light-item.status-line", "status", status));
        if (!hasPermission) {
            lore.add("");
            lore.add(translations.getMessage("gui.preferences.toggle.permission-required"));
        }

        ItemStack item = new ItemBuilder(Material.GLOWSTONE_DUST)
                .setName(name)
                .setLore(lore)
                .hideFlags()
                .build();

        return new GuiItem(item);
    }

    private GuiItem createToggleItem(boolean isEnabled, String preferenceName, boolean hasPermission) {
        String name = isEnabled ? "&cDesativar" : "&aAtivar";
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.toggle.lore");
        Material material = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;

        ItemStack item = new ItemBuilder(material)
                .setName(name)
                .setLore(lore)
                .build();

        return new GuiItem(item, event -> {
            if (!hasPermission) {
                CoreAPI.getInstance().getSoundManager().playError(player);
                return;
            }
            sendTogglePreferenceMessage(preferenceName);
        });
    }

    private GuiItem createRankupAlertToggleItem(boolean isChatMode) {
        String name = isChatMode ? "&eMudar para Actionbar" : "&eMudar para Chat";
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.toggle.lore");
        Material material = isChatMode ? Material.LIGHT_BLUE_DYE : Material.PINK_DYE;

        ItemStack item = new ItemBuilder(material)
                .setName(name)
                .setLore(lore)
                .build();

        return new GuiItem(item, event -> sendTogglePreferenceMessage("RankupAlert"));
    }

    private void sendTogglePreferenceMessage(String preferenceName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UpdatePreference");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(preferenceName);
        player.sendPluginMessage(plugin, "proxy:preferences", out.toByteArray());

        CoreAPI.getInstance().getSoundManager().playSuccess(player);
        player.getServer().getScheduler().runTaskLater(plugin, this::buildDynamicItems, 10L);
    }

    private GuiItem createBackItem() {
        String name = translations.getMessage("gui.rankup-preferences.back-item.name");
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.back-item.lore");

        ItemStack item = new ItemBuilder(Material.ARROW)
                .setName(name)
                .setLore(lore)
                .hideFlags()
                .build();

        return new GuiItem(item, event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PreferencesGUI(player).open();
        });
    }
}