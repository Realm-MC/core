package br.com.realmmc.core.npc;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.hologram.Hologram;
import br.com.realmmc.core.hologram.api.HologramAPI;
import br.com.realmmc.core.npc.actions.ActionRegistry;
import br.com.realmmc.core.npc.skin.SkinManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class NPCManager implements Listener {

    private final Main plugin;
    private final MongoCollection<Document> npcCollection;
    private final MongoCollection<Document> npcPlayerDataCollection;
    private final Map<String, NPC> activeNpcs = new ConcurrentHashMap<>();
    private final Map<Integer, String> entityIdToNpcId = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerClicksCache = new ConcurrentHashMap<>();
    private final AtomicInteger entityIdCounter = new AtomicInteger(Integer.MAX_VALUE);
    private final SkinManager skinManager;
    private final NPCPacketFactory packetFactory;
    private final ActionRegistry actionRegistry;
    private final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 500;
    private BukkitTask headTrackerTask;

    public NPCManager(Main plugin) {
        this.plugin = plugin;
        this.skinManager = new SkinManager();
        this.packetFactory = new NPCPacketFactory();
        this.actionRegistry = new ActionRegistry();
        this.npcCollection = CoreAPI.getInstance().getDatabaseManager().getDatabase().getCollection("npcs");
        this.npcPlayerDataCollection = CoreAPI.getInstance().getDatabaseManager().getDatabase().getCollection("npc_player_data");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        initPacketListener();
        startHeadTrackerTask();
        loadNpcs();
    }

    private void loadNpcs() {
        boolean isSkinsRestorerAvailable = plugin.getSkinsRestorerApi() != null;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Document doc : npcCollection.find()) {
                if (plugin.getServerName().equalsIgnoreCase(doc.getString("server"))) {
                    int entityId = entityIdCounter.decrementAndGet();
                    NPC npc = NPC.fromDocument(doc, entityId);

                    if (isSkinsRestorerAvailable && npc.getSkinUsername() != null && !npc.getSkinUsername().isEmpty()) {
                        CompletableFuture<Void> skinFuture = skinManager.getSkin(npc.getSkinUsername()).thenAccept(skinOpt -> {
                            skinOpt.ifPresent(skin -> npc.setSkin(skin, npc.getSkinUsername()));
                        });
                        futures.add(skinFuture);
                    }

                    if (npc.getLocation() != null && npc.getLocation().getWorld() != null) {
                        activeNpcs.put(npc.getId(), npc);
                        entityIdToNpcId.put(entityId, npc.getId());
                    }
                }
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                plugin.getLogger().info(activeNpcs.size() + " NPCs carregados do banco de dados para este servidor.");
            });
        });
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("Servidor completamente carregado. Exibindo " + activeNpcs.size() + " NPCs...");
            activeNpcs.values().forEach(this::showToAll);
        }, 20L);
    }

    private void initPacketListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
                Player player = (Player) event.getPlayer();
                if (player == null) return;
                WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
                if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.INTERACT) return;
                int entityId = wrapper.getEntityId();
                getNpcByEntityId(entityId).ifPresent(npc ->
                        Bukkit.getScheduler().runTask(plugin, () -> handleClick(player, npc))
                );
            }
        });
    }

    private void handleClick(Player player, NPC npc) {
        long now = System.currentTimeMillis();
        if (now - clickCooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) return;
        clickCooldowns.put(player.getUniqueId(), now);
        CoreAPI.getInstance().getSoundManager().playClick(player);
        playerClicksCache.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(npc.getId());
        hideAlertIfClicked(player, npc);
        actionRegistry.getAction(npc.getActionType()).ifPresent(action ->
                action.execute(player, npc)
        );
    }

    public void loadPlayerClicks(Player player) {
        CompletableFuture.runAsync(() -> {
            Document doc = npcPlayerDataCollection.find(Filters.eq("_id", player.getUniqueId().toString())).first();
            if (doc != null) {
                List<String> clickedIds = doc.getList("clicked_npcs", String.class);
                if (clickedIds != null) {
                    Set<String> clickSet = ConcurrentHashMap.newKeySet(clickedIds.size());
                    clickSet.addAll(clickedIds);
                    playerClicksCache.put(player.getUniqueId(), clickSet);
                }
            }
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () ->
                activeNpcs.values().forEach(npc -> hideAlertIfClicked(player, npc))
        ));
    }

    public void savePlayerClicks(UUID playerUuid) {
        Set<String> clickedIds = playerClicksCache.remove(playerUuid);
        if (clickedIds != null && !clickedIds.isEmpty()) {
            CompletableFuture.runAsync(() -> {
                Document doc = new Document("_id", playerUuid.toString()).append("clicked_npcs", new ArrayList<>(clickedIds));
                npcPlayerDataCollection.replaceOne(Filters.eq("_id", playerUuid.toString()), doc, new ReplaceOptions().upsert(true));
            });
        }
    }

    public CompletableFuture<Void> saveNpc(NPC npc) {
        return CompletableFuture.runAsync(() ->
                npcCollection.replaceOne(Filters.eq("_id", npc.getId()), npc.toDocument(), new ReplaceOptions().upsert(true))
        );
    }

    public void create(String id, String name, String displayName, Location location, String skinUsername, String actionType, List<String> actionValues) {
        if (activeNpcs.containsKey(id.toLowerCase())) {
            delete(id);
        }
        skinManager.getSkin(skinUsername).thenAccept(skinOpt -> {
            int entityId = entityIdCounter.decrementAndGet();
            String profileName = (name.length() > 16) ? name.substring(0, 16) : name;
            NPC npc = new NPC(id.toLowerCase(), entityId, UUID.randomUUID(), profileName, displayName, location, skinOpt.orElse(null), skinUsername, null, actionType, actionValues, plugin.getServerName(), true, null);
            activeNpcs.put(id.toLowerCase(), npc);
            entityIdToNpcId.put(entityId, id.toLowerCase());
            saveNpc(npc).thenRun(() -> showToAll(npc));
        });
    }

    public void create(String id, String name, Location location, String skinUsername) {
        create(id, name, name, location, skinUsername, "NONE", new ArrayList<>());
    }

    public void delete(String id) {
        npcCollection.deleteOne(Filters.eq("_id", id.toLowerCase()));
        NPC npc = activeNpcs.remove(id.toLowerCase());
        if (npc != null) {
            entityIdToNpcId.remove(npc.getEntityId());
            npc.getViewers().forEach(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) hideFrom(p, npc);
            });
        }
    }

    public void updateAndReshow(NPC npc) {
        List<Player> currentViewers = npc.getViewers().stream()
                .map(Bukkit::getPlayer).filter(Objects::nonNull).collect(Collectors.toList());
        currentViewers.forEach(player -> hideFrom(player, npc));
        Bukkit.getScheduler().runTaskLater(plugin, () -> currentViewers.forEach(player -> showTo(player, npc)), 5L);
    }

    private void hideAlertIfClicked(Player player, NPC npc) {
        npc.getClickAlert().ifPresent(alert -> {
            String holoId = "npc_alert_" + npc.getId();

            boolean shouldShow = false;
            if ("FIRST".equalsIgnoreCase(alert.mode())) {
                boolean hasClicked = playerClicksCache.getOrDefault(player.getUniqueId(), Collections.emptySet()).contains(npc.getId());
                if (!hasClicked) {
                    shouldShow = true;
                }
            }

            Hologram hologram = HologramAPI.get(holoId).orElseGet(() -> {
                Location holoLoc = npc.getLocation().clone().add(0, 2.3, 0);
                return HologramAPI.create(holoId, holoLoc, List.of(alert.text()), false);
            });

            if (shouldShow) {
                hologram.showTo(player);
            } else {
                hologram.hideFrom(player);
            }
        });
    }

    public void showTo(Player player, NPC npc) {
        if (npc == null || !player.isOnline()) return;

        packetFactory.createSpawnPackets(npc).forEach(packet ->
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet)
        );
        npc.addViewer(player.getUniqueId());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerPlayerInfoRemove(npc.getUuid()));
            }
        }, 40L);

        hideAlertIfClicked(player, npc);
    }

    public Optional<NPC> getNpcByEntityId(int entityId) {
        String npcId = entityIdToNpcId.get(entityId);
        return (npcId != null) ? getNpc(npcId) : Optional.empty();
    }

    public Optional<NPC> getNpc(String id) {
        return Optional.ofNullable(activeNpcs.get(id.toLowerCase()));
    }

    public Collection<NPC> getAllNpcs() {
        return Collections.unmodifiableCollection(activeNpcs.values());
    }

    public Set<String> getNpcIds() {
        return Collections.unmodifiableSet(activeNpcs.keySet());
    }

    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }

    public SkinManager getSkinManager() {
        return skinManager;
    }

    public void showToAll(NPC npc) {
        if (npc == null) return;
        npc.setGlobal(true);
        Bukkit.getOnlinePlayers().forEach(player -> showTo(player, npc));
    }

    public void hideFrom(Player player, NPC npc) {
        if (npc == null || !player.isOnline()) return;
        packetFactory.createDestroyPackets(npc).forEach(packet ->
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet)
        );
        npc.removeViewer(player.getUniqueId());

        HologramAPI.get("npc_alert_" + npc.getId()).ifPresent(hologram -> hologram.hideFrom(player));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loadPlayerClicks(player);
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                activeNpcs.values().stream()
                        .filter(NPC::isGlobal)
                        .forEach(npc -> showTo(player, npc)), 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        activeNpcs.values().forEach(npc -> npc.removeViewer(player.getUniqueId()));
        savePlayerClicks(player.getUniqueId());
    }

    private void startHeadTrackerTask() {
        this.headTrackerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (NPC npc : activeNpcs.values()) {
                if (!npc.isLookAtPlayer() || npc.getViewers().isEmpty()) continue;
                Location npcLocation = npc.getLocation();
                if (npcLocation == null || npcLocation.getWorld() == null) continue;
                for (UUID viewerUUID : npc.getViewers()) {
                    Player viewer = Bukkit.getPlayer(viewerUUID);
                    if (viewer == null || !viewer.getWorld().equals(npcLocation.getWorld()) || viewer.getLocation().distanceSquared(npcLocation) > 100) {
                        continue;
                    }
                    Location viewerLocation = viewer.getEyeLocation();
                    Vector direction = viewerLocation.toVector().subtract(npcLocation.toVector());
                    Location rotatedLocation = npcLocation.clone().setDirection(direction);
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packetFactory.createHeadRotationPacket(npc, rotatedLocation.getYaw()));
                }
            }
        }, 0L, 2L);
    }
}