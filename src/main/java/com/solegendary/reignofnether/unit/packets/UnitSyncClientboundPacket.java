package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitSyncAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
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

public class UnitSyncClientboundPacket {

    private final UnitSyncAction syncAction;
    private final int entityId;
    private final int targetId;
    private final float health;
    private final float absorb;
    private final double posX;
    private final double posY;
    private final double posZ;
    private final int food;
    private final int wood;
    private final int ore;
    private final String ownerName;

    public static void sendLeavePacket(LivingEntity entity) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncClientboundPacket(UnitSyncAction.LEAVE_LEVEL,
                        entity.getId(),0,0,0,0,0,0,0,0,0, "")
        );
    }

    public static void sendSyncOwnerNamePacket(Unit unit) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncClientboundPacket(UnitSyncAction.SYNC_OWNERNAME,
                        ((LivingEntity) unit).getId(),0,0,0,0,0,0,0,0,0, unit.getOwnerName())
        );
    }

    public static void sendSyncScenarioRoleIndexPacket(Unit unit) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncClientboundPacket(UnitSyncAction.SYNC_SCENARIO_ROLE_INDEX,
                        ((LivingEntity) unit).getId(), unit.getScenarioRoleIndex(),0,0,0,0,0,0,0,0, "")
        );
    }

    public static void sendSyncStatsPacket(LivingEntity entity) {
        boolean isBuilding = false;
        ResourceName gatherTarget = ResourceName.NONE;

        String owner = "";
        if (entity instanceof Unit unit)
            owner = unit.getOwnerName();

        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new UnitSyncClientboundPacket(UnitSyncAction.SYNC_STATS,
                entity.getId(), 0,
                entity.getHealth(),
                entity.getAbsorptionAmount(),
                entity.getX(), entity.getY(), entity.getZ(),
                0,0,0, owner)
        );
    }

    public static void sendSyncResourcesPacket(Unit unit) {
        Resources res = Resources.getTotalResourcesFromItems(unit.getItems());
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
            new UnitSyncClientboundPacket(UnitSyncAction.SYNC_RESOURCES,
                ((LivingEntity) unit).getId(), 0,0,0,0,0,0,
                res.food, res.wood, res.ore, "")
        );
    }

    public static void makeVillagerVeteran(LivingEntity entity) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncClientboundPacket(
                        UnitSyncAction.MAKE_VILLAGER_VETERAN,
                        entity.getId(), 0,
                        0, 0,0,0,0,0,0,0, "")
        );
    }

    public static void sendSyncAnchorPosPacket(LivingEntity entity, BlockPos bp) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncClientboundPacket(
                        UnitSyncAction.SYNC_ANCHOR_POS,
                        entity.getId(), 0,0,
                        0, bp.getX(), bp.getY(), bp.getZ(),0,0,0, "")
        );
    }

    public static void sendRemoveAnchorPosPacket(LivingEntity entity) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncClientboundPacket(
                        UnitSyncAction.SYNC_ANCHOR_POS,
                        entity.getId(), 0,0,
                        0,0,0,0,0,0,0, "")
        );
    }

    // packet-handler functions
    public UnitSyncClientboundPacket(
        UnitSyncAction syncAction,
        int unitId,
        int targetId,
        float health,
        float absorb,
        double posX,
        double posY,
        double posZ,
        int food,
        int wood,
        int ore,
        String ownerName
    ) {
        // filter out non-owned entities so we can't control them
        this.syncAction = syncAction;
        this.entityId = unitId;
        this.targetId = targetId;
        this.health = health;
        this.absorb = absorb;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.food = food;
        this.wood = wood;
        this.ore = ore;
        this.ownerName = ownerName;
    }

    public UnitSyncClientboundPacket(FriendlyByteBuf buffer) {
        this.syncAction = buffer.readEnum(UnitSyncAction.class);
        this.entityId = buffer.readInt();
        this.targetId = buffer.readInt();
        this.health = buffer.readFloat();
        this.absorb = buffer.readFloat();
        this.posX = buffer.readDouble();
        this.posY = buffer.readDouble();
        this.posZ = buffer.readDouble();
        this.food = buffer.readInt();
        this.wood = buffer.readInt();
        this.ore = buffer.readInt();
        this.ownerName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.syncAction);
        buffer.writeInt(this.entityId);
        buffer.writeInt(this.targetId);
        buffer.writeFloat(this.health);
        buffer.writeFloat(this.absorb);
        buffer.writeDouble(this.posX);
        buffer.writeDouble(this.posY);
        buffer.writeDouble(this.posZ);
        buffer.writeInt(this.food);
        buffer.writeInt(this.wood);
        buffer.writeInt(this.ore);
        buffer.writeUtf(this.ownerName);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    switch (this.syncAction) {
                        case LEAVE_LEVEL -> UnitClientEvents.onEntityLeave(this.entityId);
                        case SYNC_OWNERNAME -> UnitClientEvents.syncOwnerName(this.entityId, ownerName);
                        case SYNC_SCENARIO_ROLE_INDEX -> UnitClientEvents.syncScenarioRoleIndex(this.entityId, this.targetId);
                        case SYNC_STATS -> UnitClientEvents.syncUnitStats(
                                this.entityId,
                                this.health,
                                this.absorb,
                                new Vec3(this.posX, this.posY, this.posZ),
                                this.ownerName
                        );
                        case SYNC_RESOURCES -> UnitClientEvents.syncUnitResources(
                                this.entityId,
                                new Resources("", this.food, this.wood, this.ore)
                        );
                        case MAKE_VILLAGER_VETERAN -> UnitClientEvents.makeVillagerVeteran(this.entityId);
                        case SYNC_ANCHOR_POS -> UnitClientEvents.syncAnchorPos(
                                this.entityId,
                                new BlockPos((int) this.posX, (int) this.posY, (int) this.posZ)
                        );
                        case REMOVE_ANCHOR_POS -> UnitClientEvents.removeAnchorPos(this.entityId);
                    }
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
