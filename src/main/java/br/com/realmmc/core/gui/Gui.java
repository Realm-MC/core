package br.com.realmmc.core.gui;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.TranslationsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Gui implements InventoryHolder {

    protected final Player player;
    protected final Main plugin;
    protected final TranslationsManager translations;
    protected Inventory inventory;
    private final Map<Integer, GuiItem> items = new HashMap<>();
    private boolean cancelClicks = true;

    public Gui(Player player) {
        this.player = player;
        this.plugin = CoreAPI.getInstance().getPlugin();
        this.translations = CoreAPI.getInstance().getTranslationsManager();
    }

    public abstract String getTitle();
    public abstract int getSize();
    public abstract void setupItems();

    private void setupInventory() {
        this.inventory = Bukkit.createInventory(this, getSize(), getTitle());
    }

    public void open() {
        if (this.inventory == null) {
            setupInventory();
            setupItems();
        }
        this.player.openInventory(this.inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        if (this.cancelClicks) {
            event.setCancelled(true);
        }
        GuiItem item = this.items.get(event.getSlot());
        if (item != null && item.getAction() != null) {
            item.getAction().accept(event);
        }
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void setItem(int slot, ItemStack item) {
        this.inventory.setItem(slot, item);
    }

    public void setItem(int slot, GuiItem item) {
        this.items.put(slot, item);
        this.inventory.setItem(slot, item.getItemStack());
    }

    public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        setItem(slot, new GuiItem(item, action));
    }

    public void setCancelClicks(boolean cancelClicks) {
        this.cancelClicks = cancelClicks;
    }

    protected List<String> getLoreFromConfig(String key) {
        return this.translations.getConfig().getStringList(key);
    }

    // <-- NOVO MÉTODO ADICIONADO -->
    protected int slot(int row, int column) {
        return (column - 1) + ((row - 1) * 9);
    }
}