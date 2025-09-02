package br.com.realmmc.core.npc;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.hologram.Hologram;
import br.com.realmmc.core.npc.actions.ActionType;
import br.com.realmmc.core.npc.util.MineskinClient;
import br.com.realmmc.core.utils.ColorAPI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.StreamSupport;

public class NPCManager {

    private final Main plugin;
    private final Map<String, NPC> npcDefinitions = new HashMap<>();
    private final NPCRegistry npcRegistry;
    private final File configFile;
    private FileConfiguration config;

    public NPCManager(Main plugin) {
        this.plugin = plugin;
        this.npcRegistry = CitizensAPI.getNPCRegistry();
        this.configFile = new File(plugin.getDataFolder(), "npcs.yml");
        // A chamada loadAndSpawnAll() deve ser feita de forma atrasada na Main.java
    }

    public void loadAndSpawnAll() {
        if (!configFile.exists()) {
            plugin.saveResource("npcs.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        npcDefinitions.clear();

        ConfigurationSection section = config.getConfigurationSection("npcs");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String path = "npcs." + id;
            try {
                NPC npcDef = new NPC(
                        id,
                        section.getString(id + ".display-name"),
                        section.getString(id + ".world"),
                        section.getString(id + ".server"),
                        section.getString(id + ".skin-url"),
                        ActionType.valueOf(section.getString(id + ".action.type")),
                        section.getStringList(id + ".action.value")
                );
                npcDefinitions.put(id.toLowerCase(), npcDef);

                // ---> INÍCIO DA CORREÇÃO <---
                // Compara o nome do servidor atual com o nome do servidor definido para o NPC.
                // Se não forem iguais, ele simplesmente pula para o próximo NPC na lista.
                if (!plugin.getServerName().equalsIgnoreCase(npcDef.getServer())) {
                    continue; // Pula este NPC, pois ele não pertence a este servidor.
                }
                // ---> FIM DA CORREÇÃO <---

                if (config.isSet(path + ".location")) {
                    Location loc = config.getLocation(path + ".location");
                    if (loc != null && loc.getWorld() != null) {
                        spawnOrUpdateNPC(npcDef, loc);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao carregar a definição do NPC '" + id + "': " + e.getMessage());
            }
        }
    }

    private void spawnOrUpdateNPC(NPC definition, Location location) {
        net.citizensnpcs.api.npc.NPC existingNpc = getSpawnedNpc(definition.getId());
        if (existingNpc != null) {
            if (!existingNpc.isSpawned() || !existingNpc.getStoredLocation().equals(location)) {
                existingNpc.teleport(location, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
            if (!existingNpc.getName().equals(ColorAPI.format(definition.getDisplayName()))) {
                existingNpc.setName(ColorAPI.format(definition.getDisplayName()));
            }
            createOrUpdateHologram(existingNpc);
            plugin.getLogger().info("NPC '" + definition.getId() + "' já existia, foi atualizado.");
            return;
        }

        String cleanName = definition.getDisplayName().replaceAll("§.", "");
        net.citizensnpcs.api.npc.NPC npc = npcRegistry.createNPC(EntityType.PLAYER, cleanName);
        npc.data().set("npc-id", definition.getId().toLowerCase());

        npc.setName(ColorAPI.format(definition.getDisplayName()));
        npc.setAlwaysUseNameHologram(false);
        applySkin(npc, definition);

        npc.spawn(location);
        createOrUpdateHologram(npc);
        plugin.getLogger().info("NPC '" + definition.getId() + "' criado com sucesso.");
    }

    private void createOrUpdateHologram(net.citizensnpcs.api.npc.NPC npc) {
        String id = npc.data().get("npc-id");
        if (id == null) return;

        NPC definition = npcDefinitions.get(id.toLowerCase());
        if (definition == null || definition.getActionValue().isEmpty()) {
            CoreAPI.getInstance().getHologramManager().deleteHologram("npc_" + id.toLowerCase());
            return;
        }

        String hologramId = "npc_" + id.toLowerCase();
        Location hologramLocation = npc.getStoredLocation().clone().add(0, 2.2, 0);

        Hologram hologram = CoreAPI.getInstance().getHologramManager().getHologram(hologramId)
                .orElseGet(() -> CoreAPI.getInstance().getHologramManager().createHologram(hologramId, hologramLocation, definition.getActionValue()));

        hologram.setBaseLocation(hologramLocation);
        hologram.setLines(definition.getActionValue());
    }

    public void deleteAndDespawn(String id) {
        net.citizensnpcs.api.npc.NPC npc = getSpawnedNpc(id);
        if (npc != null) {
            CoreAPI.getInstance().getHologramManager().deleteHologram("npc_" + id.toLowerCase());
            npc.destroy();
        }
        npcDefinitions.remove(id.toLowerCase());
        config.set("npcs." + id, null);
        saveConfig();
    }

    private void applySkin(net.citizensnpcs.api.npc.NPC npc, NPC definition) {
        if (!npc.hasTrait(SkinTrait.class)) {
            npc.addTrait(SkinTrait.class);
        }
        SkinTrait skinTrait = npc.getTrait(SkinTrait.class);

        if (definition.getSkinUrl() != null && !definition.getSkinUrl().isEmpty()) {
            MineskinClient client = new MineskinClient();
            client.getSkinFromUrl(definition.getSkinUrl()).thenAccept(skin -> {
                if (skin != null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        skinTrait.setSkinPersistent(definition.getId(), skin.signature(), skin.value());
                    });
                } else {
                    plugin.getLogger().warning("Não foi possível gerar a skin para o NPC '" + definition.getId() + "' a partir da URL.");
                }
            });
        }
    }

    public void setLocationAndSpawn(String id, Location location) {
        NPC definition = npcDefinitions.get(id.toLowerCase());
        if (definition == null) {
            throw new IllegalArgumentException("NPC definition not found for ID: " + id);
        }
        config.set("npcs." + id + ".location", location);
        saveConfig();
        spawnOrUpdateNPC(definition, location);
    }

    public net.citizensnpcs.api.npc.NPC getSpawnedNpc(String id) {
        return StreamSupport.stream(npcRegistry.spliterator(), false)
                .filter(npc -> id.equalsIgnoreCase(npc.data().get("npc-id")))
                .findFirst()
                .orElse(null);
    }

    public boolean isSpawned(String id) {
        net.citizensnpcs.api.npc.NPC npc = getSpawnedNpc(id);
        return npc != null && npc.isSpawned();
    }

    public NPC getDefinition(String id) {
        return npcDefinitions.get(id.toLowerCase());
    }

    public Set<String> getDefinedIds() {
        return npcDefinitions.keySet();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar o arquivo npcs.yml", e);
        }
    }
}