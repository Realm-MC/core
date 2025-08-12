package br.com.realmmc.core.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class GuiItem {

    private final ItemStack itemStack;
    private final Consumer<InventoryClickEvent> action;

    public GuiItem(ItemStack itemStack, Consumer<InventoryClickEvent> action) {
        this.itemStack = itemStack;
        this.action = action;
    }

    public GuiItem(ItemStack itemStack) {
        this(itemStack, event -> {});
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Consumer<InventoryClickEvent> getAction() {
        return action;
    }
}