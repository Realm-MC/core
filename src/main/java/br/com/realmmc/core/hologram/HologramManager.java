package br.com.realmmc.core.hologram;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.api.CoreAPI;
import br.com.realmmc.core.utils.ColorAPI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager implements Listener {

    private final Main plugin;
    private final Map<String, Hologram> activeHolograms = new ConcurrentHashMap<>();
    private final MongoCollection<Document> hologramCollection;

    public HologramManager(Main plugin) {
        this.plugin = plugin;
        this.hologramCollection = CoreAPI.getInstance().getDatabaseManager().getDatabase().getCollection("holograms");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadHolograms() {
        CompletableFuture.runAsync(() -> {
            for (Document doc : hologramCollection.find(Filters.eq("server", plugin.getServerName()))) {
                Hologram hologram = Hologram.fromDocument(doc);
                activeHolograms.put(hologram.getId(), hologram);
            }
        }).thenRun(() -> {
            plugin.getLogger().info(activeHolograms.size() + " hologramas persistentes carregados do banco de dados.");
            Bukkit.getScheduler().runTask(plugin, () ->
                    activeHolograms.values().stream()
                            .filter(Hologram::isPersistent)
                            .forEach(this::showToAll)
            );
        });
    }

    public Hologram create(String id, Location location, List<String> lines, boolean persistent) {
        if (activeHolograms.containsKey(id.toLowerCase())) {
            delete(id.toLowerCase());
        }
        Hologram hologram = new Hologram(id.toLowerCase(), location, lines);
        hologram.setPersistent(persistent);
        activeHolograms.put(id.toLowerCase(), hologram);

        if (persistent) {
            saveHologram(hologram);
        }
        return hologram;
    }

    public void delete(String id) {
        Hologram hologram = activeHolograms.remove(id.toLowerCase());
        if (hologram != null) {
            hologram.getViewers().forEach(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) hideFrom(p, hologram);
            });
            if (hologram.isPersistent()) {
                CompletableFuture.runAsync(() -> hologramCollection.deleteOne(Filters.eq("_id", id.toLowerCase())));
            }
        }
    }

    private void saveHologram(Hologram hologram) {
        CompletableFuture.runAsync(() ->
                hologramCollection.replaceOne(
                        Filters.eq("_id", hologram.getId()),
                        hologram.toDocument(),
                        new ReplaceOptions().upsert(true)
                )
        );
    }

    public Optional<Hologram> getHologram(String id) {
        return Optional.ofNullable(activeHolograms.get(id.toLowerCase()));
    }

    public void showTo(Player player, Hologram hologram) {
        if (hologram == null || !player.isOnline()) return;
        hideFrom(player, hologram);
        Location loc = hologram.getLocation().clone();
        for (int i = 0; i < hologram.getLines().size(); i++) {
            String line = hologram.getLines().get(i);
            TextDisplay display = player.getWorld().spawn(loc, TextDisplay.class, td -> {
                td.text(LegacyComponentSerializer.legacySection().deserialize(ColorAPI.format(line)));
                td.setBillboard(Display.Billboard.CENTER);
                td.setViewRange(64f);
                td.setSeeThrough(true);
            });
            hologram.getLineEntityIds().put(i, display.getUniqueId());
            loc.subtract(0, 0.3, 0);
        }
        hologram.addViewer(player.getUniqueId());
    }

    public void showToAll(Hologram hologram) {
        if (hologram == null) return;
        hologram.setGlobal(true);
        Bukkit.getOnlinePlayers().forEach(player -> showTo(player, hologram));
    }

    public void hideFrom(Player player, Hologram hologram) {
        if (hologram == null || !player.isOnline() || hologram.getLineEntityIds().isEmpty()) return;
        hologram.getLineEntityIds().values().forEach(uuid -> {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        });
        hologram.removeViewer(player.getUniqueId());
        hologram.getLineEntityIds().clear();
    }

    public void updateLine(Hologram hologram, int lineIndex, String newText) {
        if (lineIndex >= 0 && lineIndex < hologram.getLines().size()) {
            hologram.getLines().set(lineIndex, newText);
        } else {
            return;
        }

        if (hologram.isPersistent()) {
            saveHologram(hologram);
        }

        UUID entityUuid = hologram.getLineEntityIds().get(lineIndex);
        if (entityUuid == null) return;

        org.bukkit.entity.Entity entity = Bukkit.getEntity(entityUuid);
        if (entity instanceof TextDisplay textDisplay && entity.isValid()) {
            textDisplay.text(LegacyComponentSerializer.legacySection().deserialize(ColorAPI.format(newText)));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        activeHolograms.values().stream()
                .filter(h -> h.isGlobal() || h.isPersistent())
                .forEach(hologram -> showTo(event.getPlayer(), hologram));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeHolograms.values().forEach(hologram -> hologram.removeViewer(event.getPlayer().getUniqueId()));
    }
}