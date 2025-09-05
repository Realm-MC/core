package br.com.realmmc.core.hologram;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.npc.NPC;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager implements Listener {

    private final Main plugin;
    private final Map<String, Hologram> activeHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> visibleHolograms = new ConcurrentHashMap<>();
    private File configFile;
    private FileConfiguration config;
    private BukkitTask updateTask;
    private final int viewDistanceSquared = 32 * 32;

    public HologramManager(Main plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadConfig();
    }

    public void loadHolograms() {
        if (config.isConfigurationSection("holograms")) {
            for (String id : config.getConfigurationSection("holograms").getKeys(false)) {
                Location location = config.getLocation("holograms." + id + ".location");
                List<String> lines = config.getStringList("holograms." + id + ".lines");
                if (location != null && location.getWorld() != null && !lines.isEmpty()) {
                    Hologram hologram = new Hologram(id, location, lines);
                    activeHolograms.put(id.toLowerCase(), hologram);
                }
            }
        }
        startUpdater();
    }

    public void despawnAll() {
        if (updateTask != null) updateTask.cancel();
        activeHolograms.values().forEach(h -> Bukkit.getOnlinePlayers().forEach(h::despawn));
        activeHolograms.clear();
        visibleHolograms.clear();
    }

    public void createHologram(String id, Location location, List<String> lines) {
        deleteHologram(id);
        Hologram hologram = new Hologram(id, location, lines);
        activeHolograms.put(id.toLowerCase(), hologram);
    }

    public void deleteHologram(String id) {
        Hologram hologram = activeHolograms.remove(id.toLowerCase());
        if (hologram != null) {
            Bukkit.getOnlinePlayers().forEach(hologram::despawn);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkVisibilityForPlayer(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Set<String> holograms = visibleHolograms.remove(event.getPlayer().getUniqueId());
        if (holograms != null) {
            holograms.forEach(id -> {
                Hologram h = activeHolograms.get(id);
                if (h != null) {
                    h.despawn(event.getPlayer());
                }
            });
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        checkVisibilityForPlayer(event.getPlayer());
    }

    private void startUpdater() {
        if (updateTask != null) updateTask.cancel();
        this.updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkVisibilityForPlayer(player);
            }
        }, 40L, 40L);
    }

    public void checkVisibilityForPlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        Set<String> currentlyVisible = visibleHolograms.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        Set<String> shouldBeVisible = new HashSet<>();

        for (Hologram hologram : activeHolograms.values()) {
            if (isLocationInVicinity(player, hologram.getBaseLocation())) {
                shouldBeVisible.add(hologram.getId().toLowerCase());
            }
        }

        for (net.citizensnpcs.api.npc.NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.isSpawned() && isLocationInVicinity(player, npc.getStoredLocation())) {
                String npcId = npc.data().get("npc-id");
                if (npcId != null) {
                    shouldBeVisible.add("npc_" + npcId.toLowerCase());
                }
            }
        }

        Set<String> toSpawn = new HashSet<>(shouldBeVisible);
        toSpawn.removeAll(currentlyVisible);

        Set<String> toDespawn = new HashSet<>(currentlyVisible);
        toDespawn.removeAll(shouldBeVisible);

        toDespawn.forEach(holoId -> despawnHologramForPlayer(player, holoId));
        toSpawn.forEach(holoId -> spawnOrUpdateHologramForPlayer(player, holoId));

        currentlyVisible.stream()
                .filter(holoId -> holoId.startsWith("npc_"))
                .forEach(holoId -> spawnOrUpdateHologramForPlayer(player, holoId));
    }

    private boolean isLocationInVicinity(Player player, Location location) {
        if (location == null || location.getWorld() == null || !player.getWorld().equals(location.getWorld())) return false;
        return player.getLocation().distanceSquared(location) <= viewDistanceSquared;
    }

    private void spawnOrUpdateHologramForPlayer(Player player, String holoId) {
        CompletableFuture<List<String>> linesFuture;
        Location location;

        if (holoId.startsWith("npc_")) {
            String npcId = holoId.substring(4);
            Optional<NPC> npcOpt = CoreAPI.getInstance().getNpcManager().getNpc(npcId);
            if (npcOpt.isEmpty()) return;
            NPC definition = npcOpt.get();

            net.citizensnpcs.api.npc.NPC citizensNpc = CoreAPI.getInstance().getNpcManager().findNpc(npcId);
            if (citizensNpc == null || !citizensNpc.isSpawned()) return;

            location = citizensNpc.getStoredLocation().clone().add(0, 2.1, 0);
            linesFuture = determineNpcLines(definition, player);
        } else {
            Hologram staticHologram = activeHolograms.get(holoId);
            if (staticHologram == null) return;
            location = staticHologram.getBaseLocation();
            linesFuture = CompletableFuture.completedFuture(staticHologram.getDefaultLines());
        }

        linesFuture.thenAccept(finalLines -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Hologram hologram = activeHolograms.computeIfAbsent(holoId, id -> new Hologram(id, location, finalLines));
                hologram.setBaseLocation(location);
                hologram.updateForPlayer(player, finalLines);
                visibleHolograms.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(holoId);
            });
        });
    }

    private void despawnHologramForPlayer(Player player, String holoId) {
        Hologram hologram = activeHolograms.get(holoId);
        if (hologram != null) {
            hologram.despawn(player);
        }
        visibleHolograms.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).remove(holoId);
    }

    private CompletableFuture<List<String>> determineNpcLines(NPC definition, Player viewer) {
        Optional<NPC.ClickAlert> alertOpt = definition.getClickAlert();
        return CoreAPI.getInstance().getNpcManager().getClickCount(definition.getId(), viewer.getUniqueId())
                .thenApply(count -> {
                    List<String> lines = new ArrayList<>();
                    if (alertOpt.isPresent()) {
                        NPC.ClickAlert alert = alertOpt.get();
                        if ((alert.mode().equalsIgnoreCase("FIRST") && count == 0) || alert.mode().equalsIgnoreCase("EVERYONE")) {
                            lines.add(alert.text());
                        }
                    }
                    lines.add(definition.getDisplayName());
                    return lines;
                });
    }

    public Optional<Hologram> getHologram(String id) {
        return Optional.ofNullable(activeHolograms.get(id.toLowerCase()));
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "holograms.yml");
        if (!configFile.exists()) plugin.saveResource("holograms.yml", false);
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void saveConfig() {
        try { config.save(configFile); } catch (IOException e) { e.printStackTrace(); }
    }
}