package com.solegendary.reignofnether.minimap;

import com.solegendary.reignofnether.ping.PingServerEvents;
import com.solegendary.reignofnether.ping.PingType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// a minimap marker or a ping from the ping wheel; PingServerEvents shows it to allies
public class MapMarkerServerboundPacket {
    private final int x;
    private final int y;
    private final int z;
    private final PingType type;

    public MapMarkerServerboundPacket(int x, int y, int z, PingType type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
    }

    public MapMarkerServerboundPacket(FriendlyByteBuf buffer) {
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
        this.type = buffer.readEnum(PingType.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.x);
        buffer.writeInt(this.y);
        buffer.writeInt(this.z);
        buffer.writeEnum(this.type);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null)
                PingServerEvents.ping(player.getName().getString(), type, new BlockPos(x, y, z));
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
