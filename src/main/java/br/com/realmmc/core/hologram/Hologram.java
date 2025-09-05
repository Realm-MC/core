package br.com.realmmc.core.hologram;

import br.com.realmmc.core.Main;
import br.com.realmmc.core.hologram.line.HologramLine;
import br.com.realmmc.core.hologram.line.ItemLine;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Hologram {

    private final String id;
    private Location baseLocation;
    private final List<HologramLine> defaultLines = new ArrayList<>();
    // ✅ Cache para a lógica anti-flicker
    private final Map<UUID, List<String>> lastSentLines = new ConcurrentHashMap<>();
    private final Map<UUID, List<HologramLine>> playerVisibleLines = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> animationTasks = new ConcurrentHashMap<>();
    private final Main plugin;

    public Hologram(String id, Location location, Main plugin) {
        this.id = id;
        this.baseLocation = location;
        this.plugin = plugin;
    }

    public void addLine(HologramLine line) {
        this.defaultLines.add(line);
        line.setHologram(this);
    }

    // Método principal que agora é inteligente
    public void showOrUpdate(Player player, List<HologramLine> newLines) {
        List<String> newLinesAsText = newLines.stream().map(Object::toString).collect(Collectors.toList());

        // ✅ LÓGICA ANTI-FLICKER: Compara as linhas novas com as que já foram enviadas
        if (lastSentLines.getOrDefault(player.getUniqueId(), Collections.emptyList()).equals(newLinesAsText)) {
            return; // Se forem iguais, não faz nada, evitando o flicker.
        }

        // Se mudou, despawna as linhas antigas
        despawn(player);

        // ✅ LÓGICA DE EMPILHAMENTO CORRIGIDA
        double currentY = this.baseLocation.getY();
        List<HologramLine> spawnedLines = new ArrayList<>();
        // Itera normalmente, de cima para baixo
        for (HologramLine line : newLines) {
            Location lineLocation = this.baseLocation.clone();
            lineLocation.setY(currentY);

            line.setHologram(this);
            line.spawn(player, lineLocation);
            spawnedLines.add(line);

            // Subtrai a altura para a próxima linha ficar abaixo
            currentY -= line.getHeight();

            if (line instanceof ItemLine itemLine && itemLine.hasAnimation()) {
                startAnimationFor(player, itemLine);
            }
        }

        // Atualiza o cache com as novas informações
        playerVisibleLines.put(player.getUniqueId(), spawnedLines);
        lastSentLines.put(player.getUniqueId(), newLinesAsText);
    }

    public void despawn(Player player) {
        List<HologramLine> linesToDespawn = playerVisibleLines.remove(player.getUniqueId());
        if (linesToDespawn != null) {
            linesToDespawn.forEach(line -> line.despawn(player));
        }
        lastSentLines.remove(player.getUniqueId()); // Limpa o cache
        stopAnimationFor(player);
    }

    private void startAnimationFor(Player player, ItemLine itemLine) {
        stopAnimationFor(player);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                itemLine.runAnimationTick(player);
            }
        }.runTaskTimerAsynchronously(this.plugin, 0L, 1L);
        animationTasks.put(player.getUniqueId(), task);
    }

    private void stopAnimationFor(Player player) {
        BukkitTask task = animationTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public String getId() { return id; }
    public Location getBaseLocation() { return baseLocation; }
    public List<HologramLine> getDefaultLines() { return defaultLines; }
    public void setBaseLocation(Location location) { this.baseLocation = location; }
}