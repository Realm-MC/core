package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.player.RealmPlayer;
import br.com.realmmc.core.users.UserPreferenceReader;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
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
        // NOVO: Busca o estado da preferência de luz
        CompletableFuture<Boolean> lightFuture = prefReader.hasRankupPersonalLight(player.getUniqueId());

        CompletableFuture.allOf(coinsFuture, confirmFuture, alertFuture, lightFuture).thenRun(() -> {
            player.getServer().getScheduler().runTask(CoreAPI.getInstance().getPlugin(), () -> {
                // ALTERADO: Permissão correta para o recurso de Rankup
                boolean hasPermission = player.hasPermission("rankup.champion");
                buildDynamicItems(coinsFuture.join(), confirmFuture.join(), alertFuture.join(), lightFuture.join(), hasPermission);
            });
        });
    }

    // ALTERADO: Adicionado o parâmetro `hasPersonalLight`
    private void buildDynamicItems(boolean canReceiveCoins, boolean hasConfirm, boolean hasAlert, boolean hasPersonalLight, boolean hasPermission) {
        setItem(19, createCoinsReceiptItem(canReceiveCoins, hasPermission));
        setItem(28, createToggleItem(canReceiveCoins, "CoinsReceipt", hasPermission));

        setItem(20, createRankupConfirmItem(hasConfirm, hasPermission));
        setItem(29, createToggleItem(hasConfirm, "RankupConfirmation", hasPermission));

        setItem(21, createRankupAlertItem(hasAlert));
        setItem(30, createRankupAlertToggleItem(hasAlert));

        setItem(22, createPersonalLightItem(hasPersonalLight, hasPermission));
        setItem(31, createToggleItem(hasPersonalLight, "RankupPersonalLight", hasPermission));
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
        return new GuiItem(createItem(Material.GOLD_INGOT, name, lore));
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
        return new GuiItem(createItem(Material.EMERALD, name, lore));
    }

    private GuiItem createRankupAlertItem(boolean isChatMode) {
        String name = translations.getRawMessage("gui.rankup-preferences.rankup-alert-item.name");
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.rankup-alert-item.lore");
        String status = translations.getMessage("gui.rankup-preferences.rankup-alert-item.status_" + (isChatMode ? "chat" : "actionbar"));
        lore.add(translations.getMessage("gui.rankup-preferences.status-line", "status", status));
        return new GuiItem(createItem(Material.BELL, name, lore));
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
        return new GuiItem(createItem(Material.LANTERN, name, lore));
    }

    private GuiItem createToggleItem(boolean isEnabled, String preferenceName, boolean hasPermission) {
        String name = isEnabled ? "&cDesativar" : "&aAtivar";
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.toggle.lore");

        Material material;
        if (preferenceName.equalsIgnoreCase("RankupPersonalLight")) {
            material = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;
        } else {
            material = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;
        }

        return new GuiItem(createItem(material, name, lore), event -> {
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
        return new GuiItem(createItem(material, name, lore), event -> sendTogglePreferenceMessage("RankupAlert"));
    }

    private void sendTogglePreferenceMessage(String preferenceName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UpdatePreference");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(preferenceName);
        player.sendPluginMessage(CoreAPI.getInstance().getPlugin(), "proxy:preferences", out.toByteArray());

        String messageKey = "";

        Optional<RealmPlayer> realmPlayerOpt = CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(player);
        if (realmPlayerOpt.isPresent()) {
            boolean isCurrentlyEnabled;
            switch (preferenceName.toLowerCase()) {
                case "coinsreceipt":
                    isCurrentlyEnabled = realmPlayerOpt.get().canReceiveCoins();
                    messageKey = !isCurrentlyEnabled ? "toggle.coins-receipt.enabled" : "toggle.coins-receipt.disabled";
                    break;
                case "rankupconfirmation":
                    isCurrentlyEnabled = realmPlayerOpt.get().needsRankupConfirmation();
                    messageKey = !isCurrentlyEnabled ? "toggle.rankup-confirm.enabled" : "toggle.rankup-confirm.disabled";
                    break;
                case "rankupalert":
                    isCurrentlyEnabled = realmPlayerOpt.get().prefersChatRankupAlerts();
                    messageKey = !isCurrentlyEnabled ? "toggle.rankup-alert.enabled" : "toggle.rankup-alert.disabled";
                    break;
                // NOVO CASE ADICIONADO
                case "rankuppersonallight":
                    isCurrentlyEnabled = realmPlayerOpt.get().hasPersonalLight();
                    messageKey = !isCurrentlyEnabled ? "toggle.personal-light.enabled" : "toggle.personal-light.disabled";
                    break;
            }
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