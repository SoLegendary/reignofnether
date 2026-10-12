package com.solegendary.reignofnether.debug;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

// Server -> client: the set of currently-built walkability (navmesh) chunk keys, so the debug overlay can show
// which chunks have a built mesh. Sent once per second alongside the perf stats.
public class RtsDebugChunksClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<RtsDebugChunksClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "rts_debug_chunks_clientbound"));

    @Override
    public CustomPacketPayload.Type<RtsDebugChunksClientboundPacket> type() {
        return TYPE;
    }

    private final long[] keys;

    public static void broadcast(long[] keys) {
        PacketHandler.send(PacketHandler.allPlayers(), new RtsDebugChunksClientboundPacket(keys));
    }

    public RtsDebugChunksClientboundPacket(long[] keys) {
        this.keys = keys;
    }

    public RtsDebugChunksClientboundPacket(RegistryFriendlyByteBuf buffer) {
        int n = buffer.readVarInt();
        this.keys = new long[n];
        for (int i = 0; i < n; i++) this.keys[i] = buffer.readLong();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.keys.length);
        for (long k : this.keys) buffer.writeLong(k);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() ->
            DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RtsDebugNavmesh.setBuiltChunks(this.keys)));
    }
}
