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

        ItemStack separatorPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(translations.getMessage("gui.lobby-preferences.separator-item.name"))
                .setLore(getLoreFromConfig("gui.lobby-preferences.separator-item.lore"))
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

        setItem(21, createLobbyTimeItem(realmPlayer.getLobbyTimePreference()));
        setItem(30, createLobbyTimeToggleItem());
    }

    private void sendTogglePreferenceMessage(String preferenceName) {
        CoreAPI.getInstance().getSoundManager().playSuccess(player);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UpdatePreference");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(preferenceName);

        player.sendPluginMessage(plugin, "proxy:preference_update", out.toByteArray());
    }

    private GuiItem createLobbyTimeToggleItem() {
        String name = "&eAlternar Tempo";
        List<String> lore = List.of("&7Clique para mudar para a próxima opção.");
        ItemStack item = new ItemBuilder(Material.REDSTONE_TORCH).setName(name).setLore(lore).build();
        return new GuiItem(item, event -> sendTogglePreferenceMessage("tempo"));
    }

    private GuiItem createLobbyTimeItem(String currentPreference) {
        String name = "&aTempo no Lobby";
        String status;
        String safePreference = (currentPreference == null) ? "CICLO" : currentPreference;

        switch (safePreference) {
            case "DIA": status = "&eDia"; break;
            case "TARDE": status = "&6Tarde"; break;
            case "NOITE": status = "&9Noite"; break;
            default: status = "&fCiclo Automático"; break;
        }

        List<String> lore = List.of(
                "&7Escolha um período fixo para o",
                "&7tempo no lobby ou deixe no ciclo",
                "&7padrão de dia e noite.",
                "",
                "&fEstado Atual: " + status
        );

        ItemStack item = new ItemBuilder(Material.CLOCK).setName(name).setLore(lore).build();
        return new GuiItem(item);
    }

    private GuiItem createToggleItem(boolean isEnabled, String preferenceName, boolean hasPermission) {
        String name = isEnabled ? "&cDesativar" : "&aAtivar";
        List<String> lore = getLoreFromConfig("gui.rankup-preferences.toggle.lore");
        Material material = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemBuilder(material).setName(name).setLore(lore).build();
        return new GuiItem(item, event -> {
            if (!hasPermission) {
                CoreAPI.getInstance().getSoundManager().playError(player);
                return;
            }
            sendTogglePreferenceMessage(preferenceName);
        });
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
        ItemStack item = new ItemBuilder(Material.IRON_DOOR).setName(name).setLore(lore).hideFlags().build();
        return new GuiItem(item);
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
        ItemStack item = new ItemBuilder(Material.FEATHER).setName(name).setLore(lore).hideFlags().build();
        return new GuiItem(item);
    }

    private GuiItem createBackItem() {
        String name = translations.getMessage("gui.lobby-preferences.back-item.name");
        List<String> lore = getLoreFromConfig("gui.lobby-preferences.back-item.lore");
        ItemStack item = new ItemBuilder(Material.ARROW).setName(name).setLore(lore).build();
        return new GuiItem(item, event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PreferencesGUI(player).open();
        });
    }
}