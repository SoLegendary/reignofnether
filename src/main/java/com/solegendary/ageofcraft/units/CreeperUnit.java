package com.solegendary.ageofcraft.units;

import com.solegendary.ageofcraft.units.goals.MoveToCursorBlockGoal;
import com.solegendary.ageofcraft.units.goals.SelectedTargetGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class CreeperUnit extends Creeper implements Unit {

    MoveToCursorBlockGoal moveGoal;
    SelectedTargetGoal targetGoal;

    private Boolean attackMoveFlag = false;
    private final float attackMoveRange = 5;
    private BlockPos attackMoveAnchor = null;
    private LivingEntity followTarget = null;

    public CreeperUnit(EntityType<? extends Creeper> p_32278_, Level p_32279_) { super(p_32278_, p_32279_); }

    @Override
    protected void registerGoals() {
        this.moveGoal = new MoveToCursorBlockGoal(this, 1.0f);
        this.targetGoal = new SelectedTargetGoal(this, true, true);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        // TODO: extend this to make it also activate on the spot on button press
        this.goalSelector.addGoal(2, new SwellGoal(this));
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        // TODO: this doesn't cause the creeper to move to the target before attacking
        this.targetSelector.addGoal(4, targetGoal);
    }

    public Boolean isAttackMoving() { return attackMoveFlag; }
    public float getAttackMoveRange() { return attackMoveRange; }
    public BlockPos getAttackMoveAnchor() { return attackMoveAnchor; }
    public LivingEntity getFollowTarget() { return followTarget; }

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

    public void setFollowTarget(@Nullable LivingEntity target) {
        this.followTarget = target;
    }
}
