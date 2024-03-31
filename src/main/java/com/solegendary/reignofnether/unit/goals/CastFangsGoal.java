package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.function.Consumer;

public class CastFangsGoal extends AbstractCastTargetedSpellGoal {

    public CastFangsGoal(Mob mob, int channelTicks, int range, Consumer<LivingEntity> onCast) {
        super(mob, channelTicks, range, false, onCast);
    }

    @Override
    public void startCasting() {
        super.startCasting();
        if (!this.mob.level.isClientSide())
            UnitSyncClientboundPacket.sendSyncAnimationPacket(this.mob, true);
    }

    @Override
    public void stopCasting() {
        super.stopCasting();
        if (!this.mob.level.isClientSide())
            UnitSyncClientboundPacket.sendSyncAnimationPacket(this.mob, false);
    }
}
