package com.solegendary.ageofcraft.units;

import com.solegendary.ageofcraft.units.goals.MoveToCursorBlockGoal;
import com.solegendary.ageofcraft.units.goals.RangedBowAttackModifiedGoal;
import com.solegendary.ageofcraft.units.goals.SelectedTargetGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class SkeletonUnit extends Skeleton implements Unit {

    MoveToCursorBlockGoal moveGoal;
    SelectedTargetGoal targetGoal;
    RangedBowAttackModifiedGoal attackGoal;

    // if true causes moveGoal and attackGoal to work together to allow attack moving
    // moves to a block but will chase/attack nearby monsters in range up to a certain distance away
    private Boolean attackMoveFlag = false;
    private final float attackMoveRange = 5; // range to chase before returning to move path
    private BlockPos attackMoveAnchor = null; // pos marked after chasing a target on attack move to return to

    public SkeletonUnit(EntityType<? extends Skeleton> p_33570_, Level p_33571_) {
        super(p_33570_, p_33571_);
    }

    public Boolean isAttackMoving() { return attackMoveFlag; }
    public float getAttackMoveRange() { return attackMoveRange; }
    public BlockPos getAttackMoveAnchor() { return attackMoveAnchor; }

    public void tick() {
        super.tick();
        if (this.attackGoal != null)
            attackGoal.tickCooldown();

        this.invulnerableTime = 0; // no iframes after being damaged
    }

    @Override
    protected void registerGoals() {
        this.moveGoal = new MoveToCursorBlockGoal(this, 1.0f);
        this.targetGoal = new SelectedTargetGoal(this, true, false);
        this.attackGoal = new RangedBowAttackModifiedGoal(this, 5, 45, 10.0F);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, moveGoal);
        this.goalSelector.addGoal(3, attackGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(3, targetGoal);
    }

    public void setMoveTarget(@Nullable BlockPos bp) {
        this.attackMoveFlag = false;
        targetGoal.setTarget(null);
        moveGoal.setMoveTarget(bp);
    }

    // target MUST be a serverside entity or else it cannot be attacked
    public void setAttackTarget(@Nullable LivingEntity target) {
        this.attackMoveFlag = false;
        moveGoal.setMoveTarget(null);
        targetGoal.setTarget(target);
    }

    public void setAttackMoveTarget(@Nullable BlockPos bp) {
        this.attackMoveFlag = true;
        targetGoal.setTarget(null);
        moveGoal.setMoveTarget(bp);
    }
}
