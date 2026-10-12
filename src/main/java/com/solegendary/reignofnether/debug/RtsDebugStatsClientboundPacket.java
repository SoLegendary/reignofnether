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

// Server → client snapshot of perf counters. Sent once per second while /rts-debug is enabled.
public class RtsDebugStatsClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<RtsDebugStatsClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "rts_debug_stats_clientbound"));

    @Override
    public CustomPacketPayload.Type<RtsDebugStatsClientboundPacket> type() {
        return TYPE;
    }

    private final int pathsAvg;
    private final int queueAvg;
    private final int stuckAvg;
    private final double tickTime;
    private final double pathComputeMs; // avg pure A* compute time (worker threads)
    private final double pathE2eMs;     // avg submit -> delivered time (incl. queue wait)

    public static void broadcast(int pathsAvg, int queueAvg, int stuckAvg, double tickTime, double pathComputeMs, double pathE2eMs) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new RtsDebugStatsClientboundPacket(pathsAvg, queueAvg, stuckAvg, tickTime, pathComputeMs, pathE2eMs));
    }

    public RtsDebugStatsClientboundPacket(int pathsAvg, int queueAvg, int stuckAvg, double tickTime, double pathComputeMs, double pathE2eMs) {
        this.pathsAvg = pathsAvg;
        this.queueAvg = queueAvg;
        this.stuckAvg = stuckAvg;
        this.tickTime = tickTime;
        this.pathComputeMs = pathComputeMs;
        this.pathE2eMs = pathE2eMs;
    }

    public RtsDebugStatsClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.pathsAvg = buffer.readVarInt();
        this.queueAvg = buffer.readVarInt();
        this.stuckAvg = buffer.readVarInt();
        this.tickTime = buffer.readDouble();
        this.pathComputeMs = buffer.readDouble();
        this.pathE2eMs = buffer.readDouble();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.pathsAvg);
        buffer.writeVarInt(this.queueAvg);
        buffer.writeVarInt(this.stuckAvg);
        buffer.writeDouble(this.tickTime);
        buffer.writeDouble(this.pathComputeMs);
        buffer.writeDouble(this.pathE2eMs);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                RtsDebugClientEvents.pathsAvg = this.pathsAvg;
                RtsDebugClientEvents.queueAvg = this.queueAvg;
                RtsDebugClientEvents.stuckAvg = this.stuckAvg;
                RtsDebugClientEvents.tickTime = this.tickTime;
                RtsDebugClientEvents.pathComputeMs = this.pathComputeMs;
                RtsDebugClientEvents.pathE2eMs = this.pathE2eMs;
            });
        });
    }
}
