package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitSyncMobEffectsClientboundPacket {

    private final int entityId;
    private final int effectId;
    private final int amplifier;
    private final int duration;

    public static void addEffectClientside(LivingEntity entity, MobEffectInstance mei) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncMobEffectsClientboundPacket(entity.getId(), MobEffect.getId(mei.getEffect()), mei.getAmplifier(), mei.getDuration())
        );
    }

    public static void removeEffectClientside(LivingEntity entity, MobEffect me) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncMobEffectsClientboundPacket(entity.getId(), MobEffect.getId(me), 0, 0)
        );
    }

    // packet-handler functions
    public UnitSyncMobEffectsClientboundPacket(
        int entityId,
        int descriptionId,
        int amplifier,
        int duration
    ) {
        this.entityId = entityId;
        this.effectId = descriptionId;
        this.amplifier = amplifier;
        this.duration = duration;
    }

    public UnitSyncMobEffectsClientboundPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.effectId = buffer.readInt();
        this.amplifier = buffer.readInt();
        this.duration = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.entityId);
        buffer.writeInt(this.effectId);
        buffer.writeInt(this.amplifier);
        buffer.writeInt(this.duration);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    UnitClientEvents.syncMobEffect(this.entityId, this.effectId, this.amplifier, this.duration);
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
