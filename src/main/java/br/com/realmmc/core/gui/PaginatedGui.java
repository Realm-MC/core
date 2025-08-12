package br.com.realmmc.core.gui;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.utils.ColorAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PaginatedGui extends Gui {

    protected int page = 0;
    protected final List<Integer> itemSlots; // Alterado para protected

    public PaginatedGui(Player player, List<Integer> itemSlots) {
        super(player);
        this.itemSlots = itemSlots;
    }

    public PaginatedGui(Player player) {
        this(player, null);
    }

    public abstract List<GuiItem> getPageItems();

    @Override
    public void setupItems() {
        items.clear();
        inventory.clear();

        List<GuiItem> allItems = getPageItems();
        if (allItems == null) allItems = Collections.emptyList();

        populateItems(allItems);
        addPageNavigation(allItems);
    }

    private void populateItems(List<GuiItem> allItems) {
        if (allItems.isEmpty()) return;

        int maxItemsPerPage = (itemSlots != null && !itemSlots.isEmpty()) ? itemSlots.size() : getSize() - 18;
        int startIndex = page * maxItemsPerPage;

        for (int i = 0; i < maxItemsPerPage; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= allItems.size()) break;

            int slot;
            if (itemSlots != null && !itemSlots.isEmpty()) {
                slot = itemSlots.get(i);
            } else {
                slot = i + 9;
            }
            setItem(slot, allItems.get(itemIndex));
        }
    }

    // --- MÉTODO ALTERADO PARA PROTECTED ---
    protected void addPageNavigation(List<GuiItem> allItems) {
        int totalItems = allItems.size();
        int maxItemsPerPage = (itemSlots != null && !itemSlots.isEmpty()) ? itemSlots.size() : itemSlots.size();

        // Botão de voltar página (SLOT 9)
        if (page > 0) {
            GuiItem previousPage = new GuiItem(
                    createItem(
                            Material.ARROW,
                            CoreAPI.getInstance().getTranslationsManager().getRawMessage("gui.pagination.previous-page-name"),
                            Collections.singletonList(CoreAPI.getInstance().getTranslationsManager().getRawMessage("gui.pagination.previous-page-lore"))
                    ),
                    event -> {
                        page--;
                        open();
                        CoreAPI.getInstance().getSoundManager().playClick(player);
                    }
            );
            setItem(9, previousPage);
        }

        // Botão de avançar página (SLOT 17)
        if ((page + 1) * maxItemsPerPage < totalItems) {
            GuiItem nextPage = new GuiItem(
                    createItem(
                            Material.ARROW,
                            CoreAPI.getInstance().getTranslationsManager().getRawMessage("gui.pagination.next-page-name"),
                            Collections.singletonList(CoreAPI.getInstance().getTranslationsManager().getRawMessage("gui.pagination.next-page-lore"))
                    ),
                    event -> {
                        page++;
                        open();
                        CoreAPI.getInstance().getSoundManager().playClick(player);
                    }
            );
            setItem(17, nextPage);
        }
    }

    // --- MÉTODO ALTERADO PARA PROTECTED ---
    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorAPI.format(name));
            meta.setLore(lore.stream().map(ColorAPI::format).collect(Collectors.toList()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}