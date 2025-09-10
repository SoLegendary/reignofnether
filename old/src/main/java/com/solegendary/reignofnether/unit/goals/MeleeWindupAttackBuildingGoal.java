package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import net.minecraft.world.entity.Mob;

public class MeleeWindupAttackBuildingGoal extends MeleeAttackBuildingGoal {

    private final int windupTicksMax;
    private int windupTicksLeft;

    public MeleeWindupAttackBuildingGoal(Mob mob, int windupTicksMax) {
        super(mob);
        this.windupTicksMax = windupTicksMax;
        this.windupTicksLeft = windupTicksMax;
    }

    @Override
    public void tick() {
        super.tick();
        if (isAttacking() && ticksToNextBlockBreak <= 0 && windupTicksLeft > 0) {
            if (windupTicksLeft == windupTicksMax &&
                    mob instanceof KeyframeAnimated && !mob.level().isClientSide()) {
                UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.ATTACK_UNIT, mob);
            }
            windupTicksLeft -= 1;
        }
    }

    @Override
    public void doBuildingAttack() {
        if (windupTicksLeft <= 0) {
            super.doBuildingAttack();
            windupTicksLeft = windupTicksMax;
        }
    }
}
