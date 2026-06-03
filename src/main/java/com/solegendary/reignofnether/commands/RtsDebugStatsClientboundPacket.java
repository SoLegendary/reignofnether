package com.solegendary.reignofnether.commands;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

// Server → client snapshot of perf counters. Sent once per second while /rts-debug is enabled.
public class RtsDebugStatsClientboundPacket {

    private final int pathsAvg;
    private final int queueAvg;
    private final int stuckAvg;

    public static void broadcast(int pathsAvg, int queueAvg, int stuckAvg) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new RtsDebugStatsClientboundPacket(pathsAvg, queueAvg, stuckAvg));
    }

    public RtsDebugStatsClientboundPacket(int pathsAvg, int queueAvg, int stuckAvg) {
        this.pathsAvg = pathsAvg;
        this.queueAvg = queueAvg;
        this.stuckAvg = stuckAvg;
    }

    public RtsDebugStatsClientboundPacket(FriendlyByteBuf buffer) {
        this.pathsAvg = buffer.readVarInt();
        this.queueAvg = buffer.readVarInt();
        this.stuckAvg = buffer.readVarInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.pathsAvg);
        buffer.writeVarInt(this.queueAvg);
        buffer.writeVarInt(this.stuckAvg);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                RtsDebugClientEvents.pathsAvg = this.pathsAvg;
                RtsDebugClientEvents.queueAvg = this.queueAvg;
                RtsDebugClientEvents.stuckAvg = this.stuckAvg;
            });
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
