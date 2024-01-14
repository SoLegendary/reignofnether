package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.unit.goals.RangedFlyingAttackGroundGoal;
import net.minecraft.world.entity.LivingEntity;

public interface RangedAttackerUnit {

    default RangedFlyingAttackGroundGoal<?> getRangedAttackGroundGoal() { return null; }

    default void performUnitRangedAttack(LivingEntity pTarget, float velocity) {
        double x = pTarget.getX();
        double y = pTarget.getY();
        double z = pTarget.getZ();
        performUnitRangedAttack(x, y, z, velocity);
    }

    default void performUnitRangedAttack(double x, double y, double z, float velocity) { }
}
