package com.solegendary.reignofnether.soldier;

import com.solegendary.reignofnether.ping.PingType;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.function.Supplier;

// everything the soldier HUD needs that the client can't work out itself; sent only to that soldier
public class SoldierStateClientboundPacket {

    final boolean active;
    final boolean dead;
    final int respawnSeconds;
    final String killer;
    final String respawnBuilding;
    @Nullable final PingType order;
    final BlockPos orderPos;
    final int orderAgeSeconds;

    public static void send(ServerPlayer player, SoldierStateClientboundPacket packet) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void clear(ServerPlayer player) {
        send(player, new SoldierStateClientboundPacket(false, false, 0, "", "", null, BlockPos.ZERO, 0));
    }

    public SoldierStateClientboundPacket(boolean active, boolean dead, int respawnSeconds, String killer,
                                         String respawnBuilding, @Nullable PingType order, BlockPos orderPos, int orderAgeSeconds) {
        this.active = active;
        this.dead = dead;
        this.respawnSeconds = respawnSeconds;
        this.killer = killer;
        this.respawnBuilding = respawnBuilding;
        this.order = order;
        this.orderPos = orderPos;
        this.orderAgeSeconds = orderAgeSeconds;
    }

    public SoldierStateClientboundPacket(FriendlyByteBuf buffer) {
        this.active = buffer.readBoolean();
        this.dead = buffer.readBoolean();
        this.respawnSeconds = buffer.readInt();
        this.killer = buffer.readUtf();
        this.respawnBuilding = buffer.readUtf();
        this.order = buffer.readBoolean() ? buffer.readEnum(PingType.class) : null;
        this.orderPos = buffer.readBlockPos();
        this.orderAgeSeconds = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeBoolean(dead);
        buffer.writeInt(respawnSeconds);
        buffer.writeUtf(killer);
        buffer.writeUtf(respawnBuilding);
        buffer.writeBoolean(order != null);
        if (order != null)
            buffer.writeEnum(order);
        buffer.writeBlockPos(orderPos);
        buffer.writeInt(orderAgeSeconds);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SoldierClient.update(this)));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
