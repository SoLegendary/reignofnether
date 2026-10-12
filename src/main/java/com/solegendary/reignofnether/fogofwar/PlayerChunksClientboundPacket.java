package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;
import java.util.function.Supplier;

// per-player live (currently visible/tracked) chunks and sent (already streamed to client) chunks.
// Sent to a client for debug/overlay purposes - shows what the server thinks each player can see.
public class PlayerChunksClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<PlayerChunksClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "player_chunks_clientbound"));

    @Override
    public CustomPacketPayload.Type<PlayerChunksClientboundPacket> type() {
        return TYPE;
    }

    public final Map<UUID, Set<ChunkPos>> liveChunks;
    public final Map<UUID, Set<ChunkPos>> edgeChunks;

    public static void send(ServerPlayer player, Map<UUID, Set<ChunkPos>> liveChunks, Map<UUID, Set<ChunkPos>> sentChunks) {
        PacketHandler.send(
                PacketHandler.toPlayer(() -> player),
                new PlayerChunksClientboundPacket(liveChunks, sentChunks)
        );
    }

    public PlayerChunksClientboundPacket(Map<UUID, Set<ChunkPos>> liveChunks, Map<UUID, Set<ChunkPos>> edgeChunks) {
        this.liveChunks = liveChunks;
        this.edgeChunks = edgeChunks;
    }

    public PlayerChunksClientboundPacket(RegistryFriendlyByteBuf buf) {
        this.liveChunks = readMap(buf);
        this.edgeChunks = readMap(buf);
    }

    private static Map<UUID, Set<ChunkPos>> readMap(RegistryFriendlyByteBuf buf) {
        int players = buf.readVarInt();
        Map<UUID, Set<ChunkPos>> map = new HashMap<>(players * 2);
        for (int i = 0; i < players; i++) {
            UUID uuid = buf.readUUID();
            int n = buf.readVarInt();
            Set<ChunkPos> chunks = new HashSet<>(n * 2);
            for (int j = 0; j < n; j++)
                chunks.add(new ChunkPos(buf.readLong()));
            map.put(uuid, chunks);
        }
        return map;
    }

    private static void writeMap(RegistryFriendlyByteBuf buf, Map<UUID, Set<ChunkPos>> map) {
        buf.writeVarInt(map.size());
        for (Map.Entry<UUID, Set<ChunkPos>> entry : map.entrySet()) {
            buf.writeUUID(entry.getKey());
            Set<ChunkPos> chunks = entry.getValue();
            buf.writeVarInt(chunks.size());
            for (ChunkPos cp : chunks)
                buf.writeLong(cp.toLong());
        }
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        writeMap(buf, liveChunks);
        writeMap(buf, edgeChunks);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                PlayerChunksClientEvents.applyServerState(liveChunks, edgeChunks);
            });
        });
        return;
    }
}