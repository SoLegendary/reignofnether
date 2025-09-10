package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ability.heroAbilities.monster.SoulSiphonPassive;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.NecromancerUnit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class AbilityClientboundPacket {

    private final int unitId;
    private final boolean isSettingCooldown;
    private final UnitAction unitAction;
    private final float value;

    private static void setServersideCooldown(int unitId, UnitAction unitAction, float cooldown) {
        for (LivingEntity entity : UnitServerEvents.getAllUnits())
            if (entity.getId() == unitId && entity instanceof Unit unit)
                for (Ability ability : unit.getAbilities())
                    if (ability.action == unitAction) {
                        ability.setCooldown(cooldown);
                        return;
                    }
    }

    public static void sendSetCooldownPacket(int unitId, UnitAction unitAction, float cooldown) {
        setServersideCooldown(unitId, unitAction, cooldown);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new AbilityClientboundPacket(unitId, true, unitAction, cooldown)
        );
    }

    public static void doAbility(int unitId, UnitAction unitAction, float value) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new AbilityClientboundPacket(unitId, false, unitAction, value)
        );
    }

    public AbilityClientboundPacket(
        int unitId,
        boolean isSettingCooldown,
        UnitAction unitAction,
        float value
    ) {
        this.unitId = unitId;
        this.isSettingCooldown = isSettingCooldown;
        this.unitAction = unitAction;
        this.value = value;
    }

    public AbilityClientboundPacket(FriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.isSettingCooldown = buffer.readBoolean();
        this.unitAction = buffer.readEnum(UnitAction.class);
        this.value = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        buffer.writeBoolean(this.isSettingCooldown);
        buffer.writeEnum(this.unitAction);
        buffer.writeFloat(this.value);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    Unit unit = null;
                    for (LivingEntity entity : UnitClientEvents.getAllUnits()) {
                        if (entity.getId() == this.unitId && entity instanceof Unit) {
                            unit = (Unit) entity;
                            break;
                        }
                    }
                    if (isSettingCooldown && unit != null) {
                        for (Ability ability : unit.getAbilities()) {
                            if (ability.action == this.unitAction) {
                                ability.setCooldown(this.value);
                                return;
                            }
                        }
                    }
                    if (this.unitAction == UnitAction.BLOOD_MOON) {
                        TimeClientEvents.setBloodMoonTicks((int) value, unit == null ? "" : unit.getOwnerName());
                    }
                    else if (this.unitAction == UnitAction.SOUL_SIPHON_UPDATE) {
                        if (unit instanceof NecromancerUnit necromancer) {
                            SoulSiphonPassive soulSiphon = necromancer.getSoulSiphon();
                            if (soulSiphon != null)
                                soulSiphon.souls = (int) value;
                            necromancer.updateAbilityButtons();
                        }
                    }
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
