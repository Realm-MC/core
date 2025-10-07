package br.com.realmmc.core.gui;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Classe base para GUIs que possuem múltiplas páginas e carregam seus itens de forma assíncrona.
 */
public abstract class PaginatedGui extends Gui {

    protected int page = 0;
    protected final List<Integer> itemSlots;

    public PaginatedGui(Player player, List<Integer> itemSlots) {
        super(player);
        this.itemSlots = itemSlots != null ? itemSlots : Collections.emptyList();
    }

    /**
     * Configura os itens estáticos da GUI (cabeçalho, bordas, botões fixos).
     * Este método é chamado primeiro, exibindo um "esqueleto" do menu.
     */
    public abstract void setupStaticItems();

    /**
     * Busca os itens da página de forma assíncrona.
     * @return um CompletableFuture que, quando completo, contém a lista de GuiItems para a página.
     */
    public abstract CompletableFuture<List<GuiItem>> fetchPageItems();

    /**
     * Define o que fazer quando a lista de itens está vazia (ex: colocar um item de aviso).
     */
    protected abstract void displayEmptyMessage();

    /**
     * Adiciona os botões de navegação (página anterior/próxima).
     * @param allItems A lista total de itens para calcular se há mais páginas.
     */
    protected abstract void addPageNavigation(List<GuiItem> allItems);

    @Override
    public void open() {
        CoreAPI.getInstance().getSoundManager().playClick(player);

        // 1. Cria o inventário e configura os itens estáticos.
        if (this.inventory == null) {
            // Usa o getTitle() e getSize() da classe filha
            this.inventory = plugin.getServer().createInventory(this, getSize(), getTitle());
        }
        setupStaticItems();

        // 2. Exibe o menu "esqueleto" para o jogador.
        player.openInventory(this.inventory);

        // <-- MUDANÇA: A linha `GuiManager.getOpenGuis().put(...)` foi removida daqui. -->

        // 3. Inicia a busca assíncrona pelos itens.
        fetchPageItems().thenAccept(guiItems -> {
            // 4. Quando a busca terminar, popula o inventário na thread principal.
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // <-- MUDANÇA: Lógica de verificação moderna e segura -->
                InventoryHolder currentHolder = player.getOpenInventory().getTopInventory().getHolder();
                if (currentHolder == this) {
                    populateItems(guiItems);
                }
            });
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Falha ao carregar itens para a GUI paginada: " + getTitle());
            ex.printStackTrace();
            return null;
        });
    }

    @Override
    public final void setupItems() {
        // A lógica principal foi movida para o método open() para suportar a assincronicidade.
    }

    /**
     * Popula o inventário com os itens buscados e adiciona os botões de navegação.
     */
    protected void populateItems(List<GuiItem> allItems) {
        for (int slot : itemSlots) {
            inventory.setItem(slot, null);
        }

        if (allItems == null || allItems.isEmpty()) {
            displayEmptyMessage();
            addPageNavigation(Collections.emptyList());
            return;
        }

        int maxItemsPerPage = itemSlots.size();
        int startIndex = page * maxItemsPerPage;

        for (int i = 0; i < maxItemsPerPage; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= allItems.size()) break;

            int slot = itemSlots.get(i);
            setItem(slot, allItems.get(itemIndex));
        }

        addPageNavigation(allItems);
    }

    /**
     * Helper para criar um ItemStack de forma rápida.
     */
    protected ItemStack createItem(Material material, String name, List<String> lore) {
        return new ItemBuilder(material).setName(name).setLore(lore).hideFlags().build();
    }
}