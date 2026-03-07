package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.modelling.animations.EnchanterAnimations;
import com.solegendary.reignofnether.unit.units.monsters.NecromancerUnit;
import com.solegendary.reignofnether.unit.units.monsters.WretchedWraithUnit;
import com.solegendary.reignofnether.unit.units.piglins.MarauderUnit;
import com.solegendary.reignofnether.unit.units.villagers.EnchanterUnit;
import net.minecraft.core.BlockPos;
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
    private final BlockPos pos;

    private static void setServersideCooldown(int unitId, UnitAction unitAction, float cooldown) {
        for (LivingEntity entity : UnitServerEvents.getAllUnits())
            if (entity.getId() == unitId && entity instanceof Unit unit)
                for (Ability ability : unit.getAbilities().get())
                    if (ability.action == unitAction) {
                        ability.setCooldown(cooldown, unit);
                        return;
                    }
    }

    public static void sendSetCooldownPacket(int unitId, UnitAction unitAction, float cooldown) {
        setServersideCooldown(unitId, unitAction, cooldown);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new AbilityClientboundPacket(unitId, true, unitAction, cooldown, new BlockPos(0,0,0))
        );
    }

    public static void doAbility(int unitId, UnitAction unitAction, float value) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new AbilityClientboundPacket(unitId, false, unitAction, value, new BlockPos(0,0,0))
        );
    }

    public static void doAbility(int unitId, UnitAction unitAction, float value, BlockPos pos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new AbilityClientboundPacket(unitId, false, unitAction, value, pos)
        );
    }

    public AbilityClientboundPacket(
        int unitId,
        boolean isSettingCooldown,
        UnitAction unitAction,
        float value,
        BlockPos pos
    ) {
        this.unitId = unitId;
        this.isSettingCooldown = isSettingCooldown;
        this.unitAction = unitAction;
        this.value = value;
        this.pos = pos;
    }

    public AbilityClientboundPacket(FriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.isSettingCooldown = buffer.readBoolean();
        this.unitAction = buffer.readEnum(UnitAction.class);
        this.value = buffer.readFloat();
        this.pos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        buffer.writeBoolean(this.isSettingCooldown);
        buffer.writeEnum(this.unitAction);
        buffer.writeFloat(this.value);
        buffer.writeBlockPos(this.pos);
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
                        for (Ability ability : unit.getAbilities().get()) {
                            if (ability.action == this.unitAction) {
                                ability.setCooldown(this.value, unit);
                                return;
                            }
                        }
                    }
                    if (this.unitAction == UnitAction.BLOOD_MOON) {
                        TimeClientEvents.setBloodMoonTicks((int) value, this.pos);
                    } else if (this.unitAction == UnitAction.SOUL_SIPHON_UPDATE) {
                        if (unit instanceof NecromancerUnit necromancer) {
                            necromancer.souls = (int) value;
                            necromancer.updateAbilityButtons();
                        }
                    } else if (this.unitAction == UnitAction.SET_ATTACK_COUNT) {
                        if (unit instanceof MarauderUnit marauderUnit) {
                            marauderUnit.attacksToNextBigHit = (int) value;
                        }
                    } else if (this.unitAction == UnitAction.MARCH_OF_PROGRESS_SET) {
                        boolean enable = value == 1f;
                        if (unit instanceof EnchanterUnit enchanterUnit) {
                            enchanterUnit.auraEnabled = enable;
                            if (enable) {
                                enchanterUnit.playSingleAnimation(UnitAnimationAction.ULTIMATE);
                            }
                        }
                    } else if (this.unitAction == UnitAction.BLIZZARD) {
                        if (unit instanceof WretchedWraithUnit wraithUnit) {
                            wraithUnit.blizzard();
                        }
                    }
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
