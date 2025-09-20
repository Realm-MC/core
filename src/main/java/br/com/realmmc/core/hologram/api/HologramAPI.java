package br.com.realmmc.core.hologram.api;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.hologram.Hologram;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;

public final class HologramAPI {

    private HologramAPI() {}

    /**
     * Cria um novo holograma.
     * @param id O ID único para o holograma.
     * @param location A localização do holograma.
     * @param lines As linhas de texto.
     * @param persistent Se o holograma deve ser guardado no banco de dados e sobreviver a reinicializações.
     * @return A instância do holograma criado.
     */
    public static Hologram create(String id, Location location, List<String> lines, boolean persistent) {
        return CoreAPI.getInstance().getHologramManager().create(id, location, lines, persistent);
    }

    public static void delete(String id) {
        CoreAPI.getInstance().getHologramManager().delete(id);
    }

    public static Optional<Hologram> get(String id) {
        return CoreAPI.getInstance().getHologramManager().getHologram(id);
    }
}