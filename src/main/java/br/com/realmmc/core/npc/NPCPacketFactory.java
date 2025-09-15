package br.com.realmmc.core.npc;

import br.com.realmmc.core.npc.skin.Skin;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.data.*;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class NPCPacketFactory {

    public List<PacketWrapper<?>> createSpawnPackets(NPC npc) {
        List<PacketWrapper<?>> packets = new ArrayList<>();
        Location bukkitLocation = npc.getLocation();

        String safeName = npc.getName() != null ? npc.getName() : "NPC";
        if (safeName.length() > 16) {
            safeName = safeName.substring(0, 16);
        }

        UserProfile profile = new UserProfile(npc.getUuid(), safeName);
        npc.getSkin().ifPresent(skin ->
                profile.getTextureProperties().add(new TextureProperty("textures", skin.value(), skin.signature()))
        );

        WrapperPlayServerPlayerInfoUpdate.PlayerInfo infoData =
                new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                        profile, true, 0, GameMode.CREATIVE,
                        Component.text(npc.getDisplayName() != null ? npc.getDisplayName() : safeName),
                        null
                );
        packets.add(new WrapperPlayServerPlayerInfoUpdate(EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER), infoData));

        com.github.retrooper.packetevents.protocol.world.Location packetEventsLocation =
                new com.github.retrooper.packetevents.protocol.world.Location(
                        bukkitLocation.getX(), bukkitLocation.getY(), bukkitLocation.getZ(),
                        bukkitLocation.getYaw(), bukkitLocation.getPitch()
                );
        packets.add(new WrapperPlayServerSpawnPlayer(npc.getEntityId(), npc.getUuid(), packetEventsLocation));

        // =================================================================================
        //  A CORREÇÃO FINAL PARA O ERRO DE TIPOS INCOMPATÍVEIS (GENERICS)
        // =================================================================================
        final List<EntityData<?>> metadataList = new ArrayList<>(); // AQUI ESTÁ A MUDANÇA: Adicionado <?>
        metadataList.add(new EntityData<>(17, EntityDataTypes.BYTE, (byte) 0x7f));

        EntityMetadataProvider metadataProvider = new EntityMetadataProvider() {
            @Override
            public List<EntityData<?>> entityData(ClientVersion clientVersion) {
                return metadataList;
            }
        };

        packets.add(new WrapperPlayServerEntityMetadata(npc.getEntityId(), metadataProvider));
        // =================================================================================

        return packets;
    }

    public PacketWrapper<?> createHeadRotationPacket(NPC npc, float yaw) {
        return new WrapperPlayServerEntityHeadLook(npc.getEntityId(), yaw);
    }

    public List<PacketWrapper<?>> createDestroyPackets(NPC npc) {
        return List.of(
                new WrapperPlayServerPlayerInfoRemove(npc.getUuid()),
                new WrapperPlayServerDestroyEntities(npc.getEntityId())
        );
    }
}