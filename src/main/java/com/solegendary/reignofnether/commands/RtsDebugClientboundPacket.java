package com.solegendary.reignofnether.commands;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class RtsDebugClientboundPacket {

    private final boolean enabled;

    public static void broadcast(boolean enabled) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new RtsDebugClientboundPacket(enabled));
    }

    public RtsDebugClientboundPacket(boolean enabled) { this.enabled = enabled; }
    public RtsDebugClientboundPacket(FriendlyByteBuf buffer) { this.enabled = buffer.readBoolean(); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeBoolean(this.enabled); }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RtsDebug.enabled = this.enabled);
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
