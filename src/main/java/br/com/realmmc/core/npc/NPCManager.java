package br.com.realmmc.core.npc;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.npc.actions.ActionRegistry;
import br.com.realmmc.core.npc.util.MineskinClient;
import br.com.realmmc.core.utils.ColorAPI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.SkinStorage;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class NPCManager {

    private final Main plugin;
    private final Map<String, NPC> npcCache = new ConcurrentHashMap<>();
    private final NPCRegistry npcRegistry;
    private final ActionRegistry actionRegistry;
    private final MongoCollection<Document> collection;
    private final MongoCollection<Document> clickDataCollection;
    private BukkitTask syncTask;

    public NPCManager(Main plugin) {
        this.plugin = plugin;
        this.npcRegistry = CitizensAPI.getNPCRegistry();
        this.actionRegistry = new ActionRegistry();
        this.collection = plugin.getDatabaseManager().getDatabase().getCollection("npcs");
        this.clickDataCollection = plugin.getDatabaseManager().getDatabase().getCollection("npc_click_data");
    }

    // ===================================================== //
    //      CARREGAMENTO E RECONCILIAÇÃO DE NPCs            //
    // ===================================================== //
    public void loadAndSpawnAll() {
        collection.find().forEach(doc -> {
            NPC npc = NPC.fromDocument(doc);
            npcCache.put(npc.getId().toLowerCase(), npc);
        });

        Set<String> managedIds = npcCache.keySet();
        List<net.citizensnpcs.api.npc.NPC> toDestroy = new ArrayList<>();

        for (net.citizensnpcs.api.npc.NPC citizensNpc : npcRegistry) {
            if (citizensNpc.data().has("npc-id")) {
                String id = citizensNpc.data().get("npc-id").toString();
                if (!managedIds.contains(id.toLowerCase())) {
                    toDestroy.add(citizensNpc);
                }
            }
        }

        if (!toDestroy.isEmpty()) {
            plugin.getLogger().info("Limpando " + toDestroy.size() + " NPC(s) órfão(s) do Citizens.");
            toDestroy.forEach(net.citizensnpcs.api.npc.NPC::destroy);
        }

        for (NPC definition : npcCache.values()) {
            if (definition.getServer() != null && plugin.getServerName().equalsIgnoreCase(definition.getServer())) {
                spawnOrUpdateNPC(definition);
            }
        }
        startSyncTask();
    }

    public net.citizensnpcs.api.npc.NPC findNpc(String id) {
        for (net.citizensnpcs.api.npc.NPC npc : npcRegistry) {
            Object data = npc.data().get("npc-id");
            if (data != null && id.equalsIgnoreCase(data.toString())) {
                return npc;
            }
        }
        return null;
    }

    public void spawnOrUpdateNPC(NPC definition) {
        net.citizensnpcs.api.npc.NPC npc = findNpc(definition.getId());

        if (npc == null) {
            if (definition.getLocation() == null || definition.getLocation().getWorld() == null) {
                plugin.getLogger().warning("NPC '" + definition.getId() + "' não pode ser criado pois sua localização é inválida.");
                return;
            }
            npc = npcRegistry.createNPC(EntityType.PLAYER, ColorAPI.format(definition.getDisplayName()));
            npc.spawn(definition.getLocation());
        } else {
            if (definition.getLocation() != null && !npc.getStoredLocation().equals(definition.getLocation())) {
                npc.teleport(definition.getLocation(), org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
            npc.setName(ColorAPI.format(definition.getDisplayName()));
        }

        npc.data().set(net.citizensnpcs.api.npc.NPC.Metadata.NAMEPLATE_VISIBLE, false);
        npc.data().set("npc-id", definition.getId().toLowerCase());
        npc.setAlwaysUseNameHologram(false);
        applySkin(npc, definition);
    }

    private net.citizensnpcs.api.npc.NPC findNpcById(String id) {
        for (net.citizensnpcs.api.npc.NPC npc : npcRegistry) {
            if (id.equalsIgnoreCase(npc.data().get("npc-id"))) {
                return npc;
            }
        }
        return null;
    }

    public CompletableFuture<Void> deleteNpc(String id) {
        return CompletableFuture.runAsync(() -> {
            net.citizensnpcs.api.npc.NPC npcToDestroy = findNpcById(id);

            CoreAPI.getInstance().getHologramManager().deleteHologram("npc_" + id.toLowerCase());
            collection.deleteOne(Filters.eq("_id", id));
            npcCache.remove(id.toLowerCase());

            if (npcToDestroy != null) {
                Bukkit.getScheduler().runTask(plugin, () -> npcToDestroy.destroy()); // fix destroy()
            }
        });
    }

    public CompletableFuture<Integer> syncFromFile() {
        File configFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!configFile.exists()) return CompletableFuture.completedFuture(0);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = config.getConfigurationSection("npcs");
        if (section == null) return CompletableFuture.completedFuture(0);

        MineskinClient mineskin = new MineskinClient();
        AtomicInteger count = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String id : section.getKeys(false)) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                String skinUrl = section.getString("npcs." + id + ".skin-url");
                NPCSkin skin = mineskin.getSkinFromUrl(skinUrl).join();
                String displayName = section.getString("npcs." + id + ".display-name");
                String server = section.getString("npcs." + id + ".server");
                String actionType = section.getString("npcs." + id + ".action.type", "NONE");
                List<String> actionValues = section.getStringList("npcs." + id + ".action.value");
                Location location = section.getLocation("npcs." + id + ".location");
                NPC.ClickAlert clickAlert = null;
                if (section.isConfigurationSection("npcs." + id + ".click-alert")) {
                    String mode = section.getString("npcs." + id + ".click-alert.mode", "NEVER");
                    String text = section.getString("npcs." + id + ".click-alert.text", "");
                    clickAlert = new NPC.ClickAlert(mode, text);
                }
                NPC npc = new NPC(id, displayName, location,
                        skin != null ? skin.value() : null,
                        skin != null ? skin.signature() : null,
                        skinUrl, actionType, actionValues, server, clickAlert);
                saveNpc(npc).join();
                npcCache.put(id.toLowerCase(), npc);
                if (plugin.getServerName().equalsIgnoreCase(server)) {
                    Bukkit.getScheduler().runTask(plugin, () -> spawnOrUpdateNPC(npc));
                }
                count.incrementAndGet();
            });
            futures.add(future);
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> count.get());
    }

    private void startSyncTask() {
        if (syncTask != null) syncTask.cancel();
        syncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (net.citizensnpcs.api.npc.NPC npc : npcRegistry) {
                if (npc.isSpawned() && npc.data().has("npc-id")) {
                    String id = npc.data().get("npc-id").toString();
                    CoreAPI.getInstance().getHologramManager().getHologram("npc_" + id.toLowerCase()).ifPresent(hologram -> {
                        Location newHoloLocation = npc.getStoredLocation().clone().add(0, 2.1, 0);
                        hologram.setBaseLocation(newHoloLocation);
                    });
                }
            }
        }, 40L, 10L);
    }

    public void shutdown() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
    }

    public CompletableFuture<Void> createNpc(String id, String displayName, Location location) {
        NPC npc = new NPC(id, displayName, location, null, null, null,
                "NONE", Collections.emptyList(), plugin.getServerName(), null);
        return saveNpc(npc).thenRun(() -> {
            npcCache.put(id.toLowerCase(), npc);
            spawnOrUpdateNPC(npc);
        });
    }

    public CompletableFuture<Void> saveNpc(NPC npc) {
        return CompletableFuture.runAsync(() ->
                collection.replaceOne(Filters.eq("_id", npc.getId()), npc.toDocument(), new ReplaceOptions().upsert(true))
        );
    }

    public void applySkin(net.citizensnpcs.api.npc.NPC npc, NPC definition) {
        if (!npc.hasTrait(SkinTrait.class)) npc.addTrait(SkinTrait.class);
        SkinTrait skinTrait = npc.getTrait(SkinTrait.class);
        if (definition.getSkinValue() != null && definition.getSkinSignature() != null) {
            skinTrait.setSkinPersistent(definition.getId(), definition.getSkinSignature(), definition.getSkinValue());
            return;
        }
        if (definition.getSkinUrl() != null && !definition.getSkinUrl().isEmpty()) {
            MineskinClient mineskin = new MineskinClient();
            mineskin.getSkinFromUrl(definition.getSkinUrl()).thenAccept(skinData -> {
                if (skinData != null) {
                    definition.setSkin(skinData.value(), skinData.signature());
                    saveNpc(definition);
                    Bukkit.getScheduler().runTask(plugin, () ->
                            skinTrait.setSkinPersistent(definition.getId(), skinData.signature(), skinData.value()));
                }
            });
        }
    }

    public CompletableFuture<Boolean> updateSkinFromPlayer(String npcId, String playerName) {
        SkinsRestorer skinsRestorer = plugin.getSkinsRestorerApi();
        if (skinsRestorer == null) return CompletableFuture.completedFuture(false);
        return CompletableFuture.supplyAsync(() -> {
            try {
                SkinStorage skinStorage = skinsRestorer.getSkinStorage();
                Optional<InputDataResult> resultOpt = skinStorage.findOrCreateSkinData(playerName, null);
                if (resultOpt.isEmpty()) return false;
                InputDataResult result = resultOpt.get();
                SkinProperty skin = result.getProperty();
                if (skin == null) return false;
                NPC npcDef = npcCache.get(npcId.toLowerCase());
                if (npcDef == null) return false;
                npcDef.setSkin(skin.getValue(), skin.getSignature());
                saveNpc(npcDef).join();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    net.citizensnpcs.api.npc.NPC npc = findNpc(npcId);
                    if (npc != null) applySkin(npc, npcDef);
                });
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao buscar a skin do jogador via SkinsRestorer: " + playerName, e);
                return false;
            }
        });
    }

    public void updateClickAlert(String npcId, String mode, String text) {
        getNpc(npcId).ifPresent(npc -> {
            NPC.ClickAlert newAlert = new NPC.ClickAlert(mode.toUpperCase(), text);
            npc.setClickAlert(newAlert);
            saveNpc(npc);
        });
    }

    public CompletableFuture<Integer> getClickCount(String npcId, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = clickDataCollection.find(Filters.and(
                    Filters.eq("npcId", npcId),
                    Filters.eq("playerId", playerId.toString())
            )).first();
            return doc == null ? 0 : doc.getInteger("clicks", 0);
        });
    }

    public CompletableFuture<Void> incrementClickCount(String npcId, Player player) {
        return CompletableFuture.runAsync(() -> {
            Bson filter = Filters.and(
                    Filters.eq("npcId", npcId),
                    Filters.eq("playerId", player.getUniqueId().toString())
            );
            Bson update = Updates.combine(
                    Updates.inc("clicks", 1),
                    Updates.setOnInsert("playerName", player.getName())
            );
            clickDataCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
        });
    }

    public Optional<NPC> getNpc(String id) {
        return Optional.ofNullable(npcCache.get(id.toLowerCase()));
    }

    public Collection<NPC> getAllNpcs() {
        return npcCache.values();
    }

    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }
}
