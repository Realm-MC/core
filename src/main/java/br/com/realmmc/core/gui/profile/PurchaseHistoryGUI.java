package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.model.Purchase;
import br.com.realmmc.core.users.GroupInfoReader;
import br.com.realmmc.core.utils.DateFormatter;
import br.com.realmmc.core.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PurchaseHistoryGUI extends BaseProfileMenuGUI {

    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
    private final int page;
    private static final int ITEMS_PER_PAGE = 14;

    public PurchaseHistoryGUI(Player player) {
        this(player, 1);
    }

    public PurchaseHistoryGUI(Player player, int page) {
        super(player);
        this.page = page;
    }

    @Override
    public String getTitle() {
        return translations.getMessage("gui.purchase-history.title");
    }

    @Override
    public int getSize() {
        return 6 * 9;
    }

    @Override
    public void setupItems() {
        setupHeader();

        ItemStack separatorPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(translations.getMessage("gui.purchase-history.separator-item.name"))
                .setLore(getLoreFromConfig("gui.purchase-history.separator-item.lore"))
                .build();
        for (int i = 9; i < 18; i++) {
            setItem(i, separatorPane);
        }

        setItem(49, createBackItem());

        CoreAPI.getInstance().getPurchaseHistoryReader().getPurchaseHistory(player.getUniqueId())
                .thenAccept(purchases -> {
                    player.getServer().getScheduler().runTask(this.plugin, () -> {
                        if (purchases.isEmpty()) {
                            setItem(31, createNoPurchasesItem());
                            return;
                        }

                        int startIndex = (page - 1) * ITEMS_PER_PAGE;
                        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, purchases.size());

                        if (startIndex >= purchases.size() && page > 1) {
                            new PurchaseHistoryGUI(player, 1).open();
                            return;
                        }

                        List<Purchase> pageItems = purchases.subList(startIndex, endIndex);

                        List<Integer> availableSlots = Arrays.asList(
                                20, 21, 22, 23, 24, 25, 26,
                                29, 30, 31, 32, 33, 34, 35
                        );

                        for (int i = 0; i < pageItems.size(); i++) {
                            Purchase purchase = pageItems.get(i);
                            int slot = availableSlots.get(i);
                            if (purchase.type().equals("VIP")) {
                                createVipItem(purchase, slot);
                            } else if (purchase.type().equals("CASH")) {
                                setItem(slot, createCashItem(purchase));
                            }
                        }

                        if (page > 1) {
                            setItem(48, createPreviousPageItem());
                        }
                        if (endIndex < purchases.size()) {
                            setItem(50, createNextPageItem());
                        }
                    });
                });
    }

    // =======================================================================
    // MÉTODOS QUE FALTAVAM (ADICIONADOS ABAIXO)
    // =======================================================================

    private GuiItem createNoPurchasesItem() {
        ItemStack item = new ItemBuilder(Material.COBWEB)
                .setName(translations.getMessage("gui.purchase-history.no-purchases.name"))
                .setLore(getLoreFromConfig("gui.purchase-history.no-purchases.lore"))
                .build();

        return new GuiItem(item, event -> {
            player.closeInventory();
            player.sendMessage("§aVisite nossa loja em: §b§nloja.seuservidor.com");
            CoreAPI.getInstance().getSoundManager().playClick(player);
        });
    }

    private void createVipItem(Purchase purchase, int slot) {
        GroupInfoReader groupInfoReader = CoreAPI.getInstance().getGroupInfoReader();
        // A busca do nome do grupo é assíncrona
        groupInfoReader.getDisplayName(purchase.name()).thenAccept(groupDisplayName -> {
            String statusMessage = translations.getRawMessage("gui.purchase-history.status." + purchase.status().toLowerCase());

            ItemStack item = new ItemBuilder(Material.DIAMOND)
                    .setName(translations.getMessage("gui.purchase-history.vip-item.name", "group_display_name", groupDisplayName.orElse(purchase.name())))
                    .setLore(getLoreFromConfig("gui.purchase-history.vip-item.lore",
                            "activation_date", DateFormatter.format(purchase.date()),
                            "expiration_date", purchase.expirationDate() != null ? DateFormatter.format(purchase.expirationDate()) : "Permanente",
                            "id", purchase.id(),
                            "status", statusMessage))
                    .build();

            // Atualiza o item na GUI quando a busca terminar
            player.getServer().getScheduler().runTask(plugin, () -> setItem(slot, new GuiItem(item)));
        });
    }

    private GuiItem createCashItem(Purchase purchase) {
        String formattedAmount = numberFormat.format(Long.parseLong(purchase.name()));
        String statusMessage = translations.getRawMessage("gui.purchase-history.status." + purchase.status().toLowerCase());

        ItemStack item = new ItemBuilder(Material.GOLD_INGOT)
                .setName(translations.getMessage("gui.purchase-history.cash-item.name", "amount", formattedAmount))
                .setLore(getLoreFromConfig("gui.purchase-history.cash-item.lore",
                        "reception_date", DateFormatter.format(purchase.date()),
                        "id", purchase.id(),
                        "status", statusMessage))
                .build();
        return new GuiItem(item);
    }

    private GuiItem createBackItem() {
        ItemStack item = new ItemBuilder(Material.ARROW)
                .setName(translations.getMessage("gui.purchase-history.back-item.name"))
                .setLore(getLoreFromConfig("gui.purchase-history.back-item.lore"))
                .build();
        return new GuiItem(item, event -> new ProfileGUI(player).open());
    }

    private GuiItem createPreviousPageItem() {
        ItemStack item = new ItemBuilder(Material.ARROW)
                .setName(translations.getMessage("gui.purchase-history.previous-page-item.name", "page", String.valueOf(page - 1)))
                .setLore(getLoreFromConfig("gui.purchase-history.previous-page-item.lore", "page", String.valueOf(page - 1)))
                .build();
        return new GuiItem(item, event -> new PurchaseHistoryGUI(player, page - 1).open());
    }

    private GuiItem createNextPageItem() {
        ItemStack item = new ItemBuilder(Material.ARROW)
                .setName(translations.getMessage("gui.purchase-history.next-page-item.name", "page", String.valueOf(page + 1)))
                .setLore(getLoreFromConfig("gui.purchase-history.next-page-item.lore", "page", String.valueOf(page + 1)))
                .build();
        return new GuiItem(item, event -> new PurchaseHistoryGUI(player, page + 1).open());
    }
}