package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.users.UserPreferenceManager;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LobbyPreferencesGUI extends BaseProfileMenuGUI {

    public LobbyPreferencesGUI(Player player) {
        super(player);
    }

    @Override
    public String getTitle() {
        return translations.getMessage("gui.lobby-preferences.title");
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
                translations.getMessage("gui.lobby-preferences.separator-item.name"),
                getLoreFromConfig("gui.lobby-preferences.separator-item.lore")
        );
        for (int i = 9; i <= 17; i++) {
            setItem(i, separatorPane);
        }
        fetchAndBuild();
        setItem(49, createBackItem());
    }

    private void fetchAndBuild() {

        CoreAPI.getInstance().getUserPreferenceManager().hasLobbyProtection(player.getUniqueId()).thenAccept(isEnabled -> {
            player.getServer().getScheduler().runTask(CoreAPI.getInstance().getPlugin(), () -> {
                buildDynamicItems(isEnabled);
            });
        });
    }

    private void buildDynamicItems(boolean lobbyProtectionEnabled) {
        setItem(19, createLobbyProtectionItem(lobbyProtectionEnabled));
        setItem(28, createLobbyProtectionToggleItem(lobbyProtectionEnabled));
    }

    private GuiItem createLobbyProtectionItem(boolean isEnabled) {
        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.lobby-preferences.lobby-protection-item.name");
        List<String> lore = getLoreFromConfig("gui.lobby-preferences.lobby-protection-item.lore");
        String status = translations.getMessage("gui.lobby-preferences.toggle.name_" + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getMessage("gui.lobby-preferences.lobby-protection-item.status-line", "status", status));
        Material material = Material.IRON_DOOR;
        return new GuiItem(createItem(material, name, lore));
    }

    private GuiItem createLobbyProtectionToggleItem(boolean isEnabled) {
        String name = isEnabled ? "&cDesativar" : "&aAtivar";
        List<String> lore = getLoreFromConfig("gui.lobby-preferences.toggle.lore");
        Material material = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;

        return new GuiItem(createItem(material, name, lore), event -> {
            sendTogglePreferenceMessage("LobbyProtection", !isEnabled);
        });
    }

    private void sendTogglePreferenceMessage(String preferenceName, boolean newState) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UpdatePreference");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(preferenceName);
        player.sendPluginMessage(CoreAPI.getInstance().getPlugin(), "proxy:preferences", out.toByteArray());

        String messageKey = newState ? "toggle.lobby-protection.enabled" : "toggle.lobby-protection.disabled";
        translations.sendMessage(player, messageKey);
        CoreAPI.getInstance().getSoundManager().playSuccess(player);

        player.getServer().getScheduler().runTaskLater(CoreAPI.getInstance().getPlugin(), this::fetchAndBuild, 10L);
    }

    private GuiItem createBackItem() {
        String name = translations.getMessage("gui.lobby-preferences.back-item.name");
        List<String> lore = getLoreFromConfig("gui.lobby-preferences.back-item.lore");
        return new GuiItem(createItem(Material.ARROW, name, lore), event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PreferencesGUI(player).open();
        });
    }
}