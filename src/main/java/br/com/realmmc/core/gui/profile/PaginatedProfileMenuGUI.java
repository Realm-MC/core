package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.gui.PaginatedGui;
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

public abstract class PaginatedProfileMenuGUI extends PaginatedGui {

    protected final TranslationsManager translations;

    public PaginatedProfileMenuGUI(Player player, List<Integer> itemSlots) {
        super(player, itemSlots);
        this.translations = CoreAPI.getInstance().getTranslationsManager();
    }

    @Override
    public void setupStaticItems() {
        // Agora o cabeçalho completo é configurado aqui
        setupHeader();
    }

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
        ItemStack item = new ItemBuilder(Material.PLAYER_HEAD)
                .setName(translations.getMessage("gui.profile.friends-item.name"))
                .setLore(getLoreFromConfig("gui.profile.friends-item.lore"))
                .hideFlags()
                .build();
        return new GuiItem(item, event -> showComingSoon());
    }

    private GuiItem createPunishmentsItem() {
        ItemStack item = new ItemBuilder(Material.BOOK)
                .setName(translations.getMessage("gui.profile.punishments-item.name"))
                .setLore(getLoreFromConfig("gui.profile.punishments-item.lore"))
                .hideFlags()
                .build();
        return new GuiItem(item, event -> showComingSoon());
    }

    private GuiItem createPurchaseHistoryItem() {
        ItemStack item = new ItemBuilder(Material.EMERALD)
                .setName(translations.getMessage("gui.profile.purchase-history-item.name"))
                .setLore(getLoreFromConfig("gui.profile.purchase-history-item.lore"))
                .hideFlags()
                .build();

        return new GuiItem(item, event -> {
            // Se o jogador clicar neste item, não faz nada, pois ele já está no menu.
            // Apenas toca um som de erro para indicar que a ação é inválida.
            CoreAPI.getInstance().getSoundManager().playError(player);
        });
    }

    protected void showComingSoon() {
        ActionBarManager actionBar = CoreAPI.getInstance().getActionBarManager();
        String message = "&cEste sistema será lançado em breve!";
        actionBar.setMessage(player, ActionBarManager.MessagePriority.MEDIUM, "feedback", message, 3);
        CoreAPI.getInstance().getSoundManager().playError(player);
    }

    @Override
    protected void addPageNavigation(List<GuiItem> allItems) {
        int totalItems = allItems.size();
        int maxItemsPerPage = itemSlots.size();

        // Botão de voltar página (SLOT 28)
        if (page > 0) {
            GuiItem previousPage = new GuiItem(
                    createItem(
                            Material.ARROW,
                            translations.getRawMessage("gui.purchase-history.previous-page-item.name"),
                            getLoreFromConfig("gui.purchase-history.previous-page-item.lore", "page", String.valueOf(page))
                    ),
                    event -> {
                        // O construtor da subclasse precisa ser chamado com a nova página
                        openNewPage(page - 1);
                        CoreAPI.getInstance().getSoundManager().playClick(player);
                    }
            );
            setItem(27, previousPage);
        }

        // Botão de avançar página (SLOT 36)
        if ((page + 1) * maxItemsPerPage < totalItems) {
            GuiItem nextPage = new GuiItem(
                    createItem(
                            Material.ARROW,
                            translations.getRawMessage("gui.purchase-history.next-page-item.name"),
                            getLoreFromConfig("gui.purchase-history.next-page-item.lore", "page", String.valueOf(page + 2))
                    ),
                    event -> {
                        openNewPage(page + 1);
                        CoreAPI.getInstance().getSoundManager().playClick(player);
                    }
            );
            setItem(35, nextPage);
        }
    }

    /**
     * Método abstrato para forçar as subclasses a implementar como abrir uma nova página de si mesma.
     * @param newPage O número da nova página a ser aberta.
     */
    protected abstract void openNewPage(int newPage);

    protected List<String> getLoreFromConfig(String key, String... replacements) {
        List<String> rawLore = translations.getConfig().getStringList(key);
        return rawLore.stream().map(line -> {
            for (int i = 0; i < replacements.length; i += 2) {
                line = line.replace("{" + replacements[i] + "}", replacements[i+1]);
            }
            return line;
        }).collect(Collectors.toList());
    }
}