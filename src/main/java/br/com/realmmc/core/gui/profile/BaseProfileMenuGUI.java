package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.Gui;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.managers.ActionBarManager;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.player.RealmPlayer;
import br.com.realmmc.core.utils.DateFormatter;
import br.com.realmmc.core.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseProfileMenuGUI extends Gui {

    protected final TranslationsManager translations;

    public BaseProfileMenuGUI(Player player) {
        super(player);
        this.translations = CoreAPI.getInstance().getTranslationsManager();
    }

    /**
     * Configura o cabeçalho padrão com informações do jogador e itens de navegação.
     */
    protected void setupHeader() {
        setItem(3, createFriendsItem());
        CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(player).ifPresent(this::createHeadItem);
        setItem(5, createPunishmentsItem());
        setItem(6, createPurchaseHistoryItem());
    }

    private void createHeadItem(RealmPlayer realmPlayer) {
        String formattedName = realmPlayer.getPrefix() + realmPlayer.getUsername();
        List<String> lore = getLoreFromConfig("gui.profile.head-item.lore",
                "player_group", realmPlayer.getPrimaryGroup(),
                "cash", String.valueOf(realmPlayer.getCash()),
                "first_join_date", DateFormatter.format(realmPlayer.getFirstLogin()),
                "last_join_date", DateFormatter.format(realmPlayer.getLastLogin())
        );

        ItemStack head = new ItemBuilder(Material.PLAYER_HEAD)
                .setName(formattedName)
                .setLore(lore)
                .hideFlags()
                .build();

        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
        }
        setItem(4, new GuiItem(head));
    }

    private GuiItem createFriendsItem() {
        ItemStack item = createItem(
                Material.PLAYER_HEAD,
                translations.getMessage("gui.profile.friends-item.name"),
                getLoreFromConfig("gui.profile.friends-item.lore")
        );
        return new GuiItem(item, event -> showComingSoon());
    }

    private GuiItem createPunishmentsItem() {
        ItemStack item = createItem(
                Material.BOOK,
                translations.getMessage("gui.profile.punishments-item.name"),
                getLoreFromConfig("gui.profile.punishments-item.lore")
        );
        return new GuiItem(item, event -> showComingSoon());
    }

    private GuiItem createPurchaseHistoryItem() {
        ItemStack item = createItem(
                Material.EMERALD,
                translations.getMessage("gui.profile.purchase-history-item.name"),
                getLoreFromConfig("gui.profile.purchase-history-item.lore")
        );
        return new GuiItem(item, event -> {
            CoreAPI.getInstance().getSoundManager().playClick(player);
            new PurchaseHistoryGUI(player).open();
        });
    }

    protected void showComingSoon() {
        ActionBarManager actionBar = CoreAPI.getInstance().getActionBarManager();
        String message = "&cEste sistema será lançado em breve!";
        actionBar.setMessage(player, ActionBarManager.MessagePriority.MEDIUM, "feedback", message, 3);
        CoreAPI.getInstance().getSoundManager().playError(player);
    }

    /**
     * Helper para buscar uma lista de strings (lore) do arquivo de tradução.
     */
    protected List<String> getLoreFromConfig(String key, String... replacements) {
        List<String> rawLore = translations.getConfig().getStringList(key);
        return rawLore.stream().map(line -> {
            for (int i = 0; i < replacements.length; i += 2) {
                line = line.replace("{" + replacements[i] + "}", replacements[i+1]);
            }
            return line;
        }).collect(Collectors.toList());
    }

    /**
     * Helper para criar um ItemStack de forma rápida, evitando conflitos.
     */
    protected ItemStack createItem(Material material, String name, List<String> lore) {
        return new ItemBuilder(material).setName(name).setLore(lore).hideFlags().build();
    }
}