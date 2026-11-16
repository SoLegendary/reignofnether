package com.solegendary.reignofnether.editor;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Tells the host their test match started: the editor gives way to the RTS HUD and the Stop bar. */
public class EditorTestClientboundPacket {

    public static void testing(ServerPlayer host) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> host), new EditorTestClientboundPacket());
    }

    public EditorTestClientboundPacket() { }

    public EditorTestClientboundPacket(FriendlyByteBuf buffer) { }

    public void encode(FriendlyByteBuf buffer) { }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> EditorTest::setTesting));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
