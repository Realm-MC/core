package br.com.realmmc.core.gui;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public abstract class Gui {

    protected final Player player;
    protected Inventory inventory;
    protected final Map<Integer, GuiItem> items = new HashMap<>();
    protected final Main plugin; // CAMPO ADICIONADO

    // CONSTRUTOR ATUALIZADO para obter a instância do plugin
    public Gui(Player player) {
        this.player = player;
        this.plugin = CoreAPI.getInstance().getPlugin();
    }

    public void open() {
        // A linha abaixo usa o getTitle() que será implementado pelas subclasses
        this.inventory = Bukkit.createInventory(null, getSize(), getTitle());
        this.setupItems();
        this.player.openInventory(this.inventory);
        CoreAPI.getInstance().getGuiManager().getOpenGuis().put(player.getUniqueId(), this);
    }

    public abstract String getTitle();
    public abstract int getSize();
    public abstract void setupItems();

    public void setItem(int slot, GuiItem guiItem) {
        if (this.inventory != null && slot >= 0 && slot < this.inventory.getSize()) {
            this.inventory.setItem(slot, guiItem.getItemStack());
            this.items.put(slot, guiItem);
        }
    }

    public void setItem(int slot, ItemStack itemStack) {
        setItem(slot, new GuiItem(itemStack));
    }



    public void fillBorders(ItemStack item) {
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