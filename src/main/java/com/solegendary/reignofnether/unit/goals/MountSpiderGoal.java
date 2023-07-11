package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import com.solegendary.reignofnether.unit.units.villagers.WitchUnit;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.phys.Vec3;

public class MountSpiderGoal extends MoveToTargetBlockGoal {

    private SpiderUnit targetSpider = null;
    private Ability ability; // used for syncing cooldown with clientside

    public MountSpiderGoal(PathfinderMob mob) {
        super(mob, false, 1.0f, 0);
    }

    // set the target to throw a potion - the witch will move towards this location until we're within range
    // then throw a potion at it
    // if we set an entity target, on every tick we will follow that target
    // if we set a BlockPos as the target, remove any entity target
    public void setTarget(SpiderUnit spider) {
        this.targetSpider = spider;
    }

    @Override
    public void tick() {
        if (this.targetSpider != null)
            this.setMoveTarget(this.targetSpider.getOnPos());

        if (moveTarget != null) {

            if (MyMath.distance(
                    this.mob.getX(), this.mob.getZ(),
                    moveTarget.getX(), moveTarget.getZ()) <= 3) {

                if (moveTarget != null)
                    this.mob.startRiding(targetSpider);
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
