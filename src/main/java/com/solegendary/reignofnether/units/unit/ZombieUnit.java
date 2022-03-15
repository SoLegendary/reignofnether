package com.solegendary.reignofnether.units.unit;

import com.solegendary.reignofnether.units.Unit;
import com.solegendary.reignofnether.units.goals.RangedBowAttackUnitGoal;
import com.solegendary.reignofnether.units.goals.SelectedTargetGoal;
import com.solegendary.reignofnether.units.goals.MoveToCursorBlockGoal;
import com.solegendary.reignofnether.units.goals.ZombieAttackUnitGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class ZombieUnit extends Zombie implements Unit {

    public ZombieUnit(EntityType<? extends Zombie> p_34271_, Level p_34272_) {
        super(p_34271_, p_34272_);
    }

    public MoveToCursorBlockGoal getMoveGoal() {return moveGoal;}
    public void setMoveGoal(MoveToCursorBlockGoal moveGoal) {this.moveGoal = moveGoal;}
    public SelectedTargetGoal getTargetGoal() {return targetGoal;}
    public void setTargetGoal(SelectedTargetGoal targetGoal) {this.targetGoal = targetGoal;}
    public ZombieAttackUnitGoal getAttackGoal() {return attackGoal;}
    public void setAttackGoal(ZombieAttackUnitGoal attackGoal) {this.attackGoal = attackGoal;}

    public MoveToCursorBlockGoal moveGoal;
    public SelectedTargetGoal targetGoal;
    public ZombieAttackUnitGoal attackGoal;

    // flags to not reset particular targets so we can persist them for specific actions
    public boolean getRetainAttackMoveTarget() {return retainAttackMoveTarget;}
    public void setRetainAttackMoveTarget(boolean retainAttackMoveTarget) {this.retainAttackMoveTarget = retainAttackMoveTarget;}
    public boolean getRetainAttackTarget() {return retainAttackTarget;}
    public void setRetainAttackTarget(boolean retainAttackTarget) {this.retainAttackTarget = retainAttackTarget;}
    public boolean getRetainMoveTarget() {return retainMoveTarget;}
    public void setRetainMoveTarget(boolean retainMoveTarget) {this.retainMoveTarget = retainMoveTarget;}
    public boolean getRetainFollowTarget() {return retainFollowTarget;}
    public void setRetainFollowTarget(boolean retainFollowTarget) {this.retainFollowTarget = retainFollowTarget;}
    public boolean getRetainHoldPosition() {return retainHoldPosition;}
    public void setRetainHoldPosition(boolean retainHoldPosition) {this.retainHoldPosition = retainHoldPosition;}

    boolean retainAttackMoveTarget = false;
    boolean retainAttackTarget = false;
    boolean retainMoveTarget = false;
    boolean retainFollowTarget = false;
    boolean retainHoldPosition = false;

    public BlockPos getAttackMoveTarget() { return attackMoveTarget; }
    public LivingEntity getFollowTarget() { return followTarget; }
    public boolean getHoldPosition() { return holdPosition; }
    public void setHoldPosition(boolean holdPosition) { this.holdPosition = holdPosition; }

    // if true causes moveGoal and attackGoal to work together to allow attack moving
    // moves to a block but will chase/attack nearby monsters in range up to a certain distance away
    private BlockPos attackMoveTarget = null;
    private LivingEntity followTarget = null; // if nonnull, continuously moves to the target
    private boolean holdPosition = false;

    // which player owns this unit? this format ensures its synched to client without having to use packets
    public String getOwnerName() { return this.entityData.get(ownerDataAccessor); }
    public void setOwnerName(String name) { this.entityData.set(ownerDataAccessor, name); }
    public static final EntityDataAccessor<String> ownerDataAccessor =
            SynchedEntityData.defineId(ZombieUnit.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
    }

    public void resetTargets() {
        if (!this.getRetainAttackMoveTarget())
            attackMoveTarget = null;
        if (!this.getRetainAttackTarget())
            targetGoal.setTarget(null);
        if (!this.getRetainMoveTarget())
            moveGoal.setMoveTarget(null);
        if (!this.getRetainFollowTarget())
            followTarget = null;
        if (!this.getRetainHoldPosition())
            holdPosition = false;
    }

    public void setMoveTarget(@Nullable BlockPos bp) {
        resetTargets();
        moveGoal.setMoveTarget(bp);
    }
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

    // combat stats
    public boolean getWillRetaliate() {return willRetaliate;}
    public int getAttackCooldown() {return attackCooldown;}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle;}
    public float getAttackRange() {return attackRange;}

    final public float attackRange = 0; // only used by ranged units
    final public int attackCooldown = 20;
    final public float aggroRange = 10;
    final public boolean willRetaliate = true; // will attack when hurt by an enemy
    final public boolean aggressiveWhenIdle = false;

    public void tick() {
        super.tick();
        Unit.tick(this);
    }

    @Override
    protected void registerGoals() {
        this.moveGoal = new MoveToCursorBlockGoal(this, 1.0f);
        this.targetGoal = new SelectedTargetGoal(this, true, true);
        this.attackGoal = new ZombieAttackUnitGoal(this, attackCooldown, 1.0D, false);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, moveGoal);
        this.goalSelector.addGoal(3, attackGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(3, targetGoal);
    }
}
