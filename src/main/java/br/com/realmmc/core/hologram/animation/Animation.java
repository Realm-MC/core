package br.com.realmmc.core.hologram.animation;

import br.com.realmmc.core.hologram.Hologram;
import org.bukkit.entity.Player;

public interface Animation {
    void tick(Player player, int entityId, Hologram hologram);
}