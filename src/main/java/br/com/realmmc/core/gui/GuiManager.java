package br.com.realmmc.core.gui;

import br.com.realmmc.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager implements Listener {

    private final Map<UUID, Gui> openGuis = new HashMap<>();

    public GuiManager(Main plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Gui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        // Cancela o evento para impedir que o jogador pegue ou mova itens.
        event.setCancelled(true);

        // --- LÓGICA DE VERIFICAÇÃO ADICIONADA AQUI ---
        // Pega o inventário que foi clicado.
        Inventory clickedInventory = event.getClickedInventory();

        // Se o clique não foi no inventário da GUI (parte de cima), ignora a ação.
        if (clickedInventory == null || !clickedInventory.equals(gui.getInventory())) {
            return;
        }

        // Se o clique foi na GUI, processa a ação do item.
        GuiItem clickedItem = gui.getItems().get(event.getSlot());
        if (clickedItem != null && clickedItem.getAction() != null) {
            clickedItem.getAction().accept(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openGuis.remove(event.getPlayer().getUniqueId());
    }

    public Map<UUID, Gui> getOpenGuis() {
        return openGuis;
    }
}