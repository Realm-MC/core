package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.player.RealmPlayer;
import br.com.realmmc.core.utils.ItemBuilder;
import br.com.realmmc.core.utils.Permissions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LobbyPreferencesGUI extends BaseProfileMenuGUI {

    public LobbyPreferencesGUI(Player player) {
        super(player);
    }

    @Override
    public String getTitle() {
        return translations.getRawMessage("gui.lobby-preferences.title");
    }

    @Override
    public int getSize() {
        return 6 * 9;
    }

    @Override
    public void setupItems() {
        setupHeader();
        ItemStack separator = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(translations.getMessage("gui.lobby-preferences.separator-item.name"))
                .setLore(getLoreFromConfig("gui.lobby-preferences.separator-item.lore"))
                .build();
        for (int i = 9; i <= 17; i++) {
            setItem(i, separator);
        }
        buildDynamicItems();

        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .setName(translations.getMessage("gui.lobby-preferences.back-item.name"))
                .setLore(getLoreFromConfig("gui.lobby-preferences.back-item.lore"))
                .build();
        setItem(slot(6, 5), backItem, event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PreferencesGUI(player).open();
        });
    }

    private void buildDynamicItems() {
        Optional<RealmPlayer> realmPlayerOpt = CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(player);
        if (realmPlayerOpt.isEmpty()) {
            player.closeInventory();
            CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "general.internal-error");
            return;
        }
        RealmPlayer realmPlayer = realmPlayerOpt.get();

        buildLobbyProtectionItems(realmPlayer);
        buildLobbyFlyItems(realmPlayer);
        buildLobbyTimeItems(realmPlayer);
    }

    private void buildLobbyProtectionItems(RealmPlayer realmPlayer) {
        boolean isEnabled = realmPlayer.hasLobbyProtection();
        boolean hasPermission = player.hasPermission(Permissions.CORE_CHAMPION);

        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.lobby-preferences.protection-item.name");
        List<String> lore = new ArrayList<>(getLoreFromConfig("gui.lobby-preferences.protection-item.lore"));
        lore.add("");
        String status = translations.getRawMessage("gui.lobby-preferences.toggle-names." + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getRawMessage("gui.lobby-preferences.status-line", "status", status));
        if (!hasPermission) {
            lore.add("");
            lore.add(translations.getRawMessage("gui.lobby-preferences.permission-required"));
        }

        setItem(slot(3, 2), new ItemBuilder(Material.IRON_DOOR).setName(name).setLore(lore).hideFlags(ItemFlag.HIDE_ATTRIBUTES).build());

        String toggleName = translations.getRawMessage("gui.lobby-preferences.toggle-names." + (isEnabled ? "deactivate" : "activate"));
        Material toggleMat = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;
        List<String> toggleLore = getLoreFromConfig("gui.lobby-preferences.toggle-lore");

        setItem(slot(4, 2), new ItemBuilder(toggleMat).setName(toggleName).setLore(toggleLore).build(), event -> {
            if (!hasPermission) {
                CoreAPI.getInstance().getSoundManager().playError(player);
                return;
            }
            sendTogglePreferenceMessage("LobbyProtection");
        });
    }

    private void buildLobbyFlyItems(RealmPlayer realmPlayer) {
        boolean isEnabled = realmPlayer.hasLobbyFly();
        boolean hasPermission = player.hasPermission(Permissions.CORE_CHAMPION);

        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.lobby-preferences.fly-item.name");
        List<String> lore = new ArrayList<>(getLoreFromConfig("gui.lobby-preferences.fly-item.lore"));
        lore.add("");
        String status = translations.getRawMessage("gui.lobby-preferences.toggle-names." + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getRawMessage("gui.lobby-preferences.status-line", "status", status));
        if (!hasPermission) {
            lore.add("");
            lore.add(translations.getRawMessage("gui.lobby-preferences.permission-required"));
        }

        setItem(slot(3, 3), new ItemBuilder(Material.FEATHER).setName(name).setLore(lore).build());

        String toggleName = translations.getRawMessage("gui.lobby-preferences.toggle-names." + (isEnabled ? "deactivate" : "activate"));
        Material toggleMat = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;
        List<String> toggleLore = getLoreFromConfig("gui.lobby-preferences.toggle-lore");

        setItem(slot(4, 3), new ItemBuilder(toggleMat).setName(toggleName).setLore(toggleLore).build(), event -> {
            if (!hasPermission) {
                CoreAPI.getInstance().getSoundManager().playError(player);
                return;
            }
            sendTogglePreferenceMessage("LobbyFly");
        });
    }

    private void buildLobbyTimeItems(RealmPlayer realmPlayer) {
        String currentPref = realmPlayer.getLobbyTimePreference();
        String safePref = (currentPref == null) ? "CICLO" : currentPref;

        String statusText;
        Material nextMaterial;
        String nextName;

        switch (safePref.toUpperCase()) {
            case "DIA" -> {
                statusText = translations.getRawMessage("gui.lobby-preferences.time-item.status-day");
                nextMaterial = Material.ORANGE_DYE;
                nextName = translations.getRawMessage("gui.lobby-preferences.time-toggle-item.name-to-afternoon");
            }
            case "TARDE" -> {
                statusText = translations.getRawMessage("gui.lobby-preferences.time-item.status-afternoon");
                nextMaterial = Material.BLUE_DYE;
                nextName = translations.getRawMessage("gui.lobby-preferences.time-toggle-item.name-to-night");
            }
            case "NOITE" -> {
                statusText = translations.getRawMessage("gui.lobby-preferences.time-item.status-night");
                nextMaterial = Material.LIME_DYE;
                nextName = translations.getRawMessage("gui.lobby-preferences.time-toggle-item.name-to-cycle");
            }
            default -> { // CICLO
                statusText = translations.getRawMessage("gui.lobby-preferences.time-item.status-cycle");
                nextMaterial = Material.LIGHT_BLUE_DYE;
                nextName = translations.getRawMessage("gui.lobby-preferences.time-toggle-item.name-to-day");
            }
        }

        List<String> displayLore = new ArrayList<>(getLoreFromConfig("gui.lobby-preferences.time-item.lore"));
        displayLore.add("");
        displayLore.add(translations.getRawMessage("gui.lobby-preferences.time-item.status-prefix") + statusText);

        ItemStack displayItem = new ItemBuilder(Material.CLOCK)
                .setName(translations.getMessage("gui.lobby-preferences.time-item.name"))
                .setLore(displayLore)
                .build();
        setItem(slot(3, 4), displayItem);

        List<String> toggleLore = getLoreFromConfig("gui.lobby-preferences.toggle-lore");
        ItemStack toggleItem = new ItemBuilder(nextMaterial)
                .setName(nextName)
                .setLore(toggleLore)
                .build();
        // <-- MUDANÇA: Corrigido de "tempo" para a chave correta "LobbyTime" -->
        setItem(slot(4, 4), toggleItem, event -> sendTogglePreferenceMessage("LobbyTime"));
    }

    private void sendTogglePreferenceMessage(String preferenceName) {
        CoreAPI.getInstance().getSoundManager().playSuccess(player);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PreferenceUpdate");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(preferenceName);

        // Para booleanos, enviamos um valor dummy, o proxy irá inverter.
        if (!preferenceName.equalsIgnoreCase("LobbyTime")) {
            out.writeBoolean(false);
        }

        player.sendPluginMessage(plugin, "proxy:preference_update", out.toByteArray());
    }
}