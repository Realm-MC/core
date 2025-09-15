package br.com.realmmc.core.npc.api;

import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.npc.NPC;
import org.bukkit.Location;
import java.util.Optional;

/**
 * Ponto de acesso estático e simplificado para o framework de NPCs.
 */
public final class NPCAPI {

    private NPCAPI() {}

    public static void create(String id, String name, Location location, String skinUsername) {
        CoreAPI.getInstance().getNpcManager().create(id, name, location, skinUsername);
    }

    public static void delete(String id) {
        CoreAPI.getInstance().getNpcManager().delete(id);
    }

    public static Optional<NPC> get(String id) {
        return CoreAPI.getInstance().getNpcManager().getNpc(id);
    }
}