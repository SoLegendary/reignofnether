package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class AbilityServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<AbilityServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "ability_serverbound"));

    @Override
    public CustomPacketPayload.Type<AbilityServerboundPacket> type() {
        return TYPE;
    }

    private final int unitId;
    private final UnitAction unitAction;

    public static void rankUpAbility(int unitId, UnitAction abilityAction) {
        PacketHandler.sendToServer(new AbilityServerboundPacket(unitId, abilityAction));
    }

    public AbilityServerboundPacket(
        int unitId,
        UnitAction unitAction
    ) {
        this.unitId = unitId;
        this.unitAction = unitAction;
    }

    public AbilityServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.unitAction = buffer.readEnum(UnitAction.class);
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        buffer.writeEnum(this.unitAction);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {

            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                ReignOfNether.LOGGER.warn("AbilityServerboundPacket: Sender was null");
                return;
            }
            for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
                if (entity.getId() == this.unitId && entity instanceof Unit unit) {

                    if (!player.getName().getString().equals(unit.getOwnerName())) {
                        ReignOfNether.LOGGER.warn("AbilityServerboundPacket: Tried to process packet from " + player.getName() + " for: " + unit.getOwnerName());
                        return;
                    }

                    for (Ability ability : unit.getAbilities().get()) {
                        if (ability.action == this.unitAction && ability instanceof HeroAbility heroAbility && unit instanceof HeroUnit hero) {
                            ReignOfNether.LOGGER.info("[Ability] {} ranked up ability {} on unit {}", player.getName(), this.unitAction, this.unitId);
                            heroAbility.rankUp(hero);
                        }
                    }
                }
            }
        });
        return;
    }
}
