package com.solegendary.reignofnether.replay;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** "Download replay" on the end screen: always the server's last finished match, never a path from the client. */
public class ReplayDownloadServerboundPacket {

    public static void request() {
        PacketHandler.INSTANCE.sendToServer(new ReplayDownloadServerboundPacket());
    }

    public ReplayDownloadServerboundPacket() { }

    public ReplayDownloadServerboundPacket(FriendlyByteBuf buffer) { }

    public void encode(FriendlyByteBuf buffer) { }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null)
                ReplayServerEvents.requestDownload(player);
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
