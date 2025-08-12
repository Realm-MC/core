package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

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
        ItemStack separatorPane = createItem(
                Material.BLACK_STAINED_GLASS_PANE,
                translations.getMessage("gui.chat-preferences.separator-item.name"),
                getLoreFromConfig("gui.chat-preferences.separator-item.lore")
        );
        for (int i = 9; i <= 17; i++) {
            setItem(i, separatorPane);
        }
        fetchAndBuild();
        setItem(49, createBackItem());
    }

    private void fetchAndBuild() {
        CoreAPI.getInstance().getUserPreferenceReader().canReceiveTell(player.getUniqueId()).thenAccept(isEnabled -> {
            player.getServer().getScheduler().runTask(CoreAPI.getInstance().getPlugin(), () -> {
                boolean hasPermission = player.hasPermission("core.champion");
                buildDynamicItems(isEnabled, hasPermission);
            });
        });
    }

    private void buildDynamicItems(boolean canReceiveTell, boolean hasPermission) {
        setItem(19, createTellItem(canReceiveTell, hasPermission));
        setItem(28, createTellToggleItem(canReceiveTell, hasPermission));
    }

    private GuiItem createTellItem(boolean isEnabled, boolean hasPermission) {
        String nameColor = isEnabled ? "&a" : "&c";
        String name = nameColor + translations.getRawMessage("gui.chat-preferences.tell-item.name");
        List<String> lore = getLoreFromConfig("gui.chat-preferences.tell-item.lore");
        String status = translations.getMessage("gui.chat-preferences.toggle.name_" + (isEnabled ? "enabled" : "disabled"));
        lore.add(translations.getMessage("gui.chat-preferences.tell-item.status-line", "status", status));
        if (!hasPermission) {
            lore.add("");
            lore.add(translations.getMessage("gui.preferences.toggle.permission-required"));
        }
        return new GuiItem(createItem(Material.OAK_SIGN, name, lore));
    }

    private GuiItem createTellToggleItem(boolean isEnabled, boolean hasPermission) {
        String name = isEnabled ? "&cDesativar" : "&aAtivar";
        List<String> lore = getLoreFromConfig("gui.chat-preferences.toggle.lore");
        Material material = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;

        return new GuiItem(createItem(material, name, lore), event -> {
            if (!hasPermission) {
                CoreAPI.getInstance().getSoundManager().playError(player);
                return;
            }
            sendTogglePreferenceMessage("PlayerTell", !isEnabled);
        });
    }

    private void sendTogglePreferenceMessage(String preferenceName, boolean newState) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UpdatePreference");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(preferenceName);
        player.sendPluginMessage(CoreAPI.getInstance().getPlugin(), "proxy:preferences", out.toByteArray());

        String messageKey = newState ? "toggle.tell.enabled" : "toggle.tell.disabled";
        translations.sendMessage(player, messageKey);
        CoreAPI.getInstance().getSoundManager().playSuccess(player);

        player.getServer().getScheduler().runTaskLater(CoreAPI.getInstance().getPlugin(), this::fetchAndBuild, 10L);
    }

    private GuiItem createBackItem() {
        String name = translations.getMessage("gui.chat-preferences.back-item.name");
        List<String> lore = getLoreFromConfig("gui.chat-preferences.back-item.lore");
        return new GuiItem(createItem(Material.ARROW, name, lore), event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PreferencesGUI(player).open();
        });
    }
}