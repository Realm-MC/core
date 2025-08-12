package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.Gui;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.managers.ActionBarManager;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.player.RealmPlayer;
import br.com.realmmc.core.utils.ColorAPI;
import br.com.realmmc.core.utils.DateFormatter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseProfileMenuGUI extends Gui {

    protected final TranslationsManager translations;

    public BaseProfileMenuGUI(Player player) {
        super(player);
        this.translations = CoreAPI.getInstance().getTranslationsManager();
    }

    protected void setupHeader() {
        setItem(3, createFriendsItem());
        setItem(5, createPunishmentsItem());

        CoreAPI.getInstance().getPlayerDataManager().getRealmPlayer(player).ifPresent(this::createHeadItem);
    }

    private void createHeadItem(RealmPlayer realmPlayer) {
        String formattedName = realmPlayer.getPrefix() + realmPlayer.getUsername();
        List<String> lore = getLoreFromConfig("gui.profile.head-item.lore",
                "player_group", realmPlayer.getPrimaryGroup(),
                "first_join_date", DateFormatter.format(realmPlayer.getFirstLogin()),
                "last_join_date", DateFormatter.format(realmPlayer.getLastLogin())
        );
        ItemStack head = createItem(Material.PLAYER_HEAD, formattedName, lore);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
        }
        this.inventory.setItem(4, head);
    }

    private GuiItem createFriendsItem() {
        String name = translations.getMessage("gui.profile.friends-item.name");
        List<String> lore = getLoreFromConfig("gui.profile.friends-item.lore");
        return new GuiItem(createItem(Material.PLAYER_HEAD, name, lore), event -> showComingSoon());
    }

    private GuiItem createPunishmentsItem() {
        String name = translations.getMessage("gui.profile.punishments-item.name");
        List<String> lore = getLoreFromConfig("gui.profile.punishments-item.lore");
        return new GuiItem(createItem(Material.BOOK, name, lore), event -> {
            // Futuramente, pode executar um comando que abre a GUI de punições do Proxy.
            showComingSoon();
        });
    }

    protected void showComingSoon() {
        player.closeInventory();
        ActionBarManager actionBar = CoreAPI.getInstance().getActionBarManager();
        String message = "&cEste sistema será lançado em breve!";
        actionBar.setMessage(player, ActionBarManager.MessagePriority.MEDIUM, "feedback", message, 3);
        CoreAPI.getInstance().getSoundManager().playError(player);
    }

    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorAPI.format(name));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    protected List<String> getLoreFromConfig(String key, String... replacements) {
        List<String> rawLore = translations.getConfig().getStringList(key);
        return rawLore.stream().map(line -> {
            for (int i = 0; i < replacements.length; i += 2) {
                line = line.replace("{" + replacements[i] + "}", replacements[i+1]);
            }
            return ColorAPI.format(line);
        }).collect(Collectors.toList());
    }
}