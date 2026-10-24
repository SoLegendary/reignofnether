package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.unit.goals.RangedAttackGroundGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

public interface RangedAttackerUnit {

    int FOG_REVEAL_TICKS_MAX = 60;

    // multishot extras (fired at an angle) carry this tag; AbstractArrowMixin halves their damage
    String MULTISHOT_ARROW_TAG = "ron_multishot_arrow";

    // call from any unit's crossbow shooting override so that its multishot extras get tagged
    static void tagIfMultishot(Projectile projectile, float projectileAngle) {
        if (projectileAngle != 0.0F)
            projectile.addTag(MULTISHOT_ARROW_TAG);
    }

    int getFogRevealDuration();
    void setFogRevealDuration(int duration);

    default RangedAttackGroundGoal<?> getRangedAttackGroundGoal() { return null; }

    default void performUnitRangedAttack(LivingEntity pTarget, float velocity) {
        double x = pTarget.getX();
        double y = pTarget.getY();
        double z = pTarget.getZ();
        performUnitRangedAttack(x, y, z, velocity);
    }

    // attack ground
    default void performUnitRangedAttack(double x, double y, double z, float velocity) { }
}
