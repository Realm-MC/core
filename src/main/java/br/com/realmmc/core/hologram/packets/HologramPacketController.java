package br.com.realmmc.core.hologram.packets;

import br.com.realmmc.core.hologram.placeholder.PlaceholderRegistry;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class HologramPacketController {

    private static final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private static final AtomicInteger fallbackEntityId = new AtomicInteger(Integer.MAX_VALUE / 2);

    private HologramPacketController() {}

    public static int spawnTextDisplay(Player player, Location loc, String text, ChatColor backgroundColor) {
        int entityId = fallbackEntityId.decrementAndGet();
        sendSpawnPacket(player, entityId, loc, EntityType.TEXT_DISPLAY);
        updateTextDisplay(player, entityId, text, backgroundColor);
        return entityId;
    }

    public static int spawnItemDisplay(Player player, Location loc, ItemStack itemStack) {
        int entityId = fallbackEntityId.decrementAndGet();
        sendSpawnPacket(player, entityId, loc, EntityType.ITEM_DISPLAY);
        updateItemDisplay(player, entityId, itemStack);
        return entityId;
    }

    public static void updateTextDisplay(Player player, int entityId, String text, ChatColor backgroundColor) {
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        String processedText = PlaceholderRegistry.replacePlaceholders(text);
        String jsonText = GsonComponentSerializer.gson().serialize(net.kyori.adventure.text.Component.text(processedText));

        watcher.setObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(23, WrappedDataWatcher.Registry.getChatComponentSerializer(false)),
                WrappedChatComponent.fromJson(jsonText)
        );

        if (backgroundColor != null) {
            int argb = backgroundColor.asBungee().getColor().getRGB();
            watcher.setObject(
                    // ✅ Esta é a forma correta para a sua versão do ProtocolLib
                    new WrappedDataWatcher.WrappedDataWatcherObject(26, WrappedDataWatcher.Registry.get(Integer.class)),
                    argb
            );
        }

        watcher.setByte(15, (byte) 3, true);
        sendMetadataPacket(player, entityId, watcher);
    }

    public static void updateItemDisplay(Player player, int entityId, ItemStack itemStack) {
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(23, WrappedDataWatcher.Registry.getItemStackSerializer(false)),
                itemStack
        );
        watcher.setByte(15, (byte) 3, true);
        sendMetadataPacket(player, entityId, watcher);
    }

    public static void updateDisplayTransformation(Player player, int entityId, Transformation transformation) {
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        watcher.setObject(
                // ✅ Esta é a forma correta para a sua versão do ProtocolLib
                new WrappedDataWatcher.WrappedDataWatcherObject(14, WrappedDataWatcher.Registry.get(Transformation.class)),
                transformation
        );
        sendMetadataPacket(player, entityId, watcher);
    }

    public static void destroyEntities(Player player, List<Integer> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) return;
        PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroyPacket.getIntLists().write(0, entityIds);
        try {
            protocolManager.sendServerPacket(player, destroyPacket);
        } catch (Exception e) {
            // Ignored
        }
    }

    private static void sendSpawnPacket(Player player, int entityId, Location loc, EntityType type) {
        PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawnPacket.getIntegers().write(0, entityId);
        spawnPacket.getUUIDs().write(0, UUID.randomUUID());
        spawnPacket.getEntityTypeModifier().write(0, type);
        spawnPacket.getDoubles().write(0, loc.getX()).write(1, loc.getY()).write(2, loc.getZ());
        try {
            protocolManager.sendServerPacket(player, spawnPacket);
        } catch (Exception e) { /* Ignored */ }
    }

    private static void sendMetadataPacket(Player player, int entityId, WrappedDataWatcher watcher) {
        PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, entityId);
        List<WrappedDataValue> dataValues = watcher.toDataValueCollection();
        metadataPacket.getDataValueCollectionModifier().write(0, dataValues);
        try {
            protocolManager.sendServerPacket(player, metadataPacket);
        } catch (Exception e) { /* Ignored */ }
    }
}