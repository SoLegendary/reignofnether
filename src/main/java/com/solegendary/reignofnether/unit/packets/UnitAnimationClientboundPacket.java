package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class UnitAnimationClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<UnitAnimationClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "unit_animation_clientbound"));

    @Override
    public CustomPacketPayload.Type<UnitAnimationClientboundPacket> type() {
        return TYPE;
    }

    private final UnitAnimationAction animAction;
    private final int entityId;
    private final int targetId;
    private final double posX;
    private final double posY;
    private final double posZ;

    // no targets
    public static void sendBasicPacket(UnitAnimationAction animAction, LivingEntity entity) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new UnitAnimationClientboundPacket(
                        animAction,
                        entity.getId(),
                        0,0,0,0
                )
        );
    }

    public static void sendEntityPacket(UnitAnimationAction animAction, LivingEntity entity, LivingEntity target) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new UnitAnimationClientboundPacket(
                        animAction,
                        entity.getId(), target.getId(),
                        0,0,0)
        );
    }

    public static void sendBlockPosPacket(UnitAnimationAction animAction, LivingEntity entity, BlockPos bp) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new UnitAnimationClientboundPacket(
                        animAction,
                        entity.getId(), 0,
                        bp.getX(), bp.getY(), bp.getZ())
        );
    }

    public static void sendEatFoodPacket(LivingEntity entity, int itemId) {
        PacketHandler.send(PacketHandler.allPlayers(),
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

    public UnitAnimationClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.animAction = buffer.readEnum(UnitAnimationAction.class);
        this.entityId = buffer.readInt();
        this.targetId = buffer.readInt();
        this.posX = buffer.readDouble();
        this.posY = buffer.readDouble();
        this.posZ = buffer.readDouble();

    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.animAction);
        buffer.writeInt(this.entityId);
        buffer.writeInt(this.targetId);
        buffer.writeDouble(this.posX);
        buffer.writeDouble(this.posY);
        buffer.writeDouble(this.posZ);
    }

    // client-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
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
        return;
    }
}
