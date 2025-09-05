package br.com.realmmc.core.hologram;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.hologram.line.HologramLine;
import br.com.realmmc.core.hologram.line.TextLine;
import br.com.realmmc.core.npc.NPC;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager implements Listener {

    private final Main plugin;
    private final Map<String, Hologram> activeHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> visibleHolograms = new ConcurrentHashMap<>();
    private BukkitTask updateTask;
    private final int viewDistanceSquared = 32 * 32;

    public HologramManager(Main plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startUpdater();
    }

    public void createOrUpdateHologram(Hologram hologram) {
        deleteHologram(hologram.getId());
        activeHolograms.put(hologram.getId().toLowerCase(), hologram);
        Bukkit.getOnlinePlayers().forEach(this::checkVisibilityForPlayer);
    }

    public void deleteHologram(String id) {
        Hologram hologram = activeHolograms.remove(id.toLowerCase());
        if (hologram != null) {
            Bukkit.getOnlinePlayers().forEach(hologram::despawn);
        }
        visibleHolograms.forEach((uuid, set) -> set.remove(id.toLowerCase()));
    }

    public Optional<Hologram> getHologram(String id) {
        return Optional.ofNullable(activeHolograms.get(id.toLowerCase()));
    }

    public void despawnAll() {
        if (updateTask != null) updateTask.cancel();
        activeHolograms.values().forEach(h -> Bukkit.getOnlinePlayers().forEach(h::despawn));
        activeHolograms.clear();
        visibleHolograms.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkVisibilityForPlayer(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Set<String> holograms = visibleHolograms.remove(event.getPlayer().getUniqueId());
        if (holograms != null) {
            holograms.forEach(id -> getHologram(id).ifPresent(h -> h.despawn(event.getPlayer())));
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkVisibilityForPlayer(event.getPlayer()), 5L);
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

        activeHolograms.values().stream()
                .filter(h -> !h.getId().startsWith("npc_") && isLocationInVicinity(player, h.getBaseLocation()))
                .map(Hologram::getId)
                .forEach(shouldBeVisible::add);

        for (net.citizensnpcs.api.npc.NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.isSpawned() && isLocationInVicinity(player, npc.getStoredLocation())) {
                String npcId = npc.data().get("npc-id");
                if (npcId != null) shouldBeVisible.add("npc_" + npcId.toLowerCase());
            }
        }

        Set<String> toDespawn = new HashSet<>(currentlyVisible);
        toDespawn.removeAll(shouldBeVisible);

        toDespawn.forEach(holoId -> despawnHologramForPlayer(player, holoId));
        shouldBeVisible.forEach(holoId -> updateHologramForPlayer(player, holoId));
    }

    private boolean isLocationInVicinity(Player player, Location location) {
        if (location == null || location.getWorld() == null || !player.getWorld().equals(location.getWorld())) return false;
        return player.getLocation().distanceSquared(location) <= viewDistanceSquared;
    }

    private void updateHologramForPlayer(Player player, String holoId) {
        if (holoId.startsWith("npc_")) {
            updateNpcHologram(player, holoId);
        } else {
            getHologram(holoId).ifPresent(hologram -> {
                hologram.showOrUpdate(player, hologram.getDefaultLines());
                visibleHolograms.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(holoId);
            });
        }
    }

    private void updateNpcHologram(Player player, String holoId) {
        String npcId = holoId.substring(4);
        CoreAPI.getInstance().getNpcManager().getNpc(npcId).ifPresent(definition -> {
            net.citizensnpcs.api.npc.NPC citizensNpc = CoreAPI.getInstance().getNpcManager().findNpc(npcId);
            if (citizensNpc == null || !citizensNpc.isSpawned()) {
                despawnHologramForPlayer(player, holoId);
                return;
            }

            determineNpcLines(definition, player).thenAccept(newLines -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Hologram hologram = getHologram(holoId).orElseGet(() -> {
                        Location location = citizensNpc.getStoredLocation().clone().add(0, 2.3, 0);
                        Hologram newHolo = new Hologram(holoId, location, plugin);
                        activeHolograms.put(holoId, newHolo);
                        return newHolo;
                    });

                    hologram.showOrUpdate(player, newLines);
                    visibleHolograms.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(holoId);
                });
            });
        });
    }

    private void despawnHologramForPlayer(Player player, String holoId) {
        getHologram(holoId).ifPresent(hologram -> hologram.despawn(player));
        if (visibleHolograms.containsKey(player.getUniqueId())) {
            visibleHolograms.get(player.getUniqueId()).remove(holoId);
        }
    }

    private CompletableFuture<List<HologramLine>> determineNpcLines(NPC definition, Player viewer) {
        return CoreAPI.getInstance().getNpcManager().getClickCount(definition.getId(), viewer.getUniqueId())
                .thenApply(count -> {
                    List<HologramLine> lines = new ArrayList<>();

                    definition.getClickAlert().ifPresent(alert -> {
                        if ((alert.mode().equalsIgnoreCase("FIRST") && count == 0) || alert.mode().equalsIgnoreCase("EVERYONE")) {
                            lines.add(TextLine.builder().text(alert.text()).build());
                        }
                    });

                    lines.add(TextLine.builder().text(definition.getDisplayName()).build());

                    // A ordem das linhas é invertida para que o empilhamento para baixo funcione corretamente
                    Collections.reverse(lines);
                    return lines;
                });
    }
}