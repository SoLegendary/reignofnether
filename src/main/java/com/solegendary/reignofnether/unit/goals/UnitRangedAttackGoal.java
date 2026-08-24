package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

// modified version of UnitBowAttackGoal which:
// - does not require a weapon
// - accepts an attack windup param
// - can start animations when the attack is winding up

public class UnitRangedAttackGoal<T extends net.minecraft.world.entity.Mob> extends Goal {

    private final int VELOCITY = 20;
    private static final int LOS_CHECK_INTERVAL = 5; // recompute raycast at most every N ticks

    private final T mob;
    private final int attackWindupTicksMax;
    private int attackWindupTicksLeft; // time to wind up an attack, should be matched to animation
    private int attackCooldown = 0; // time to wait between bow windups
    private int attackTime = -1;
    private int seeTime = 0; // how long we have seen the target for
    private int losCheckCooldown = 0; // throttle for hasLineOfSight raycast
    private boolean lastLineOfSight = false; // cached raycast result

    public UnitRangedAttackGoal(T mob, int attackWindupTime) {
        this.mob = mob;
        this.attackWindupTicksMax = attackWindupTime;
        this.attackWindupTicksLeft = attackWindupTime;
        setToMaxAttackCooldown();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void tickAttackCooldown() {
        if (this.attackCooldown > ((AttackerUnit) this.mob).getAttackCooldown())
            setToMaxAttackCooldown();
        if (this.attackCooldown > 0)
            this.attackCooldown -= 1;
    }

    public float getAttackCooldown() {
        return attackCooldown;
    }

    public void setToMaxAttackCooldown() {
        this.attackCooldown = (int) ((AttackerUnit) this.mob).getAttackCooldown();
    }

    public void resetCooldown() {
        this.attackCooldown = 0;
    }

    public boolean canUse() { return this.mob.getTarget() != null; }

    public boolean canContinueToUse() {
        Entity target = this.mob.getTarget();

        if (target == null || !target.isAlive())
            return false;
        if (!this.canUse() && this.isDoneMoving())
            return false;

        return true;
    }

    public void start() {
        super.start();
        this.mob.setAggressive(true);
        losCheckCooldown = 0; // force a fresh raycast for the new target
    }

    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.seeTime = 0;
        this.attackTime = -1;
        this.attackWindupTicksLeft = attackWindupTicksMax;
    }

    public void tick() {
        LivingEntity target = this.mob.getTarget();

        if (target != null && target.isAlive()) {
            this.mob.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());

            BuildingPlacement garrPlacement = GarrisonableBuildingAddon.getGarrison((Unit) this.mob);
            BuildingPlacement targetGarrPlacement = null;
            if (target instanceof Unit unit)
                targetGarrPlacement = GarrisonableBuildingAddon.getGarrison(unit);

            GarrisonableBuildingAddon garr = garrPlacement != null ? garrPlacement.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class) : null;
            GarrisonableBuildingAddon targetGarr = targetGarrPlacement != null ? targetGarrPlacement.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class) : null;
            boolean isGarrisoned = garr != null;
            boolean isTargetGarrisoned = targetGarr != null;

            if (losCheckCooldown <= 0) {
                lastLineOfSight = this.mob.getSensing().hasLineOfSight(target);
                losCheckCooldown = LOS_CHECK_INTERVAL;
            } else {
                losCheckCooldown -= 1;
            }
            boolean canSeeTarget = lastLineOfSight || isGarrisoned || isTargetGarrisoned;
            boolean flag = this.seeTime > 0;
            if (canSeeTarget != flag) {
                this.seeTime = 0;
            }
            if (canSeeTarget) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }
            float attackRange = ((AttackerUnit) this.mob).getAttackRange();

            if (isGarrisoned)
                attackRange = garr.getAttackRange();
            else if (isTargetGarrisoned)
                attackRange += targetGarr.getExternalAttackRangeBonus();

            double distToTarget = this.mob.distanceTo(target);

            // move towards the target until in range and target is visible
            // don't if the attacker is riding (eg. skeleton jockey) or it influences the vehicle movement
            // move to slightly closer than range so we can still chase and attack a moving target of the same speed
            if (!this.mob.isPassenger()) {
                if ((distToTarget > attackRange - 1 || !canSeeTarget) &&
                    !((Unit) this.mob).getHoldPosition()) {
                    this.moveTo(target);
                } else {
                    this.stopMoving();
                }
            }

            if (distToTarget <= attackRange && canSeeTarget && this.seeTime >= -60 && attackCooldown <= 0) {

                if (attackWindupTicksLeft == attackWindupTicksMax &&
                    mob instanceof KeyframeAnimated && !mob.level().isClientSide())
                    UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.ATTACK_UNIT, mob);

                attackWindupTicksLeft -= 1;

                if (attackWindupTicksLeft <= 0) {
                    this.attackWindupTicksLeft = attackWindupTicksMax;
                    this.setToMaxAttackCooldown();
                    if (mob instanceof RangedAttackerUnit rangedAttackerUnit)
                        rangedAttackerUnit.performUnitRangedAttack(target, VELOCITY);
                }
            } else {
                attackWindupTicksLeft = attackWindupTicksMax;
            }
        }
    }

    // moveGoal controllers
    private boolean isDoneMoving() {
        Unit unit = (Unit) this.mob;
        return this.mob.getNavigation().isDone();
    }

    private void stopMoving() {
        Unit unit = (Unit) this.mob;
        this.mob.getNavigation().stop();
    }

    private void moveTo(LivingEntity target) {
        Unit unit = (Unit) this.mob;
        this.mob.getNavigation().moveTo(target, 1.0f);
    }
}
