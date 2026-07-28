package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

// per-player live (currently visible/tracked) chunks and sent (already streamed to client) chunks.
// Sent to a client for debug/overlay purposes - shows what the server thinks each player can see.
public class PlayerChunksClientboundPacket {

    public final Map<UUID, Set<ChunkPos>> liveChunks;
    public final Map<UUID, Set<ChunkPos>> edgeChunks;

    public static void send(ServerPlayer player, Map<UUID, Set<ChunkPos>> liveChunks, Map<UUID, Set<ChunkPos>> sentChunks) {
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PlayerChunksClientboundPacket(liveChunks, sentChunks)
        );
    }

    public PlayerChunksClientboundPacket(Map<UUID, Set<ChunkPos>> liveChunks, Map<UUID, Set<ChunkPos>> edgeChunks) {
        this.liveChunks = liveChunks;
        this.edgeChunks = edgeChunks;
    }

    public PlayerChunksClientboundPacket(FriendlyByteBuf buf) {
        this.liveChunks = readMap(buf);
        this.edgeChunks = readMap(buf);
    }

    private static Map<UUID, Set<ChunkPos>> readMap(FriendlyByteBuf buf) {
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

    private static void writeMap(FriendlyByteBuf buf, Map<UUID, Set<ChunkPos>> map) {
        buf.writeVarInt(map.size());
        for (Map.Entry<UUID, Set<ChunkPos>> entry : map.entrySet()) {
            buf.writeUUID(entry.getKey());
            Set<ChunkPos> chunks = entry.getValue();
            buf.writeVarInt(chunks.size());
            for (ChunkPos cp : chunks)
                buf.writeLong(cp.toLong());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        writeMap(buf, liveChunks);
        writeMap(buf, edgeChunks);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                PlayerChunksClientEvents.applyServerState(liveChunks, edgeChunks);
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}