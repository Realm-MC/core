package br.com.realmmc.core.hologram.api;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.hologram.Hologram;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;

/**
 * Ponto de acesso estático e simplificado para o framework de Hologramas.
 */
public final class HologramAPI {

    private HologramAPI() {}

    public static Hologram create(String id, Location location, List<String> lines) {
        return CoreAPI.getInstance().getHologramManager().create(id, location, lines);
    }

    public static void delete(String id) {
        CoreAPI.getInstance().getHologramManager().delete(id);
    }

    public static Optional<Hologram> get(String id) {
        return CoreAPI.getInstance().getHologramManager().getHologram(id);
    }
}