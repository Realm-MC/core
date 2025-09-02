package br.com.realmmc.core.hologram;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.hologram.placeholder.PlaceholderRegistry;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final Main plugin;
    private final Map<String, Hologram> activeHolograms = new ConcurrentHashMap<>();
    private File configFile;
    private FileConfiguration config;
    private BukkitTask updateTask;

    public HologramManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
        PlaceholderRegistry.initializeDefaults(plugin);
    }

    public void loadHolograms() {
        if (config.isConfigurationSection("holograms")) {
            for (String id : config.getConfigurationSection("holograms").getKeys(false)) {
                Location location = config.getLocation("holograms." + id + ".location");
                List<String> lines = config.getStringList("holograms." + id + ".lines");
                if (location != null && location.getWorld() != null && !lines.isEmpty()) {
                    Hologram hologram = new Hologram(id, location, lines);
                    hologram.spawn();
                    activeHolograms.put(id.toLowerCase(), hologram);
                }
            }
        }
        startUpdater();
    }

    public void saveHolograms() {
        config.set("holograms", null);
        for (Hologram hologram : activeHolograms.values()) {
            config.set("holograms." + hologram.getId() + ".location", hologram.getBaseLocation());
            config.set("holograms." + hologram.getId() + ".lines", hologram.getLines());
        }
        saveConfig();
    }

    public void despawnAll() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        activeHolograms.values().forEach(Hologram::despawn);
        activeHolograms.clear();
    }

    public Hologram createHologram(String id, Location location, List<String> lines) {
        if (activeHolograms.containsKey(id.toLowerCase())) {
            deleteHologram(id);
        }
        Hologram hologram = new Hologram(id, location, lines);
        hologram.spawn();
        activeHolograms.put(id.toLowerCase(), hologram);
        return hologram;
    }

    public void deleteHologram(String id) {
        Hologram hologram = activeHolograms.remove(id.toLowerCase());
        if (hologram != null) {
            hologram.despawn();
        }
    }

    public Optional<Hologram> getHologram(String id) {
        return Optional.ofNullable(activeHolograms.get(id.toLowerCase()));
    }

    private void startUpdater() {
        if (updateTask != null) updateTask.cancel();
        this.updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            activeHolograms.values().forEach(Hologram::update);
        }, 20L, 20L);
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "holograms.yml");
        if (!configFile.exists()) {
            plugin.saveResource("holograms.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}