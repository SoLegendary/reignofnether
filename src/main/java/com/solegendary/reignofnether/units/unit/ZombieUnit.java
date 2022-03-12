package com.solegendary.reignofnether.units.unit;

import com.solegendary.reignofnether.units.Unit;
import com.solegendary.reignofnether.units.goals.SelectedTargetGoal;
import com.solegendary.reignofnether.units.goals.MoveToCursorBlockGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class ZombieUnit extends Zombie {

    MoveToCursorBlockGoal moveGoal;
    SelectedTargetGoal targetGoal;

    private BlockPos attackMoveTarget = null;
    private final float attackMoveRange = 5;
    private BlockPos attackMoveAnchor = null;
    private LivingEntity followTarget = null;

    // which player owns this unit?
    private String ownerName = "";

    public String getOwnerName() { return this.ownerName; }
    public void setOwnerName(String name) { this.ownerName = name; }

    public ZombieUnit(EntityType<? extends Zombie> p_34271_, Level p_34272_) {
        super(p_34271_, p_34272_);
    }

    @Override
    protected void registerGoals() {
        this.moveGoal = new MoveToCursorBlockGoal(this, 1.0f);
        this.targetGoal = new SelectedTargetGoal(this, true, true);

        // TODO: extend zombie goal to always null target on end
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, moveGoal);
        this.goalSelector.addGoal(3, new ZombieAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(3, targetGoal);
    }
}
