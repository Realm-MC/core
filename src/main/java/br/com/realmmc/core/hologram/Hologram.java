package br.com.realmmc.core.hologram;

import br.com.realmmc.core.hologram.placeholder.PlaceholderRegistry;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Hologram {

    private final String id;
    private Location baseLocation;
    private List<String> defaultLines = new CopyOnWriteArrayList<>();
    private final Map<UUID, List<Integer>> playerVisibleEntities = new ConcurrentHashMap<>();
    private static final double LINE_HEIGHT = 0.3;
    private static final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private static final AtomicInteger fallbackEntityId = new AtomicInteger(Integer.MAX_VALUE / 2);

    public Hologram(String id, Location location, List<String> lines) {
        this.id = id;
        this.baseLocation = location;
        this.defaultLines.addAll(lines);
    }

    public void updateForPlayer(Player player, List<String> lines) {
        despawn(player);
        if (baseLocation.getWorld() == null || !player.getWorld().equals(baseLocation.getWorld())) {
            return;
        }
        List<Integer> entityIds = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            int entityId = fallbackEntityId.decrementAndGet();
            entityIds.add(entityId);
            Location lineLocation = baseLocation.clone().add(0, (lines.size() - 1 - i) * LINE_HEIGHT, 0);
            sendSpawnPacket(player, entityId, lineLocation);
            sendMetadataPacket(player, entityId, lines.get(i));
        }
        playerVisibleEntities.put(player.getUniqueId(), entityIds);
    }

    public void despawn(Player player) {
        List<Integer> entityIds = playerVisibleEntities.remove(player.getUniqueId());
        if (entityIds == null || entityIds.isEmpty()) return;
        PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroyPacket.getIntLists().write(0, entityIds);
        try {
            protocolManager.sendServerPacket(player, destroyPacket);
        } catch (Exception e) {
            // Ignorado
        }
    }

    private void sendSpawnPacket(Player player, int entityId, Location loc) {
        PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawnPacket.getIntegers().write(0, entityId);
        spawnPacket.getUUIDs().write(0, UUID.randomUUID());
        spawnPacket.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);
        spawnPacket.getDoubles()
                .write(0, loc.getX())
                .write(1, loc.getY())
                .write(2, loc.getZ());
        try {
            protocolManager.sendServerPacket(player, spawnPacket);
        } catch (Exception e) {
            // Ignorado
        }
    }

    private void sendMetadataPacket(Player player, int entityId, String text) {
        PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, entityId);

        WrappedDataWatcher watcher = new WrappedDataWatcher();
        String processedText = PlaceholderRegistry.replacePlaceholders(text);
        String jsonText = GsonComponentSerializer.gson().serialize(net.kyori.adventure.text.Component.text(processedText));

        watcher.setObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(23, WrappedDataWatcher.Registry.getChatComponentSerializer(false)),
                WrappedChatComponent.fromJson(jsonText)
        );

        // Esta é a forma final e correta que não gera avisos de "deprecated"
        watcher.setByte(15, (byte) 3, true);

        List<WrappedDataValue> dataValues = watcher.toDataValueCollection();
        metadataPacket.getDataValueCollectionModifier().write(0, dataValues);
        try {
            protocolManager.sendServerPacket(player, metadataPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setBaseLocation(Location location) {
        this.baseLocation = location;
    }

    public String getId() { return id; }
    public Location getBaseLocation() { return baseLocation; }
    public List<String> getDefaultLines() { return defaultLines; }
}