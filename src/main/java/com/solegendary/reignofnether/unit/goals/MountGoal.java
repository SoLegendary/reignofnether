package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

// allows a unit to be able to mount any target as long as their ability allows them to
public class MountGoal extends MoveToTargetBlockGoal {

    public static final float RANGE = 2;
    private LivingEntity targetEntity = null;

    public MountGoal(PathfinderMob mob) {
        super(mob, false, 1.0f, 0);
    }

    public void setTarget(LivingEntity entity) {
        this.targetEntity = entity;
    }

    @Override
    public void tick() {
        if (this.targetEntity != null)
            this.setMoveTarget(this.targetEntity.getOnPos());

        if (moveTarget != null &&
            this.mob instanceof Unit unit1 &&
            this.targetEntity instanceof Unit unit2) {
            if (MyMath.distance(
                    this.mob.getX(), this.mob.getZ(),
                    moveTarget.getX(), moveTarget.getZ()) <= RANGE &&
                unit1.getOwnerName().equals(unit2.getOwnerName())) {
                Unit.resetBehaviours(unit1);
                this.mob.startRiding(targetEntity);
                this.stop();
            }
        }
    }

    @Override
    public void stop() {
        this.stopMoving();
        this.setTarget(null);
    }
}
