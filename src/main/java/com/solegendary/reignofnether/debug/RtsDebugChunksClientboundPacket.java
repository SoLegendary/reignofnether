package com.solegendary.reignofnether.debug;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

// Server -> client: the set of currently-built walkability (navmesh) chunk keys, so the debug overlay can show
// which chunks have a built mesh. Sent once per second alongside the perf stats.
public class RtsDebugChunksClientboundPacket {

    private final long[] keys;

    public static void broadcast(long[] keys) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new RtsDebugChunksClientboundPacket(keys));
    }

    public RtsDebugChunksClientboundPacket(long[] keys) {
        this.keys = keys;
    }

    public RtsDebugChunksClientboundPacket(FriendlyByteBuf buffer) {
        int n = buffer.readVarInt();
        this.keys = new long[n];
        for (int i = 0; i < n; i++) this.keys[i] = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.keys.length);
        for (long k : this.keys) buffer.writeLong(k);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RtsDebugNavmesh.setBuiltChunks(this.keys)));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
