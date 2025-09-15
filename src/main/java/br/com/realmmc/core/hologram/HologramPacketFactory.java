package br.com.realmmc.core.hologram;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fábrica responsável por criar os pacotes brutos para manipulação de hologramas.
 */
public class HologramPacketFactory {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public List<PacketWrapper<?>> createSpawnPackets(int entityId, UUID entityUUID, Location location, String text) {
        List<PacketWrapper<?>> packets = new ArrayList<>();

        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(entityUUID),
                EntityTypes.TEXT_DISPLAY,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getPitch(),
                location.getYaw(),
                location.getYaw(),
                0,
                Optional.empty()
        );

        Component textComponent = miniMessage.deserialize(text);

        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData(22, EntityDataTypes.ADV_COMPONENT, textComponent));
        metadata.add(new EntityData(14, EntityDataTypes.BYTE, (byte) 1));
        metadata.add(new EntityData(23, EntityDataTypes.INT, 0));

        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);

        packets.add(spawnPacket);
        packets.add(metadataPacket);
        return packets;
    }

    public WrapperPlayServerEntityMetadata createUpdateTextPacket(int entityId, String newText) {
        Component textComponent = miniMessage.deserialize(newText);
        List<EntityData<?>> metadata = List.of(
                new EntityData(22, EntityDataTypes.ADV_COMPONENT, textComponent)
        );
        return new WrapperPlayServerEntityMetadata(entityId, metadata);
    }

    public WrapperPlayServerDestroyEntities createDestroyPacket(int... entityIds) {
        return new WrapperPlayServerDestroyEntities(entityIds);
    }
}