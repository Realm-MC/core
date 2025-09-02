package br.com.realmmc.core.gui.profile;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.gui.GuiItem;
import br.com.realmmc.core.model.Purchase;
import br.com.realmmc.core.utils.DateFormatter;
import br.com.realmmc.core.utils.ItemBuilder;
import br.com.realmmc.core.utils.JsonMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PurchaseHistoryGUI extends PaginatedProfileMenuGUI {

    private static final List<Integer> ITEM_SLOTS = Arrays.asList(
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    );

    private static final Map<String, String> GROUP_COLORS = Map.ofEntries(
            Map.entry("master", "&6"), Map.entry("manager", "&4"), Map.entry("administrator", "&c"),
            Map.entry("moderator", "&2"), Map.entry("helper", "&e"), Map.entry("builder", "&3"),
            Map.entry("partner", "&c"), Map.entry("supreme", "&4"), Map.entry("legendary", "&2"),
            Map.entry("hero", "&5"), Map.entry("champion", "&3"), Map.entry("default", "&7"),
            Map.entry("test", "&3"), Map.entry("momo", "&d")
    );

    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
    private final LuckPerms luckPerms;

    public PurchaseHistoryGUI(Player player, int page) {
        super(player, ITEM_SLOTS);
        this.page = page;
        this.luckPerms = CoreAPI.getInstance().getPlugin().getLuckPerms();
    }

    public PurchaseHistoryGUI(Player player) {
        this(player, 0);
    }

    @Override
    protected void openNewPage(int newPage) {
        new PurchaseHistoryGUI(player, newPage).open();
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
    public void setupStaticItems() {
        super.setupStaticItems();
        ItemStack separatorPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(translations.getMessage("gui.purchase-history.separator-item.name"))
                .setLore(getLoreFromConfig("gui.purchase-history.separator-item.lore"))
                .build();
        for (int i = 9; i < 18; i++) {
            setItem(i, separatorPane);
        }
        setItem(49, createBackItem());
    }

    @Override
    protected void displayEmptyMessage() {
        setItem(31, createNoPurchasesItem());
    }

    @Override
    public CompletableFuture<List<GuiItem>> fetchPageItems() {
        return CoreAPI.getInstance().getPurchaseHistoryReader().getPurchaseHistory(player.getUniqueId())
                .thenApply(purchases -> purchases.stream()
                        .map(purchase -> "VIP".equals(purchase.type()) ? createVipItem(purchase) : createCashItem(purchase))
                        .collect(Collectors.toList()));
    }

    private GuiItem createVipItem(Purchase purchase) {
        String groupId = purchase.name();
        Group group = this.luckPerms.getGroupManager().getGroup(groupId);
        String groupDisplayName = (group != null && group.getDisplayName() != null) ? group.getDisplayName() : groupId;

        String statusMessage = translations.getRawMessage("gui.purchase-history.status." + purchase.status().toLowerCase());
        String color = GROUP_COLORS.getOrDefault(groupId.toLowerCase(), "&7");
        String finalItemName = color + groupDisplayName;

        ItemStack item = new ItemBuilder(Material.DIAMOND)
                .setName(finalItemName)
                .setLore(getLoreFromConfig("gui.purchase-history.vip-item.lore",
                        "activation_date", DateFormatter.format(purchase.date()),
                        "expiration_date", purchase.expirationDate() != null ? DateFormatter.format(purchase.expirationDate()) : "Permanente",
                        "id", purchase.id(),
                        "status", statusMessage))
                .build();
        return new GuiItem(item);
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

    private GuiItem createNoPurchasesItem() {
        ItemStack item = new ItemBuilder(Material.COBWEB)
                .setName(translations.getMessage("gui.purchase-history.no-purchases.name"))
                .setLore(getLoreFromConfig("gui.purchase-history.no-purchases.lore"))
                .build();

        return new GuiItem(item, event -> {
            player.closeInventory();
            CoreAPI.getInstance().getSoundManager().playSuccess(player);

            String storeUrl = translations.getRawMessage("general.store-url");
            List<String> messageLines = translations.getConfig().getStringList("gui.purchase-history.no-purchases.click-message");
            String hoverText = translations.getRawMessage("gui.purchase-history.no-purchases.click-hover");

            for (String line : messageLines) {
                if (line.contains("AQUI")) {
                    String[] parts = line.split("AQUI");
                    JsonMessage jsonMessage = JsonMessage.create(parts[0]);
                    jsonMessage.then("&e&lAQUI")
                            .url(storeUrl)
                            .withHover(hoverText);
                    if (parts.length > 1) {
                        jsonMessage.then(parts[1]);
                    }
                    jsonMessage.send(player);
                } else {
                    player.sendMessage(CoreAPI.getInstance().getTranslationsManager().getMessage(line));
                }
            }
        });
    }

    private GuiItem createBackItem() {
        ItemStack item = new ItemBuilder(Material.ARROW)
                .setName(translations.getMessage("gui.purchase-history.back-item.name"))
                .setLore(getLoreFromConfig("gui.purchase-history.back-item.lore"))
                .build();
        return new GuiItem(item, event -> new ProfileGUI(player).open());
    }
}