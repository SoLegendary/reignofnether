package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class UnitSyncMobEffectsClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<UnitSyncMobEffectsClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "unit_sync_mob_effects_clientbound"));

    @Override
    public CustomPacketPayload.Type<UnitSyncMobEffectsClientboundPacket> type() {
        return TYPE;
    }

    private final int entityId;
    private final int effectId;
    private final int amplifier;
    private final int duration;

    public static void addEffectClientside(LivingEntity entity, MobEffectInstance mei) {
        PacketHandler.send(PacketHandler.allPlayers(),
                // 1.21.1's Registry#getId takes the registered value, and effects are handed out
                // as holders now, so unwrap before looking up the numeric id.
                new UnitSyncMobEffectsClientboundPacket(entity.getId(), BuiltInRegistries.MOB_EFFECT.getId(mei.getEffect().value()), mei.getAmplifier(), mei.getDuration())
        );
    }

    public static void removeEffectClientside(LivingEntity entity, Holder<MobEffect> me) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new UnitSyncMobEffectsClientboundPacket(entity.getId(), BuiltInRegistries.MOB_EFFECT.getId(me.value()), 0, 0)
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

    public UnitSyncMobEffectsClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.effectId = buffer.readInt();
        this.amplifier = buffer.readInt();
        this.duration = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.entityId);
        buffer.writeInt(this.effectId);
        buffer.writeInt(this.amplifier);
        buffer.writeInt(this.duration);
    }

    // client-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    // rest is handled by MobEffectEvent.Added event
                    UnitClientEvents.syncMobEffect(this.entityId, this.effectId, this.amplifier, this.duration);
                });
        });
        return;
    }
}
