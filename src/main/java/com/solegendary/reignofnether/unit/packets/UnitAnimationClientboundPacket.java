package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitAnimationClientboundPacket {

    private final UnitAnimationAction animAction;
    private final int entityId;
    private final int targetId;
    private final double posX;
    private final double posY;
    private final double posZ;

    // no targets
    public static void sendBasicPacket(UnitAnimationAction animAction, LivingEntity entity) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitAnimationClientboundPacket(
                        animAction,
                        entity.getId(),
                        0,0,0,0
                )
        );
    }

    public static void sendEntityPacket(UnitAnimationAction animAction, LivingEntity entity, LivingEntity target) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitAnimationClientboundPacket(
                        animAction,
                        entity.getId(), target.getId(),
                        0,0,0)
        );
    }

    public static void sendBlockPosPacket(UnitAnimationAction animAction, LivingEntity entity, BlockPos bp) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitAnimationClientboundPacket(
                        animAction,
                        entity.getId(), 0,
                        bp.getX(), bp.getY(), bp.getZ())
        );
    }

    public static void sendEatFoodPacket(LivingEntity entity, int itemId) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitAnimationClientboundPacket(
                        UnitAnimationAction.EAT_FOOD_ITEM,
                        entity.getId(), itemId,
                        0,0,0)
        );
    }

    // packet-handler functions
    public UnitAnimationClientboundPacket(
        UnitAnimationAction animAction,
        int unitId,
        int targetId,
        double posX,
        double posY,
        double posZ
    ) {
        // filter out non-owned entities so we can't control them
        this.animAction = animAction;
        this.entityId = unitId;
        this.targetId = targetId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public UnitAnimationClientboundPacket(FriendlyByteBuf buffer) {
        this.animAction = buffer.readEnum(UnitAnimationAction.class);
        this.entityId = buffer.readInt();
        this.targetId = buffer.readInt();
        this.posX = buffer.readDouble();
        this.posY = buffer.readDouble();
        this.posZ = buffer.readDouble();

    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.animAction);
        buffer.writeInt(this.entityId);
        buffer.writeInt(this.targetId);
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
                    switch (this.animAction) {
                        case EAT_FOOD_ITEM -> UnitClientEvents.syncUnitEatingFood(this.entityId, this.targetId);
                        case NON_KEYFRAME_START -> UnitClientEvents.syncUnitAnimation(this.animAction, true,
                                this.entityId, this.targetId, new BlockPos((int) this.posX, (int) this.posY, (int) this.posZ));
                        case NON_KEYFRAME_STOP -> UnitClientEvents.syncUnitAnimation(this.animAction, false,
                                this.entityId, this.targetId, new BlockPos((int) this.posX, (int) this.posY, (int) this.posZ));
                        case NON_KEYFRAME_ATTACK -> UnitClientEvents.playAttackAnimation(this.entityId);
                        default -> UnitClientEvents.playKeyframeAnimation(this.animAction, this.entityId);
                    }
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
