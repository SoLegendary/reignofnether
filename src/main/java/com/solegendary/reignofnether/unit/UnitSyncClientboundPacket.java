package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.unit.units.interfaces.Unit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitSyncClientboundPacket {

    private final UnitSyncAction syncAction;
    private final int entityId;
    private final float health;
    private final double posX;
    private final double posY;
    private final double posZ;
    private final int food;
    private final int wood;
    private final int ore;

    public static void sendLeavePacket(LivingEntity entity) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new UnitSyncClientboundPacket(UnitSyncAction.LEAVE_LEVEL,
                entity.getId(),0,0,0,0,0,0,0)
        );
    }

    public static void sendSyncStatsPacket(LivingEntity entity) {
        boolean isBuilding = false;
        ResourceName gatherTarget = ResourceName.NONE;

        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncClientboundPacket(UnitSyncAction.SYNC_STATS,
                        entity.getId(),
                        entity.getHealth(),
                        entity.getX(), entity.getY(), entity.getZ(),
                        0,0,0)
        );
    }

    public static void sendSyncResourcesPacket(Unit unit) {
        Resources res = Resources.getTotalResourcesFromItems(unit.getItems());
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncClientboundPacket(UnitSyncAction.SYNC_RESOURCES,
                        ((LivingEntity) unit).getId(), 0,0,0,0,
                        res.food, res.wood, res.ore)
        );
    }

    // packet-handler functions
    public UnitSyncClientboundPacket(
        UnitSyncAction syncAction,
        int unitId,
        float health,
        double posX,
        double posY,
        double posZ,
        int food,
        int wood,
        int ore
    ) {
        // filter out non-owned entities so we can't control them
        this.syncAction = syncAction;
        this.entityId = unitId;
        this.health = health;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.food = food;
        this.wood = wood;
        this.ore = ore;
    }

    public UnitSyncClientboundPacket(FriendlyByteBuf buffer) {
        this.syncAction = buffer.readEnum(UnitSyncAction.class);
        this.entityId = buffer.readInt();
        this.health = buffer.readFloat();
        this.posX = buffer.readDouble();
        this.posY = buffer.readDouble();
        this.posZ = buffer.readDouble();
        this.food = buffer.readInt();
        this.wood = buffer.readInt();
        this.ore = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.syncAction);
        buffer.writeInt(this.entityId);
        buffer.writeFloat(this.health);
        buffer.writeDouble(this.posX);
        buffer.writeDouble(this.posY);
        buffer.writeDouble(this.posZ);
        buffer.writeInt(this.food);
        buffer.writeInt(this.wood);
        buffer.writeInt(this.ore);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    switch (this.syncAction) {
                        case LEAVE_LEVEL -> UnitClientEvents.onEntityLeave(this.entityId);
                        case SYNC_STATS -> UnitClientEvents.syncUnitStats(
                                this.entityId,
                                this.health,
                                new Vec3(this.posX, this.posY, this.posZ));
                        case SYNC_RESOURCES -> UnitClientEvents.syncUnitResources(
                                this.entityId,
                                new Resources("", this.food, this.wood, this.ore));
                    }
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
