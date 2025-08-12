package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.users.UserPreferenceReader;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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

        ItemStack separatorPane = createItem(
                Material.BLACK_STAINED_GLASS_PANE,
                translations.getMessage("gui.rankup-preferences.separator-item.name"),
                getLoreFromConfig("gui.rankup-preferences.separator-item.lore")
        );
        for (int i = 9; i <= 17; i++) {
            setItem(i, separatorPane);
        }

        fetchAndBuild();

        setItem(49, createBackItem());
    }

    private void fetchAndBuild() {
        UserPreferenceReader prefReader = CoreAPI.getInstance().getUserPreferenceReader();

        CompletableFuture<Boolean> coinsFuture = prefReader.canReceiveCoins(player.getUniqueId());
        CompletableFuture<Boolean> confirmFuture = prefReader.hasRankupConfirmation(player.getUniqueId());
        CompletableFuture<Boolean> alertFuture = prefReader.hasRankupAlert(player.getUniqueId());

        CompletableFuture.allOf(coinsFuture, confirmFuture, alertFuture).thenRun(() -> {
            player.getServer().getScheduler().runTask(CoreAPI.getInstance().getPlugin(), () -> {
                buildDynamicItems(coinsFuture.join(), confirmFuture.join(), alertFuture.join());
            });
        });
    }

    private void buildDynamicItems(boolean canReceiveCoins, boolean hasConfirm, boolean hasAlert) {
        setItem(19, createCoinsReceiptItem(canReceiveCoins));
        setItem(28, createToggleItem(canReceiveCoins, "CoinsReceipt"));

        setItem(20, createRankupConfirmItem(hasConfirm));
        setItem(29, createToggleItem(hasConfirm, "RankupConfirmation"));

        setItem(21, createRankupAlertItem(hasAlert));
        setItem(30, createRankupAlertToggleItem(hasAlert));
    }

    private GuiItem createCoinsReceiptItem(boolean isEnabled) {
        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.rankup-preferences.coins-receipt-item.name");
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.coins-receipt-item.lore");
        String status = translations.getMessage("gui.rankup-preferences.toggle.name_" + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getMessage("gui.rankup-preferences.status-line", "status", status));
        return new GuiItem(createItem(Material.GOLD_INGOT, name, lore));
    }

    private GuiItem createRankupConfirmItem(boolean isEnabled) {
        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.rankup-preferences.rankup-confirm-item.name");
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.rankup-confirm-item.lore");
        String status = translations.getMessage("gui.rankup-preferences.toggle.name_" + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getMessage("gui.rankup-preferences.status-line", "status", status));
        return new GuiItem(createItem(Material.EMERALD, name, lore));
    }

    private GuiItem createRankupAlertItem(boolean isChatMode) {
        String name = translations.getRawMessage("gui.rankup-preferences.rankup-alert-item.name");
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.rankup-alert-item.lore");
        String status = translations.getMessage("gui.rankup-preferences.rankup-alert-item.status_" + (isChatMode ? "chat" : "actionbar"));
        lore.add(translations.getMessage("gui.rankup-preferences.status-line", "status", status));
        return new GuiItem(createItem(Material.BELL, name, lore));
    }

    private GuiItem createToggleItem(boolean isEnabled, String preferenceName) {
        String name = isEnabled ? "&cDesativar" : "&aAtivar";
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.toggle.lore");
        Material material = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;
        return new GuiItem(createItem(material, name, lore), event -> sendTogglePreferenceMessage(preferenceName, !isEnabled));
    }

    private GuiItem createRankupAlertToggleItem(boolean isChatMode) {
        String name = isChatMode ? "&eMudar para Actionbar" : "&eMudar para Chat";
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.toggle.lore");
        Material material = isChatMode ? Material.LIGHT_BLUE_DYE : Material.PINK_DYE;
        return new GuiItem(createItem(material, name, lore), event -> sendTogglePreferenceMessage("RankupAlert", !isChatMode));
    }

    private void sendTogglePreferenceMessage(String preferenceName, boolean newState) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UpdatePreference");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(preferenceName);
        player.sendPluginMessage(CoreAPI.getInstance().getPlugin(), "proxy:preferences", out.toByteArray());

        String messageKey = "";
        switch (preferenceName.toLowerCase()) {
            case "coinsreceipt" -> messageKey = newState ? "toggle.coins-receipt.enabled" : "toggle.coins-receipt.disabled";
            case "rankupconfirmation" -> messageKey = newState ? "toggle.rankup-confirm.enabled" : "toggle.rankup-confirm.disabled";
            case "rankupalert" -> messageKey = newState ? "toggle.rankup-alert.enabled" : "toggle.rankup-alert.disabled"; // 'newState' aqui será true para Chat, false para Actionbar
        }

        if (!messageKey.isEmpty()) {
            translations.sendMessage(player, messageKey);
        }
        CoreAPI.getInstance().getSoundManager().playSuccess(player);

        player.getServer().getScheduler().runTaskLater(CoreAPI.getInstance().getPlugin(), this::fetchAndBuild, 10L);
    }

    private GuiItem createBackItem() {
        String name = translations.getMessage("gui.rankup-preferences.back-item.name");
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.back-item.lore");
        return new GuiItem(createItem(Material.ARROW, name, lore), event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PreferencesGUI(player).open();
        });
    }
}