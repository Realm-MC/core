package br.com.realmmc.core.hologram.line;

import br.com.realmmc.core.hologram.Hologram;
import br.com.realmmc.core.hologram.animation.Animation;
import br.com.realmmc.core.hologram.packets.HologramPacketController;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemLine implements HologramLine {

    private final ItemStack itemStack;
    private final Map<UUID, Integer> entityIdMap = new ConcurrentHashMap<>();
    private Animation animation;
    private Hologram hologram;

    private ItemLine(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void spawn(Player player, Location location) {
        int entityId = HologramPacketController.spawnItemDisplay(player, location, itemStack);
        entityIdMap.put(player.getUniqueId(), entityId);
    }

    @Override
    public void update(Player player) {
        // A atualização de itens é mais complexa e geralmente não é necessária
        // A animação cuidará da atualização da posição/rotação
    }

    @Override
    public void despawn(Player player) {
        if (entityIdMap.containsKey(player.getUniqueId())) {
            HologramPacketController.destroyEntities(player, List.of(entityIdMap.get(player.getUniqueId())));
            entityIdMap.remove(player.getUniqueId());
        }
    }

    public void runAnimationTick(Player player) {
        if (animation != null && entityIdMap.containsKey(player.getUniqueId())) {
            animation.tick(player, entityIdMap.get(player.getUniqueId()), hologram);
        }
    }

    @Override
    public double getHeight() {
        return 0.5; // Itens geralmente ocupam mais espaço vertical
    }

    @Override
    public List<Integer> getEntityIds(Player player) {
        return List.of(entityIdMap.get(player.getUniqueId()));
    }

    @Override
    public void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }

    public void setAnimation(Animation animation) {
        this.animation = animation;
    }

    public boolean hasAnimation() {
        return this.animation != null;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ItemStack itemStack;
        private Animation animation;

        public Builder item(ItemStack itemStack) {
            this.itemStack = itemStack;
            return this;
        }

        public Builder animation(Animation animation) {
            this.animation = animation;
            return this;
        }

        public ItemLine build() {
            ItemLine itemLine = new ItemLine(itemStack);
            if (animation != null) {
                itemLine.setAnimation(animation);
            }
            return itemLine;
        }
    }
}