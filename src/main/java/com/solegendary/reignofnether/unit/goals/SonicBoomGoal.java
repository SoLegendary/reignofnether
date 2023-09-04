package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.units.monsters.WardenUnit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.function.Consumer;

public class SonicBoomGoal extends AbstractCastTargetedSpellGoal {

    public SonicBoomGoal(PathfinderMob mob, int channelTicks, int range, Consumer<LivingEntity> onCast) {
        super(mob, channelTicks, range, false, onCast);
    }

    @Override
    public void startCasting() {
        super.startCasting();
        if (this.mob instanceof WardenUnit wardenUnit && this.mob.level.isClientSide())
            wardenUnit.sonicBoomAnimationState.start(wardenUnit.tickCount);
    }

    @Override
    public void stopCasting() {
        super.stopCasting();
        if (this.mob instanceof WardenUnit wardenUnit && this.mob.level.isClientSide())
            wardenUnit.sonicBoomAnimationState.stop();
    }

    @Override
    public void stop() {
        // hack fix to stop a weird bug where it gets stopped unexpectedly (serverside)
        // happens when needing to move towards the target first
        if (this.ticksCasting <= 2)
            return;
        super.stop();
    }
}
