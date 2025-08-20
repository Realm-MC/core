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
import org.bukkit.Bukkit;
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
        loadAndSpawnAll();
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
                        id, config.getString(path + ".display_name"),
                        config.getString(path + ".skin_owner"), config.getString(path + ".skin_url"),
                        config.getString(path + ".skin_texture"), config.getString(path + ".skin_signature"),
                        config.getString(path + ".world"), config.getString(path + ".server"),
                        config.getStringList(path + ".hologram_lines"),
                        ActionType.valueOf(config.getString(path + ".action_type", "NONE").toUpperCase()),
                        config.getStringList(path + ".action_value")
                );
                npcDefinitions.put(id.toLowerCase(), npcDef);

                if (config.isSet(path + ".location")) {
                    Location loc = config.getLocation(path + ".location");
                    if (loc != null && Bukkit.getWorld(npcDef.getWorld()) != null) {
                        spawnOrUpdateNPC(npcDef, loc);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Falha ao carregar NPC com ID: " + id, e);
            }
        }
        plugin.getLogger().info(npcDefinitions.size() + " definições de NPCs carregadas e spawnadas.");
    }

    private void spawnOrUpdateNPC(NPC definition, Location location) {
        net.citizensnpcs.api.npc.NPC npc = getSpawnedNpc(definition.getId());

        if (npc == null) {
            String cleanName = definition.getDisplayName().replaceAll("§.", "");
            npc = npcRegistry.createNPC(EntityType.PLAYER, cleanName);
            npc.data().set("npc-id", definition.getId().toLowerCase());
        }

        npc.setName(ColorAPI.format(definition.getDisplayName()));
        npc.setAlwaysUseNameHologram(true);
        applySkin(npc, definition);

        if (!npc.isSpawned() || !npc.getStoredLocation().equals(location)) {
            npc.spawn(location);
        }

        createOrUpdateHologram(npc);
    }

    public void setLocationAndSpawn(String id, Location location) {
        config.set("npcs." + id.toLowerCase() + ".location", location);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar a localização do NPC " + id, e);
        }
        loadAndSpawnAll();
    }

    public void deleteAndDespawn(String id) {
        net.citizensnpcs.api.npc.NPC npc = getSpawnedNpc(id);
        if (npc != null) {
            CoreAPI.getInstance().getHologramManager().deleteHologram("npc_" + id.toLowerCase());
            npc.destroy();
        }

        config.set("npcs." + id.toLowerCase() + ".location", null);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar o npcs.yml ao deletar o NPC " + id, e);
        }
        loadAndSpawnAll();
    }

    public NPC getDefinition(String id) {
        return npcDefinitions.get(id.toLowerCase());
    }

    public Set<String> getDefinedIds() {
        return npcDefinitions.keySet();
    }

    public net.citizensnpcs.api.npc.NPC getSpawnedNpc(String id) {
        for (net.citizensnpcs.api.npc.NPC npc : npcRegistry) {
            if (npc.data().has("npc-id") && npc.data().get("npc-id").equals(id.toLowerCase())) {
                return npc;
            }
        }
        return null;
    }

    public boolean isSpawned(String id) {
        return getSpawnedNpc(id) != null;
    }

    private void applySkin(net.citizensnpcs.api.npc.NPC npc, NPC definition) {
        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
        String npcIdForSkin = "npc-" + definition.getId();

        if (definition.getSkinOwner() != null && !definition.getSkinOwner().isEmpty()) {
            skinTrait.setSkinName(definition.getSkinOwner(), true);
        } else if (definition.getSkinUrl() != null && !definition.getSkinUrl().isEmpty()) {
            MineskinClient.getSkinFromUrl(definition.getSkinUrl()).thenAccept(skinData -> {
                if (skinData != null) {
                    skinTrait.setSkinPersistent(npcIdForSkin, skinData.signature(), skinData.texture());
                }
            });
        } else if (definition.getSkinTexture() != null && !definition.getSkinTexture().isEmpty()) {
            skinTrait.setSkinPersistent(npcIdForSkin, definition.getSkinSignature(), definition.getSkinTexture());
        }
    }

    public void createOrUpdateHologram(net.citizensnpcs.api.npc.NPC npc) {
        if (npc == null || !npc.isSpawned()) return;

        String id = npc.data().get("npc-id");
        NPC definition = getDefinition(id);
        if (definition == null || definition.getHologramLines().isEmpty()) return;

        Location hologramLocation = npc.getStoredLocation().clone().add(0, 1.70, 0);
        String hologramId = "npc_" + id.toLowerCase();

        CoreAPI.getInstance().getHologramManager().getHologram(hologramId).ifPresent(Hologram::despawn);

        Hologram newHologram = new Hologram(hologramId, hologramLocation, definition.getHologramLines());
        newHologram.spawn();
    }
}