package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.unit.goals.RangedAttackGroundGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

public interface RangedAttackerUnit {

    int FOG_REVEAL_TICKS_MAX = 60;

    // multishot extras (fired at an angle) carry this tag; UnitServerEvents halves their damage
    String MULTISHOT_ARROW_TAG = "ron_multishot_arrow";

    // piercing arrows get this tag after damaging a target; UnitServerEvents halves their damage on any further targets
    String DEALT_DAMAGE_ARROW_TAG = "ron_dealt_damage_arrow";

    // multishot extras fired at an angle beyond this also get WIDE_MULTISHOT_ARROW_TAG; UnitServerEvents quarters their damage
    // (vanilla crossbows fire their extras at exactly +/-10 degrees; higher multishot levels add arrows beyond that)
    float WIDE_MULTISHOT_MIN_ANGLE = 10.0F;
    String WIDE_MULTISHOT_ARROW_TAG = "ron_wide_multishot_arrow";

    // arrows fired by a pillager while mounted on a ravager explode on impact. Tagged when fired so they still explode
    // if the pillager dismounts or dies before they land
    String EXPLOSIVE_ARROW_TAG = "ron_explosive_arrow";

    // explosive arrows fired in siege mode (primary arrow only) get a larger explosion and no direct hit damage
    String SIEGE_ARROW_TAG = "ron_siege_arrow";

    // arrows (eg. from pillagers on ravagers) get this tag after exploding so they can never explode again, eg. when piercing
    String EXPLODED_ARROW_TAG = "ron_exploded_arrow";

    // call from any unit's crossbow shooting override so that its multishot extras get tagged
    static void tagIfMultishot(Projectile projectile, float projectileAngle) {
        if (projectileAngle != 0.0F)
            projectile.addTag(MULTISHOT_ARROW_TAG);
        if (Math.abs(projectileAngle) > WIDE_MULTISHOT_MIN_ANGLE)
            projectile.addTag(WIDE_MULTISHOT_ARROW_TAG);
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
