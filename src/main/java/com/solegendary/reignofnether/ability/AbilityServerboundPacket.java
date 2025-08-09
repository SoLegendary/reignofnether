package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class AbilityServerboundPacket {

    private final int unitId;
    private final UnitAction unitAction;

    public static void rankUpAbility(int unitId, UnitAction abilityAction) {
        PacketHandler.INSTANCE.sendToServer(new AbilityServerboundPacket(unitId, abilityAction));
    }

    public AbilityServerboundPacket(
        int unitId,
        UnitAction unitAction
    ) {
        this.unitId = unitId;
        this.unitAction = unitAction;
    }

    public AbilityServerboundPacket(FriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.unitAction = buffer.readEnum(UnitAction.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        buffer.writeEnum(this.unitAction);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("AbilityServerboundPacket: Sender was null");
                success.set(false);
                return;
            }
            for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
                if (entity.getId() == this.unitId && entity instanceof Unit unit) {

                    if (!player.getName().getString().equals(unit.getOwnerName())) {
                        ReignOfNether.LOGGER.warn("AbilityServerboundPacket: Tried to process packet from " + player.getName() + " for: " + unit.getOwnerName());
                        success.set(false);
                        return;
                    }

                    for (Ability ability : unit.getAbilities().get()) {
                        if (ability.action == this.unitAction && ability instanceof HeroAbility heroAbility && unit instanceof HeroUnit hero) {
                            heroAbility.rankUp(hero);
                        }
                    }
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
