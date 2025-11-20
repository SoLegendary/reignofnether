package com.solegendary.reignofnether.minimap;

import com.solegendary.reignofnether.minimap.MapMarkerClientboundPacket;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class MapMarkerServerboundPacket {
    private final int x;
    private final int z;

    public MapMarkerServerboundPacket(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public MapMarkerServerboundPacket(FriendlyByteBuf buffer) {
        this.x = buffer.readInt();
        this.z = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.x);
        buffer.writeInt(this.z);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // Broadcast to all players
                PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                        new MapMarkerClientboundPacket(x, z, player.getName().getString()));
                
                // Play notification sound for everyone
                SoundClientboundPacket.playSoundForAllPlayers(SoundAction.ALLY);
                success.set(true);
            }
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
