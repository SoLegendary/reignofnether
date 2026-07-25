package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

// per-client vision state: the sent (bright) chunk set plus the covered-column bitmask of each edge chunk.
// Server-authoritative — the client renders exactly these masks. Resent whenever either changes.
public class FogChunksClientboundPacket {

    public final Set<ChunkPos> bright;
    public final Map<ChunkPos, long[]> edgeMasks;

    public static void send(ServerPlayer player, Set<ChunkPos> bright, Map<ChunkPos, long[]> edgeMasks) {
        //if (player.getName().getString().equals("SoLegendary")) {
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FogChunksClientboundPacket(bright, edgeMasks)
        );
        //}
    }

    public FogChunksClientboundPacket(Set<ChunkPos> bright, Map<ChunkPos, long[]> edgeMasks) {
        this.bright = bright;
        this.edgeMasks = edgeMasks;
    }

    public FogChunksClientboundPacket(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        this.bright = new HashSet<>(n * 2);
        for (int i = 0; i < n; i++)
            this.bright.add(new ChunkPos(buf.readLong()));
        int m = buf.readVarInt();
        this.edgeMasks = new HashMap<>(m * 2);
        for (int i = 0; i < m; i++) {
            ChunkPos cp = new ChunkPos(buf.readLong());
            long[] mask = new long[] { buf.readLong(), buf.readLong(), buf.readLong(), buf.readLong() };
            this.edgeMasks.put(cp, mask);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(bright.size());
        for (ChunkPos p : bright) buf.writeLong(p.toLong());
        buf.writeVarInt(edgeMasks.size());
        for (Map.Entry<ChunkPos, long[]> e : edgeMasks.entrySet()) {
            buf.writeLong(e.getKey().toLong());
            long[] mask = e.getValue();
            buf.writeLong(mask[0]);
            buf.writeLong(mask[1]);
            buf.writeLong(mask[2]);
            buf.writeLong(mask[3]);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                FogOfWarClientEvents.applyServerFogState(bright, edgeMasks);
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
