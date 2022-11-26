package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.PathfinderMob;

import javax.annotation.Nullable;
import java.util.Random;

// Move towards a building to attack it
// will continually try to move towards the building if too far away as long as this goal is being enacted

// similar to BuildRepairGoal but to damage instead of repair
// unlike BuildRepairGoal the ticks and destroy logic is on the goal side since units have different damage and
// attack speed amounts stats the building is not damaged in unison

// TODO: add arm animations for specific models

public class AttackBuildingGoal extends MoveToTargetBlockGoal {

    private int ticksToNextBlockBreak = ((AttackerUnit) mob).getAttackCooldown();

    private Building buildingTarget;

    public AttackBuildingGoal(PathfinderMob mob, double speedModifier) {
        super(mob, true, speedModifier, 0);
    }

    public void tick() {
        if (buildingTarget != null) {
            calcMoveTarget();
            if (buildingTarget.getBlocksPlaced() <= 0) {
                stopAttacking();
            }
            if (isAttacking()) {
                BlockPos bp = BuildingUtils.getCentrePos(buildingTarget.getBlocks());
                this.mob.getLookControl().setLookAt(bp.getX(), bp.getY(), bp.getZ());

                // a unit with 1 attack damage @ 20 attack cd and building damage multiplier 1.0 will destroy a block once per second
                // if the damage multiplier leaves a fraction remainder, treat that as a chance to destroy an additional block
                // eg. if a unit with 3 damage attacks a building with 0.5 multiplier, always destroy 1 block + 50% chance to destroy 2 blocks
                ticksToNextBlockBreak -= 1;
                if (ticksToNextBlockBreak <= 0) {
                    AttackerUnit unit = (AttackerUnit) mob;
                    ticksToNextBlockBreak = unit.getAttackCooldown();
                    double damageFloat = unit.getAttackDamage() * buildingTarget.MELEE_DAMAGE_MULTIPLIER;
                    double damageFloor = Math.floor(damageFloat);
                    int damageInt = (int) damageFloor;
                    if (new Random().nextDouble(1.0f) < damageFloat - damageFloor)
                        damageInt += 1;
                    buildingTarget.destroyRandomBlocks(damageInt);
                    System.out.println(damageInt);
                    this.mob.playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR);
                }
            }
        }
        else
            this.moveTarget = null;
    }

    private void calcMoveTarget() {
        if (this.buildingTarget != null)
            this.moveTarget = this.buildingTarget.getClosestGroundPos(mob.getOnPos(), 0);
    }

    // only count as building if in range of the target - building is actioned in Building.tick()
    public boolean isAttacking() {
        if (buildingTarget != null && this.moveTarget != null)
            if (BuildingServerEvents.getUnitToBuildingRelationship((Unit) this.mob, buildingTarget) == Relationship.OWNED)
                return Math.sqrt(moveTarget.distSqr(new Vec3i(mob.getX(), mob.getY(), mob.getZ()))) < 2;
        return false;
    }

    public void setBuildingTarget(BlockPos blockPos) {
        if (blockPos != null) {
            if (this.mob.level.isClientSide())
                this.buildingTarget = BuildingUtils.findBuilding(BuildingClientEvents.getBuildings(), blockPos);
            else
                this.buildingTarget = BuildingUtils.findBuilding(BuildingServerEvents.getBuildings(), blockPos);
            calcMoveTarget();
            this.start();
        }
    }

    public Building getBuildingTarget() { return buildingTarget; }

    // if we override stop() it for some reason is called after start() and we can never begin this goal...
    public void stopAttacking() {
        buildingTarget = null;
        super.stopMoving();
    }
}
