package com.solegendary.ageofcraft.units;

import com.mojang.math.Vector3d;
import com.solegendary.ageofcraft.units.goals.MoveToCursorBlockGoal;
import com.solegendary.ageofcraft.units.goals.RangedBowAttackUnitGoal;
import com.solegendary.ageofcraft.units.goals.SelectedTargetGoal;
import com.solegendary.ageofcraft.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SkeletonUnit extends Skeleton implements Unit {

    MoveToCursorBlockGoal moveGoal;
    SelectedTargetGoal targetGoal;
    RangedBowAttackUnitGoal attackGoal;

    // if true causes moveGoal and attackGoal to work together to allow attack moving
    // moves to a block but will chase/attack nearby monsters in range up to a certain distance away
    private BlockPos attackMoveTarget = null;
    private BlockPos attackMoveAnchor = null; // pos marked after chasing a target on attack move to return to
    private LivingEntity followTarget = null; // if nonnull, continuously moves to the target
    private boolean holdPosition = false;

    // flags to not reset particular targets so we can persist them for specific actions
    boolean retainAttackMoveTarget = false;
    boolean retainAttackTarget = false;
    boolean retainMoveTarget = false;
    boolean retainFollowTarget = false;
    boolean retainHoldPosition = false;

    // combat stats
    private final float attackRange = 10.0F;
    private final int attackCooldown = 45;
    private final float aggroRange = 10;
    private final boolean willRetaliate = true; // will attack when hurt by an enemy, TODO: for workers, run if false

    public SkeletonUnit(EntityType<? extends Skeleton> p_33570_, Level p_33571_) {
        super(p_33570_, p_33571_);
    }

    public Boolean isAttackMoving() { return attackMoveTarget != null; }
    public Boolean isFollowing() { return followTarget != null; }

    public void tick() {
        super.tick();

        if (!this.level.isClientSide) {

            // TODO: target is being reassigned every tick and seems to cause spazzing out bug after the target dies

            if (this.attackGoal != null)
                attackGoal.tickCooldown();

            // no iframes after being damaged so multiple units can attack at once
            this.invulnerableTime = 0;

            // enact target-following, and stop followTarget being reset
            if (followTarget != null) {
                retainFollowTarget = true;
                setMoveTarget(followTarget.blockPosition());
                retainFollowTarget = false;
            }

            // enact attack moving - move to target but chase enemies, resuming move once dead or out of range/sight
            if (attackMoveTarget != null && !hasLivingTarget()) {
                retainAttackMoveTarget = true;
                boolean attacked = this.attackClosestEnemy((ServerLevel) this.level);
                if (!attacked && this.moveGoal.getMoveTarget() == null)
                    setMoveTarget(attackMoveTarget);
                retainAttackMoveTarget = false;
                if (!attacked && !this.moveGoal.canContinueToUse()) // finished attack-moving
                    resetTargets();
            }

            // retaliate against a mob that damaged us
            if (getLastDamageSource() != null && willRetaliate) {
                Entity lastDSEntity = getLastDamageSource().getEntity();
                if (lastDSEntity instanceof PathfinderMob &&
                        UnitCommonVanillaEvents.isUnitFriendly(lastDSEntity.getId()) &&
                        !hasLivingTarget())
                    this.setAttackTarget((PathfinderMob) lastDSEntity);
            }
            // enact aggression when idle TODO: doesn't seem to work?
            //if (isIdle()) {
            //    System.out.println(this.attackClosestEnemy((ServerLevel) this.level));
            //}

            // TODO: enact hold position
        }
    }

    // returns true and attacks the closest enemy OR
    // returns false and does nothing if none are found
    private boolean attackClosestEnemy(ServerLevel level) {

        List<PathfinderMob> nearbyMobs = MiscUtil.getEntitiesWithinRange(
                new Vector3d(this.position().x, this.position().y, this.position().z),
                aggroRange,
                PathfinderMob.class,
                level);

        List<PathfinderMob> nearbyUnfriendlyMobs = new ArrayList<>();

        for (PathfinderMob mob : nearbyMobs) {
            if (!UnitCommonVanillaEvents.isUnitFriendly(mob.getId()))
                nearbyUnfriendlyMobs.add(mob);
        }
        // sort by distance, then attack the closest one
        double closestDist = attackRange + 1;
        PathfinderMob closestMob = null;
        for (PathfinderMob mob : nearbyUnfriendlyMobs) {
            if (this.position().distanceTo(mob.position()) < closestDist) {
                closestDist = this.position().distanceTo(mob.position());
                closestMob = mob;
            }
        }
        if (closestMob != null && this.hasLineOfSight(closestMob)) {
            this.setAttackTarget(closestMob);
            return true;
        }
        return false;
    }

    @Override
    protected void registerGoals() {
        this.moveGoal = new MoveToCursorBlockGoal(this, 1.0f);
        this.targetGoal = new SelectedTargetGoal(this, true, false);
        this.attackGoal = new RangedBowAttackUnitGoal(this, 5, attackCooldown, attackRange);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, moveGoal);
        this.goalSelector.addGoal(3, attackGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(3, targetGoal);
    }

    public boolean isIdle() {
        return this.attackMoveTarget == null &&
                !hasLivingTarget() &&
                this.moveGoal.getMoveTarget() == null &&
                this.followTarget == null;
    }
    
    public boolean hasLivingTarget() {
        return this.getTarget() != null && this.getTarget().isAlive();
    }

    public void resetTargets() {
        if (!retainAttackMoveTarget)
            this.attackMoveTarget = null;
        if (!retainAttackTarget)
            targetGoal.setTarget(null);
        if (!retainMoveTarget)
            moveGoal.setMoveTarget(null);
        if (!retainFollowTarget)
            this.followTarget = null;
        if (!retainHoldPosition)
            this.holdPosition = false;
    }

    public void setMoveTarget(@Nullable BlockPos bp) {
        resetTargets();
        moveGoal.setMoveTarget(bp);
    }
    // target MUST be a serverside entity or it cannot be attacked
    public void setAttackTarget(@Nullable LivingEntity target) {
        resetTargets();
        targetGoal.setTarget(target);
    }
    public void setAttackMoveTarget(@Nullable BlockPos bp) {
        resetTargets();
        this.attackMoveTarget = bp;
    }
    public void setFollowTarget(@Nullable LivingEntity target) {
        resetTargets();
        this.followTarget = target;
    }
}
