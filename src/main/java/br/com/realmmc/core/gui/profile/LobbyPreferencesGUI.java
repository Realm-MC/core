package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.player.RealmPlayer;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

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
        boolean hasPermission = player.hasPermission("core.champion");

        setItem(19, createLobbyProtectionItem(realmPlayer.hasLobbyProtection(), hasPermission));
        setItem(28, createToggleItem(realmPlayer.hasLobbyProtection(), "LobbyProtection", hasPermission));

        if (hasPermission) {
            setItem(20, createLobbyFlyItem(realmPlayer.hasLobbyFly(), true));
            setItem(29, createToggleItem(realmPlayer.hasLobbyFly(), "LobbyFly", true));
        }
    }

    private GuiItem createLobbyProtectionItem(boolean isEnabled, boolean hasPermission) {
        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.lobby-preferences.lobby-protection-item.name");
        List<String> lore = getLoreFromConfig("gui.lobby-preferences.lobby-protection-item.lore");
        String status = translations.getMessage("gui.rankup-preferences.toggle.name_" + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getMessage("gui.lobby-preferences.lobby-protection-item.status-line", "status", status));
        if (!hasPermission) {
            lore.add("");
            lore.add(translations.getMessage("gui.preferences.toggle.permission-required"));
        }
        return new GuiItem(createItem(Material.IRON_DOOR, name, lore));
    }

    private GuiItem createLobbyFlyItem(boolean isEnabled, boolean hasPermission) {
        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.lobby-preferences.fly-lobby-item.name");
        List<String> lore = getLoreFromConfig("gui.lobby-preferences.fly-lobby-item.lore");
        String status = translations.getMessage("gui.rankup-preferences.toggle.name_" + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getMessage("gui.lobby-preferences.fly-lobby-item.status-line", "status", status));
        if (!hasPermission) {
            lore.add("");
            lore.add(translations.getMessage("gui.preferences.toggle.permission-required"));
        }
        return new GuiItem(createItem(Material.FEATHER, name, lore));
    }

    private GuiItem createToggleItem(boolean isEnabled, String preferenceName, boolean hasPermission) {
        String name = isEnabled ? "&cDesativar" : "&aAtivar";
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.toggle.lore");
        Material material = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;

        return new GuiItem(createItem(material, name, lore), event -> {
            if (!hasPermission) {
                CoreAPI.getInstance().getSoundManager().playError(player);
                return;
            }
            sendTogglePreferenceMessage(preferenceName);
        });
    }

    private void sendTogglePreferenceMessage(String preferenceName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UpdatePreference");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(preferenceName);
        player.sendPluginMessage(CoreAPI.getInstance().getPlugin(), "proxy:preferences", out.toByteArray());

        Optional<RealmPlayer> realmPlayerOpt = CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(player);
        if (realmPlayerOpt.isPresent()) {
            boolean isCurrentlyEnabled;
            String messageKey = "";
            switch (preferenceName.toLowerCase()) {
                case "lobbyprotection":
                    isCurrentlyEnabled = realmPlayerOpt.get().hasLobbyProtection();
                    messageKey = !isCurrentlyEnabled ? "toggle.lobby-protection.enabled" : "toggle.lobby-protection.disabled";
                    break;
                case "lobbyfly":
                    isCurrentlyEnabled = realmPlayerOpt.get().hasLobbyFly();
                    messageKey = !isCurrentlyEnabled ? "toggle.fly-lobby.enabled" : "toggle.fly-lobby.disabled";
                    break;
            }
            if (!messageKey.isEmpty()) {
                translations.sendMessage(player, messageKey);
            }
        }

        CoreAPI.getInstance().getSoundManager().playSuccess(player);
        player.getServer().getScheduler().runTaskLater(CoreAPI.getInstance().getPlugin(), this::buildDynamicItems, 10L);
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