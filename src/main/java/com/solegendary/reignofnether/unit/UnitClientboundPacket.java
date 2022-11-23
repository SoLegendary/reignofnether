package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitClientboundPacket {

    private final UnitSyncAction action;
    private final int entityId;
    private final float health;
    private final double posX;
    private final double posY;
    private final double posZ;

    public static void sendLeavePacket(LivingEntity entity) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new UnitClientboundPacket(UnitSyncAction.LEAVE_LEVEL,
                entity.getId(),0,0,0,0)
        );
    }

    public static void sendSyncPacket(LivingEntity entity) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new UnitClientboundPacket(UnitSyncAction.SYNC,
                entity.getId(),
                entity.getHealth(),
                entity.getX(), entity.getY(), entity.getZ())
        );
    }

    // packet-handler functions
    public UnitClientboundPacket(
        UnitSyncAction action,
        int unitId,
        float health,
        double posX,
        double posY,
        double posZ
    ) {
        // filter out non-owned entities so we can't control them
        this.action = action;
        this.entityId = unitId;
        this.health = health;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public UnitClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(UnitSyncAction.class);
        this.entityId = buffer.readInt();
        this.health = buffer.readFloat();
        this.posX = buffer.readDouble();
        this.posY = buffer.readDouble();
        this.posZ = buffer.readDouble();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeInt(this.entityId);
        buffer.writeFloat(this.health);
        buffer.writeDouble(this.posX);
        buffer.writeDouble(this.posY);
        buffer.writeDouble(this.posZ);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    switch (this.action) {
                        case LEAVE_LEVEL -> UnitClientEvents.onEntityLeave(this.entityId);
                        case SYNC -> UnitClientEvents.syncUnitData(this.entityId, this.health, new Vec3(this.posX, this.posY, this.posZ));
                    }
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
