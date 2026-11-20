package com.solegendary.reignofnether.replay;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A player's RTS camera every few ticks, only for the replay (camera follow in playback). */
public class ReplayCameraServerboundPacket {

    private final float x, z, zoom, rot;

    public static void send(float x, float z, float zoom, float rot) {
        PacketHandler.INSTANCE.sendToServer(new ReplayCameraServerboundPacket(x, z, zoom, rot));
    }

    public ReplayCameraServerboundPacket(float x, float z, float zoom, float rot) {
        this.x = x;
        this.z = z;
        this.zoom = zoom;
        this.rot = rot;
    }

    public ReplayCameraServerboundPacket(FriendlyByteBuf buffer) {
        this(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(x);
        buffer.writeFloat(z);
        buffer.writeFloat(zoom);
        buffer.writeFloat(rot);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && Float.isFinite(x) && Float.isFinite(z) && Float.isFinite(zoom) && Float.isFinite(rot))
                ReplayServerEvents.onCamera(player, x, z, zoom, rot);
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
