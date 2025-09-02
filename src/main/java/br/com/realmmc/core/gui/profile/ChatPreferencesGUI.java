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

public class ChatPreferencesGUI extends BaseProfileMenuGUI {

    public ChatPreferencesGUI(Player player) {
        super(player);
    }

    @Override
    public String getTitle() {
        return translations.getMessage("gui.chat-preferences.title");
    }

    @Override
    public int getSize() {
        return 6 * 9;
    }

    @Override
    public void setupItems() {
        setupHeader();

        ItemStack separatorPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(translations.getMessage("gui.chat-preferences.separator-item.name"))
                .setLore(getLoreFromConfig("gui.chat-preferences.separator-item.lore"))
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
        boolean hasPermission = player.hasPermission("core.champion");

        setItem(19, createTellItem(realmPlayer.canReceivePrivateMessages(), hasPermission));
        setItem(28, createToggleItem(realmPlayer.canReceivePrivateMessages(), "PlayerTell", hasPermission));
    }

    private GuiItem createTellItem(boolean isEnabled, boolean hasPermission) {
        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.chat-preferences.tell-item.name");
        List<String> lore = getLoreFromConfig("gui.chat-preferences.tell-item.lore");
        String status = translations.getMessage("gui.chat-preferences.toggle.name_" + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getMessage("gui.chat-preferences.status-line", "status", status));
        if (!hasPermission) {
            lore.add("");
            lore.add(translations.getMessage("gui.preferences.toggle.permission-required"));
        }

        ItemStack item = new ItemBuilder(Material.PAPER)
                .setName(name)
                .setLore(lore)
                .hideFlags()
                .build();

        // A ação de clique foi removida daqui e centralizada no botão de toggle
        return new GuiItem(item);
    }

    private GuiItem createToggleItem(boolean isEnabled, String preferenceName, boolean hasPermission) {
        String name = isEnabled ? "&cDesativar" : "&aAtivar";
        List<String> lore = getLoreFromConfig("gui.chat-preferences.toggle.lore");
        Material material = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;

        ItemStack item = new ItemBuilder(material)
                .setName(name)
                .setLore(lore)
                .build();

        return new GuiItem(item, event -> sendTogglePreferenceMessage(preferenceName, hasPermission));
    }

    private void sendTogglePreferenceMessage(String preferenceName, boolean hasPermission) {
        if (!hasPermission) {
            CoreAPI.getInstance().getSoundManager().playError(player);
            return;
        }

        // Toca o som para feedback instantâneo ao jogador
        CoreAPI.getInstance().getSoundManager().playSuccess(player);

        // Apenas envia a requisição para o proxy. A atualização visual
        // será acionada pelo PreferenceUpdateListener quando a mudança for confirmada.
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UpdatePreference");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(preferenceName);
        player.sendPluginMessage(plugin, "proxy:preferences", out.toByteArray());
    }

    private GuiItem createBackItem() {
        String name = translations.getMessage("gui.chat-preferences.back-item.name");
        List<String> lore = getLoreFromConfig("gui.chat-preferences.back-item.lore");

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