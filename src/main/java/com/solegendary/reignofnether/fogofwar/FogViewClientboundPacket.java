package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

// Tells ONE player whether the server is hiding fog for them (see FogOfWarServerEvents.setFogHiddenFor).
public class FogViewClientboundPacket {

    public boolean hidden;

    public static void send(ServerPlayer player, boolean hidden) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new FogViewClientboundPacket(hidden));
    }

    public FogViewClientboundPacket(boolean hidden) {
        this.hidden = hidden;
    }

    public FogViewClientboundPacket(FriendlyByteBuf buffer) {
        this.hidden = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.hidden);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                FogOfWarClientEvents.setSpectatorFogHidden(hidden);
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
