package com.solegendary.reignofnether.units.monsters;

import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.units.Unit;
import com.solegendary.reignofnether.units.goals.MoveToCursorBlockGoal;
import com.solegendary.reignofnether.units.goals.RangedBowAttackUnitGoal;
import com.solegendary.reignofnether.units.goals.SelectedTargetGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SkeletonUnit extends Skeleton implements Unit {

    public SkeletonUnit(EntityType<? extends Skeleton> p_33570_, Level p_33571_) {
        super(p_33570_, p_33571_);
    }

    // region
    public List<AbilityButton> getAbilities() {return abilities;};

    public MoveToCursorBlockGoal getMoveGoal() {return moveGoal;}
    public void setMoveGoal(MoveToCursorBlockGoal moveGoal) {this.moveGoal = moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public void setTargetGoal(SelectedTargetGoal<? extends LivingEntity> targetGoal) {this.targetGoal = targetGoal;}

    public MoveToCursorBlockGoal moveGoal;
    public SelectedTargetGoal<? extends LivingEntity> targetGoal;

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
            SynchedEntityData.defineId(SkeletonUnit.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
    }

    // combat stats
    public boolean getWillRetaliate() {return willRetaliate;}
    public int getAttackCooldown() {return attackCooldown;}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle;}
    public float getAttackRange() {return attackRange;}
    public float getSpeedModifier() {return speedModifier;}

    public void setAttackMoveTarget(@Nullable BlockPos bp) { this.attackMoveTarget = bp; }
    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    final public float speedModifier = 1.0f;
    final public float attackRange = 10.0F; // only used by ranged units
    final public int attackCooldown = 45;
    final public float aggroRange = 10;
    final public boolean willRetaliate = true; // will attack when hurt by an enemy
    final public boolean aggressiveWhenIdle = false;

    public RangedBowAttackUnitGoal<? extends LivingEntity> attackGoal;

    private static final List<AbilityButton> abilities = new ArrayList<>();

    public void tick() {
        super.tick();
        Unit.tick(this);

        // need to do this outside the goal so it ticks down while not attacking
        // only needed for attack goals created by reignofnether like RangedBowAttackUnitGoal
        if (attackGoal != null)
            attackGoal.tickCooldown();
    }

    @Override
    protected void registerGoals() {
        this.moveGoal = new MoveToCursorBlockGoal(this, speedModifier);
        this.targetGoal = new SelectedTargetGoal(this, true, false);
        this.attackGoal = new RangedBowAttackUnitGoal(this, 5, attackCooldown, attackRange);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, moveGoal);
        this.goalSelector.addGoal(3, attackGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(3, targetGoal);
    }
}
