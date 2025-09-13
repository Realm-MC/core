package br.com.realmmc.core.npc;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.npc.actions.ActionRegistry;
import br.com.realmmc.core.npc.util.MineskinClient;
import br.com.realmmc.core.utils.ColorAPI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.SkinStorage;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class NPCManager {

    private final Main plugin;
    private final Map<String, NPC> npcCache = new ConcurrentHashMap<>();
    private final NPCRegistry npcRegistry;
    private final ActionRegistry actionRegistry;
    private final MongoCollection<Document> collection;
    private final MongoCollection<Document> clickDataCollection;
    private BukkitTask syncTask;

    private final Map<UUID, Set<String>> playerClicksCache = new ConcurrentHashMap<>();

    public NPCManager(Main plugin) {
        this.plugin = plugin;
        this.npcRegistry = CitizensAPI.getNPCRegistry();
        this.actionRegistry = new ActionRegistry();
        this.collection = plugin.getDatabaseManager().getDatabase().getCollection("npcs");
        this.clickDataCollection = plugin.getDatabaseManager().getDatabase().getCollection("npc_click_data");
    }

    private void spawnFromDefinition(NPC definition) {
        net.citizensnpcs.api.npc.NPC npc = npcRegistry.createNPC(EntityType.PLAYER, ColorAPI.format(definition.getDisplayName()));
        configureNpc(npc, definition);
        npc.spawn(definition.getLocation());

        String hologramIdDefault = "npc_" + definition.getId().toLowerCase() + "_default";
        String hologramIdAlert = "npc_" + definition.getId().toLowerCase() + "_alert";
        Location hologramLocation = npc.getStoredLocation().clone().add(0, 2.3, 0);

        if (DHAPI.getHologram(hologramIdDefault) != null) DHAPI.removeHologram(hologramIdDefault);
        if (DHAPI.getHologram(hologramIdAlert) != null) DHAPI.removeHologram(hologramIdAlert);

        Hologram holoDefault = DHAPI.createHologram(hologramIdDefault, hologramLocation, List.of(definition.getDisplayName()));
        if (holoDefault != null) {
            holoDefault.setDefaultVisibleState(true);
        }

        definition.getClickAlert().ifPresent(alert -> {
            Location alertLocation = hologramLocation.clone().add(0, 0.3, 0);
            Hologram holoAlert = DHAPI.createHologram(hologramIdAlert, alertLocation, List.of(alert.text()));
            if (holoAlert != null) {
                holoAlert.setDefaultVisibleState(true);
            }
        });
    }

    public void hideAlertIfClicked(Player player, NPC npcDefinition) {
        if (npcDefinition.getClickAlert().isEmpty()) return;

        String mode = npcDefinition.getClickAlert().get().mode();
        boolean hasClicked = hasClicked(player.getUniqueId(), npcDefinition.getId());

        if ("FIRST".equalsIgnoreCase(mode) && hasClicked) {
            String hologramIdAlert = "npc_" + npcDefinition.getId().toLowerCase() + "_alert";
            Hologram holoAlert = DHAPI.getHologram(hologramIdAlert);
            if (holoAlert != null) {
                holoAlert.hide(player);
            }
        }
    }

    public void shutdown() {
        if (syncTask != null) syncTask.cancel();
    }

    public void handlePlayerQuit(Player player) {
        playerClicksCache.remove(player.getUniqueId());
    }

    public void loadAndSpawnAll() {
        npcCache.clear();
        collection.find().forEach(doc -> {
            NPC npc = NPC.fromDocument(doc);
            npcCache.put(npc.getId().toLowerCase(), npc);
        });

        Set<String> managedIdsOnThisServer = npcCache.values().stream()
                .filter(def -> plugin.getServerName().equalsIgnoreCase(def.getServer()))
                .map(def -> def.getId().toLowerCase())
                .collect(Collectors.toSet());

        List<net.citizensnpcs.api.npc.NPC> toDestroy = new ArrayList<>();
        for (net.citizensnpcs.api.npc.NPC npc : this.npcRegistry) {
            if (npc.data().has("npc-id")) {
                toDestroy.add(npc);
            }
        }
        if (!toDestroy.isEmpty()) {
            toDestroy.forEach(npc -> npc.destroy());
        }

        for (String id : managedIdsOnThisServer) {
            NPC definition = npcCache.get(id);
            if (definition != null && definition.getLocation() != null && definition.getLocation().getWorld() != null) {
                spawnFromDefinition(definition);
            }
        }
        startSyncTask();
    }

    private void configureNpc(net.citizensnpcs.api.npc.NPC npc, NPC definition) {
        npc.data().set("npc-id", definition.getId().toLowerCase());
        npc.data().set(net.citizensnpcs.api.npc.NPC.Metadata.NAMEPLATE_VISIBLE, false);
        npc.setAlwaysUseNameHologram(false);
        npc.data().set(net.citizensnpcs.api.npc.NPC.Metadata.SHOULD_SAVE, false);
        npc.setName(ColorAPI.format(definition.getDisplayName()));

        if (!npc.hasTrait(LookClose.class)) npc.addTrait(LookClose.class);
        LookClose lookCloseTrait = npc.getTrait(LookClose.class);
        lookCloseTrait.lookClose(definition.isLookAtPlayer());
        lookCloseTrait.setRange(8);

        if (npc.getStoredLocation() == null || !npc.getStoredLocation().equals(definition.getLocation())) {
            npc.teleport(definition.getLocation(), org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
        applySkin(npc, definition);
    }

    private void startSyncTask() {
        if (syncTask != null) syncTask.cancel();
        syncTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (net.citizensnpcs.api.npc.NPC npc : npcRegistry) {
                if (npc.isSpawned() && npc.data().has("npc-id")) {
                    String id = npc.data().get("npc-id");
                    Location newHoloLocation = npc.getStoredLocation().clone().add(0, 2.3, 0);

                    Hologram holoDefault = DHAPI.getHologram("npc_" + id.toLowerCase() + "_default");
                    if (holoDefault != null) DHAPI.moveHologram(holoDefault, newHoloLocation);

                    Hologram holoAlert = DHAPI.getHologram("npc_" + id.toLowerCase() + "_alert");
                    if (holoAlert != null) DHAPI.moveHologram(holoAlert, newHoloLocation.clone().add(0, 0.3, 0));
                }
            }
        }, 40L, 5L);
    }

    public boolean hasClicked(UUID playerId, String npcId) {
        return playerClicksCache.getOrDefault(playerId, Collections.emptySet()).contains(npcId.toLowerCase());
    }

    public void loadPlayerClicks(Player player) {
        try {
            Set<String> cacheSet = playerClicksCache.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
            clickDataCollection.find(Filters.eq("playerId", player.getUniqueId().toString())).forEach(doc -> {
                cacheSet.add(doc.getString("npcId").toLowerCase());
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Falha ao carregar dados de clique para " + player.getName(), e);
            playerClicksCache.putIfAbsent(player.getUniqueId(), ConcurrentHashMap.newKeySet());
        }
    }

    public void incrementClickCount(String npcId, Player player) {
        playerClicksCache.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(npcId.toLowerCase());
        CompletableFuture.runAsync(() -> {
            Bson filter = Filters.and(Filters.eq("npcId", npcId), Filters.eq("playerId", player.getUniqueId().toString()));
            Bson update = Updates.combine(Updates.inc("clicks", 1), Updates.setOnInsert("playerName", player.getName()));
            clickDataCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
        });
    }

    // ===================================================================================== //
    //                                MÉTODOS RESTAURADOS AQUI                               //
    // ===================================================================================== //

    public CompletableFuture<Void> saveNpc(NPC npc) {
        return CompletableFuture.runAsync(() -> collection.replaceOne(Filters.eq("_id", npc.getId()), npc.toDocument(), new ReplaceOptions().upsert(true)));
    }

    public void applySkin(net.citizensnpcs.api.npc.NPC npc, NPC definition) {
        if (!npc.hasTrait(SkinTrait.class)) npc.addTrait(SkinTrait.class);
        SkinTrait skinTrait = npc.getTrait(SkinTrait.class);
        if (definition.getSkinValue() != null && definition.getSkinSignature() != null) {
            skinTrait.setSkinPersistent(definition.getId(), definition.getSkinSignature(), definition.getSkinValue());
            return;
        }
        if (definition.getSkinUrl() != null && !definition.getSkinUrl().isEmpty()) {
            new MineskinClient().getSkinFromUrl(definition.getSkinUrl()).thenAccept(skinData -> {
                if (skinData != null) {
                    definition.setSkin(skinData.value(), skinData.signature());
                    saveNpc(definition); // Esta chamada precisa do método saveNpc
                    Bukkit.getScheduler().runTask(plugin, () -> skinTrait.setSkinPersistent(definition.getId(), skinData.signature(), skinData.value()));
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
                    if (npc != null) {
                        applySkin(npc, npcDef);
                    }
                });
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ocorreu um erro ao buscar a skin do jogador via SkinsRestorer: " + playerName, e);
                return false;
            }
        });
    }

    public CompletableFuture<Void> createNpc(String id, String displayName, Location location) {
        NPC npcDefinition = new NPC(id, displayName, location, null, null, null, "NONE", Collections.emptyList(), plugin.getServerName(), null, true);
        return saveNpc(npcDefinition).thenRun(() -> {
            npcCache.put(id.toLowerCase(), npcDefinition);
            spawnFromDefinition(npcDefinition);
        });
    }

    public CompletableFuture<Void> deleteNpc(String id) {
        return CompletableFuture.runAsync(() -> {
            net.citizensnpcs.api.npc.NPC npcToDestroy = findNpc(id);
            if (DHAPI.getHologram("npc_" + id.toLowerCase() + "_default") != null) DHAPI.removeHologram("npc_" + id.toLowerCase() + "_default");
            if (DHAPI.getHologram("npc_" + id.toLowerCase() + "_alert") != null) DHAPI.removeHologram("npc_" + id.toLowerCase() + "_alert");
            collection.deleteOne(Filters.eq("_id", id));
            npcCache.remove(id.toLowerCase());
            if (npcToDestroy != null) {
                Bukkit.getScheduler().runTask(plugin, () -> npcToDestroy.destroy());
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

    public net.citizensnpcs.api.npc.NPC findNpc(String id) {
        for (net.citizensnpcs.api.npc.NPC npc : npcRegistry) {
            if (id.equalsIgnoreCase(npc.data().get("npc-id"))) {
                return npc;
            }
        }
        return null;
    }

    public void reconfigureNpc(String id) {
        getNpc(id).ifPresent(definition -> {
            net.citizensnpcs.api.npc.NPC citizensNpc = findNpc(id);
            if (citizensNpc != null) {
                configureNpc(citizensNpc, definition);
            }
        });
    }

    public Optional<NPC> getNpc(String id) {
        return Optional.ofNullable(npcCache.get(id.toLowerCase()));
    }

    public Collection<NPC> getAllNpcs() {
        return Collections.unmodifiableCollection(npcCache.values());
    }

    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }
}