package br.com.realmmc.core.hologram.line;

import br.com.realmmc.core.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public interface HologramLine {
    void spawn(Player player, Location location);
    void update(Player player);
    void despawn(Player player);
    double getHeight();
    List<Integer> getEntityIds(Player player);
    void setHologram(Hologram hologram);
}