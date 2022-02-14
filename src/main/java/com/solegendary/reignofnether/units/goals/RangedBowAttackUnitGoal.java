package com.solegendary.reignofnether.units.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;

import java.util.EnumSet;

// modified version of RangedBowAttackGoal which:
// - doesn't strafe
// - stops when the target is dead
// - does not move towards a target if already in attack range and can see the target
// - faces the target
// - does not require the user to wind up bow attacks, instead using RTS-like attack cooldowns
// TODO: prevent friendly fire

public class RangedBowAttackUnitGoal<T extends net.minecraft.world.entity.Mob & RangedAttackMob> extends Goal {
    private final T mob;
    private int attackWindupTime; // time to wind up a bow attack
    private int attackCooldownMax;
    private int attackCooldown = 0; // time to wait between bow windups
    private final float attackRadiusSqr;
    private int attackTime = -1;
    private int seeTime; // how long we have seen the target for

    public RangedBowAttackUnitGoal(T mob, int attackWindupTime, int attackCooldown, float attackRadius) {
        this.mob = mob;
        this.attackWindupTime = attackWindupTime;
        this.attackCooldownMax = attackCooldown;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public void tickCooldown() {
        if (this.attackCooldown > 0)
            this.attackCooldown -= 1;
    }

    public boolean canUse() { return this.mob.getTarget() != null && this.isHoldingBow(); }

    protected boolean isHoldingBow() {
        return this.mob.isHolding(is -> is.getItem() instanceof BowItem);
    }

    public boolean canContinueToUse() {
       return (this.canUse() || !this.mob.getNavigation().isDone()) &&
            this.isHoldingBow() &&
            this.mob.getTarget() != null &&
            this.mob.getTarget().isAlive();
    }

    public void start() {
        super.start();
        this.mob.setAggressive(true);
    }

    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.seeTime = 0;
        this.attackTime = -1;
        this.mob.stopUsingItem();
    }

    public void tick() {
        LivingEntity target = this.mob.getTarget();

        if (target != null && target.isAlive()) {

            this.mob.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());

            double distToTargetSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean canSeeTarget = this.mob.getSensing().hasLineOfSight(target);
            boolean flag = this.seeTime > 0;
            if (canSeeTarget != flag) {
                this.seeTime = 0;
            }

            if (canSeeTarget) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            // move towards the target until in range and target is visible
            if (distToTargetSqr > (double) this.attackRadiusSqr || !canSeeTarget) {
                this.mob.getNavigation().moveTo(target, 1.0f);
            } else {
                this.mob.getNavigation().stop();
            }

            if (this.mob.isUsingItem()) {
                if (!canSeeTarget && this.seeTime < -60) {
                    this.mob.stopUsingItem();
                }
                else if (canSeeTarget) { // start drawing bowstring
                    int i = this.mob.getTicksUsingItem();
                    if (i >= attackWindupTime && attackCooldown <= 0) {
                        this.mob.stopUsingItem();
                        this.mob.performRangedAttack(target, BowItem.getPowerForTime(i));
                        this.attackTime = this.attackWindupTime;
                        this.attackCooldown = this.attackCooldownMax;
                    }
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
                this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof BowItem));
            }
        }
    }
}
