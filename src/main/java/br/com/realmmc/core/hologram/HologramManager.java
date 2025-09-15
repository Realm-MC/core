package br.com.realmmc.core.hologram;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.utils.ColorAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o ciclo de vida, visibilidade e atualização de todos os hologramas.
 */
public class HologramManager implements Listener {

    private final Main plugin;
    private final Map<String, Hologram> activeHolograms = new ConcurrentHashMap<>();

    public HologramManager(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Hologram create(String id, Location location, List<String> lines) {
        if (activeHolograms.containsKey(id.toLowerCase())) {
            delete(id.toLowerCase());
        }
        Hologram hologram = new Hologram(id.toLowerCase(), location, lines);
        activeHolograms.put(id.toLowerCase(), hologram);
        return hologram;
    }

    public void delete(String id) {
        Hologram hologram = activeHolograms.remove(id.toLowerCase());
        if (hologram != null) {
            hologram.getViewers().forEach(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) hideFrom(p, hologram);
            });
        }
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
                String formattedLine = ColorAPI.format(line);
                td.text(LegacyComponentSerializer.legacySection().deserialize(formattedLine));

                td.setBillboard(Display.Billboard.CENTER);
                td.setViewRange(64f);
                td.setBrightness(new Display.Brightness(15, 15));
                td.setSeeThrough(true);

                td.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new Quaternionf(),
                        new Vector3f(1.0f, 1.0f, 1.0f),
                        new Quaternionf()
                ));
            });

            hologram.getLineEntityIds().put(i, display.getUniqueId());
            loc.subtract(0, 0.3, 0);
        }

        hologram.addViewer(player.getUniqueId());
        hologram.setGlobal(false);
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

    public void updateLines(Hologram hologram, List<String> newLines) {
        hologram.setLines(newLines);
        hologram.getViewers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) showTo(p, hologram);
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        activeHolograms.values().stream()
                .filter(Hologram::isGlobal)
                .forEach(hologram -> showTo(event.getPlayer(), hologram));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeHolograms.values().forEach(hologram -> hologram.removeViewer(event.getPlayer().getUniqueId()));
    }
}