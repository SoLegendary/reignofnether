package com.solegendary.reignofnether.matchstart;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The host's RESUME on the saved game's resume screen. */
public class MatchResumeServerboundPacket {

    public static void resume() {
        PacketHandler.INSTANCE.sendToServer(new MatchResumeServerboundPacket());
    }

    public MatchResumeServerboundPacket() { }

    public MatchResumeServerboundPacket(FriendlyByteBuf buffer) { }

    public void encode(FriendlyByteBuf buffer) { }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null)
                MatchResumeServerEvents.resume(player); // checks the sender is the host
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
