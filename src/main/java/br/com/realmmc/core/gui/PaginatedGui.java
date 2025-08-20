package br.com.realmmc.core.gui;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Versão refatorada do PaginatedGui que suporta o carregamento assíncrono de itens.
 * É a classe base para qualquer menu que precise exibir dados de uma fonte externa (como um banco de dados)
 * e que possua múltiplas páginas.
 */
public abstract class PaginatedGui extends Gui {

    protected int page = 0;
    protected final List<Integer> itemSlots;

    public PaginatedGui(Player player, List<Integer> itemSlots) {
        super(player);
        this.itemSlots = itemSlots != null ? itemSlots : Collections.emptyList();
    }

    /**
     * Configura os itens estáticos da GUI, como o cabeçalho, bordas, painéis de separação
     * e botões fixos (ex: "Voltar"). Este método é chamado primeiro, exibindo um "esqueleto" do menu.
     */
    public abstract void setupStaticItems();

    /**
     * Busca os itens da página de forma assíncrona. Este método é o coração da GUI,
     * responsável por buscar os dados e transformá-los em uma lista de GuiItems.
     * @return um CompletableFuture que, quando completo, contém a lista de GuiItems para a página.
     */
    public abstract CompletableFuture<List<GuiItem>> fetchPageItems();

    @Override
    public void open() {
        // Adiciona som ao abrir o menu
        CoreAPI.getInstance().getSoundManager().playClick(player);

        // 1. Cria o inventário e configura os itens estáticos (o "esqueleto")
        this.inventory = plugin.getServer().createInventory(null, getSize(), getTitle());
        setupStaticItems();

        // 2. Exibe o menu "esqueleto" para o jogador imediatamente
        player.openInventory(this.inventory);
        CoreAPI.getInstance().getGuiManager().getOpenGuis().put(player.getUniqueId(), this);

        // 3. Inicia a busca assíncrona pelos itens dinâmicos
        fetchPageItems().thenAccept(guiItems -> {
            // 4. Quando a busca terminar, popula o inventário na thread principal do servidor
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Garante que o jogador ainda está com a GUI aberta
                if (CoreAPI.getInstance().getGuiManager().getOpenGuis().get(player.getUniqueId()) == this) {
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
        // Este método não é mais utilizado diretamente. A lógica foi movida para
        // open(), setupStaticItems() e fetchPageItems() para suportar a assincronicidade.
    }

    /**
     * Popula o inventário com os itens buscados e adiciona os botões de navegação.
     * @param allItems A lista completa de GuiItems retornada por fetchPageItems().
     */
    private void populateItems(List<GuiItem> allItems) {
        // Limpa os slots de itens antes de popular (remove o item "Carregando...")
        for (int slot : itemSlots) {
            inventory.setItem(slot, null);
        }

        if (allItems == null || allItems.isEmpty()) {
            addPageNavigation(Collections.emptyList());
            return;
        }

        // Caso especial: se a lista tem apenas um item (como o "nenhum item encontrado"), centraliza ele
        if (allItems.size() == 1 && itemSlots.size() > 1) {
            int middleSlotIndex = itemSlots.size() / 2;
            setItem(itemSlots.get(middleSlotIndex), allItems.get(0));
            addPageNavigation(allItems);
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
     * Adiciona os botões de "Próxima Página" e "Página Anterior" ao menu.
     * Este método deve ser sobrescrito pela subclasse para definir os slots corretos.
     * @param allItems A lista total de itens para calcular se há mais páginas.
     */
    protected abstract void addPageNavigation(List<GuiItem> allItems);

    /**
     * Helper para criar um ItemStack de forma rápida.
     */
    protected ItemStack createItem(Material material, String name, List<String> lore) {
        return new ItemBuilder(material).setName(name).setLore(lore).hideFlags().build();
    }
}