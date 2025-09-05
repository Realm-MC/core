package br.com.realmmc.core.hologram.line;

import br.com.realmmc.core.hologram.Hologram;
import br.com.realmmc.core.hologram.packets.HologramPacketController;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TextLine implements HologramLine {

    private final String text;
    private final ChatColor backgroundColor;
    private final Map<UUID, Integer> entityIdMap = new ConcurrentHashMap<>();
    private Hologram hologram;

    private TextLine(String text, ChatColor backgroundColor) {
        this.text = text;
        this.backgroundColor = backgroundColor;
    }

    @Override
    public void spawn(Player player, Location location) {
        int entityId = HologramPacketController.spawnTextDisplay(player, location, text, backgroundColor);
        entityIdMap.put(player.getUniqueId(), entityId);
    }

    @Override
    public void update(Player player) {
        if (entityIdMap.containsKey(player.getUniqueId())) {
            HologramPacketController.updateTextDisplay(player, entityIdMap.get(player.getUniqueId()), text, backgroundColor);
        }
    }

    @Override
    public void despawn(Player player) {
        if (entityIdMap.containsKey(player.getUniqueId())) {
            HologramPacketController.destroyEntities(player, List.of(entityIdMap.get(player.getUniqueId())));
            entityIdMap.remove(player.getUniqueId());
        }
    }

    @Override
    public double getHeight() {
        return 0.3;
    }

    @Override
    public List<Integer> getEntityIds(Player player) {
        return List.of(entityIdMap.get(player.getUniqueId()));
    }

    @Override
    public void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }

    // Builder para facilitar a criação
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String text = "";
        private ChatColor backgroundColor = null;

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder backgroundColor(ChatColor backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public TextLine build() {
            return new TextLine(text, backgroundColor);
        }
    }
}