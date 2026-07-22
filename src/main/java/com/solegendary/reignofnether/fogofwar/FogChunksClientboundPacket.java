package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

// per-client bright set; resent whenever it changes
public class FogChunksClientboundPacket {

    public final Set<ChunkPos> bright;

    public static void send(ServerPlayer player, Set<ChunkPos> bright) {
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FogChunksClientboundPacket(bright)
        );
    }

    public FogChunksClientboundPacket(Set<ChunkPos> bright) {
        this.bright = bright;
    }

    public FogChunksClientboundPacket(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        this.bright = new HashSet<>(n * 2);
        for (int i = 0; i < n; i++)
            this.bright.add(new ChunkPos(buf.readLong()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(bright.size());
        for (ChunkPos p : bright) buf.writeLong(p.toLong());
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                FogOfWarClientEvents.applyServerFogState(bright);
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
