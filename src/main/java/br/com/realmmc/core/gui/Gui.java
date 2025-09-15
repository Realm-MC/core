package br.com.realmmc.core.gui;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.managers.TranslationsManager;
import br.com.realmmc.core.utils.ColorAPI;
import br.com.realmmc.core.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Gui {

    protected final Player player;
    protected Inventory inventory;
    protected final Map<Integer, GuiItem> items = new HashMap<>();
    protected final Main plugin;
    protected final TranslationsManager translations;

    public Gui(Player player) {
        this.player = player;
        this.plugin = CoreAPI.getInstance().getPlugin();
        this.translations = CoreAPI.getInstance().getTranslationsManager();
    }

    public void open() {
        this.inventory = Bukkit.createInventory(null, getSize(), ColorAPI.format(getTitle()));
        this.setupItems();
        this.player.openInventory(this.inventory);
        CoreAPI.getInstance().getGuiManager().getOpenGuis().put(player.getUniqueId(), this);
    }

    public abstract String getTitle();
    public abstract int getSize();
    public abstract void setupItems();

    protected boolean cancelClicks = true;

    protected void setCancelClicks(boolean cancelClicks) {
        this.cancelClicks = cancelClicks;
    }

    public boolean areClicksCancelled() {
        return this.cancelClicks;
    }

    protected int slot(int row, int column) {
        return ((row - 1) * 9) + (column - 1);
    }

    protected void setItem(int slot, ItemStack item) {
        setItem(slot, new GuiItem(item, null));
    }

    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        setItem(slot, new GuiItem(item, action));
    }

    protected void setItem(int slot, GuiItem guiItem) {
        if (this.inventory != null && slot >= 0 && slot < this.inventory.getSize()) {
            this.inventory.setItem(slot, guiItem.getItemStack());
            if (guiItem.getAction() != null) {
                this.items.put(slot, guiItem);
            }
        }
    }

    protected List<String> getLoreFromConfig(String key) {
        return translations.getConfig().getStringList(key);
    }

    protected void fillBorders(ItemStack item) {
        int invSize = getSize();
        for (int i = 0; i < invSize; i++) {
            if (i < 9 || i >= invSize - 9 || i % 9 == 0 || (i + 1) % 9 == 0) {
                setItem(i, item);
            }
        }
    }

    public Map<Integer, GuiItem> getItems() {
        return items;
    }

    public Inventory getInventory() {
        return inventory;
    }
}