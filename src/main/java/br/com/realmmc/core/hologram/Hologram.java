package br.com.realmmc.core.hologram;

import br.com.realmmc.core.hologram.placeholder.PlaceholderRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Hologram {

    private final String id;
    private final Location baseLocation;
    private final List<String> lines = new CopyOnWriteArrayList<>();
    private final List<TextDisplay> displayEntities = new ArrayList<>();
    private static final double LINE_HEIGHT = 0.3;

    public Hologram(String id, Location location, List<String> lines) {
        this.id = id;
        this.baseLocation = location;
        this.lines.addAll(lines);
    }

    public void spawn() {
        if (!displayEntities.isEmpty()) {
            despawn();
        }
        for (int i = 0; i < lines.size(); i++) {
            Location lineLocation = baseLocation.clone().add(0, (lines.size() - 1 - i) * LINE_HEIGHT, 0);
            String text = PlaceholderRegistry.replacePlaceholders(lines.get(i));

            baseLocation.getWorld().spawn(lineLocation, TextDisplay.class, entity -> {
                entity.setText(text);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setSeeThrough(true);
                Transformation transformation = entity.getTransformation();
                transformation.getScale().set(new Vector3f(1.5f, 1.5f, 1.5f));
                entity.setTransformation(transformation);
                displayEntities.add(entity);
            });
        }
    }

    public void despawn() {
        displayEntities.forEach(Entity::remove);
        displayEntities.clear();
    }

    public void update() {
        for (int i = 0; i < lines.size(); i++) {
            if (i < displayEntities.size()) {
                String newText = PlaceholderRegistry.replacePlaceholders(lines.get(i));
                TextDisplay display = displayEntities.get(i);
                if (!display.getText().equals(newText)) {
                    display.setText(newText);
                }
            }
        }
    }

    public void setLines(List<String> newLines) {
        this.lines.clear();
        this.lines.addAll(newLines);
        despawn();
        spawn();
    }

    public String getId() {
        return id;
    }

    public Location getBaseLocation() {
        return baseLocation;
    }

    public List<String> getLines() {
        return new ArrayList<>(lines);
    }
}