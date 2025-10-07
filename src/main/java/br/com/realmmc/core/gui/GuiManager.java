package br.com.realmmc.core.gui;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GuiManager implements Listener {

    private final Main plugin;
    private final Map<String, Consumer<Player>> guiOpeners = new ConcurrentHashMap<>();

    public GuiManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void registerGui(String name, Consumer<Player> opener) {
        guiOpeners.put(name.toLowerCase(), opener);
        plugin.getLogger().info("Menu '" + name + "' registado com sucesso no GuiManager.");
    }

    public void openGui(Player player, String name) {
        Consumer<Player> opener = guiOpeners.get(name.toLowerCase());
        if (opener != null) {
            opener.accept(player);
        } else {
            CoreAPI.getInstance().getTranslationsManager().sendMessage(player, "general.menu-not-found", "menu_name", name);
            CoreAPI.getInstance().getSoundManager().playError(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof Gui gui) {
            gui.handleClick(event);
        }
    }
}