package br.com.realmmc.core.hologram;

import br.com.realmmc.core.api.CoreAPI;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Representa uma instância de um holograma, contendo suas propriedades e estado.
 */
public class Hologram {

    private final String id;
    private Location location;
    private List<String> lines;
    private final Set<UUID> viewers;
    private final Map<Integer, UUID> lineEntityIds; // Agora guarda UUID das entidades (TextDisplay)
    private boolean isGlobal;

    public Hologram(String id, Location location, List<String> lines) {
        this.id = id;
        this.location = location;
        this.lines = new CopyOnWriteArrayList<>(lines);
        this.viewers = ConcurrentHashMap.newKeySet();
        this.lineEntityIds = new ConcurrentHashMap<>();
        this.isGlobal = false;
    }

    public Hologram showTo(Player player) {
        CoreAPI.getInstance().getHologramManager().showTo(player, this);
        return this;
    }

    public Hologram showToAll() {
        CoreAPI.getInstance().getHologramManager().showToAll(this);
        return this;
    }

    public Hologram hideFrom(Player player) {
        CoreAPI.getInstance().getHologramManager().hideFrom(player, this);
        return this;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = new CopyOnWriteArrayList<>(lines);
    }

    public Set<UUID> getViewers() {
        return viewers;
    }

    public Map<Integer, UUID> getLineEntityIds() {
        return lineEntityIds;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(boolean global) {
        isGlobal = global;
    }

    public void addViewer(UUID uuid) {
        this.viewers.add(uuid);
    }

    public void removeViewer(UUID uuid) {
        this.viewers.remove(uuid);
    }

    public boolean isViewer(UUID uuid) {
        return isGlobal || viewers.contains(uuid);
    }
}
