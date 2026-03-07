package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import com.solegendary.reignofnether.unit.units.monsters.WardenUnit;
import com.solegendary.reignofnether.unit.units.monsters.ZoglinUnit;
import com.solegendary.reignofnether.unit.units.piglins.HoglinUnit;
import com.solegendary.reignofnether.unit.units.piglins.MarauderUnit;
import com.solegendary.reignofnether.unit.units.villagers.IronGolemUnit;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;

import java.util.Random;

// Move towards a building to attack it
// will continually try to move towards the building if too far away as long as this goal is being enacted

// similar to BuildRepairGoal but to damage instead of repair
// unlike BuildRepairGoal the ticks and destroy logic is on the goal side since units have different damage and
// attack speed amounts stats the building is not damaged in unison

public class MeleeAttackBuildingGoal extends MoveToTargetBlockGoal {

    protected int ticksToNextBlockBreak = (int) ((AttackerUnit) mob).getAttackCooldown();
    protected BuildingPlacement buildingTarget;

    protected final int RECALC_COOLDOWN_MAX = 10;
    protected int recalcCooldown = 0; // limit start() used by canContinueToUse

    public MeleeAttackBuildingGoal(Mob mob) {
        super(mob, true, 0);
    }

    public void tick() {
        if (buildingTarget != null) {

            // for some reason, isDone() can sometimes be true even when moveTarget is nonnull and
            // we haven't reached the target, esp. for Brutes
            if (this.mob.getNavigation().isDone() && moveTarget != null &&
                this.mob.getOnPos().distSqr(moveTarget) > 1 && !isAttacking()) {
                if (recalcCooldown > 0) {
                    recalcCooldown -= 1;
                } else {
                    recalcCooldown = RECALC_COOLDOWN_MAX;
                    this.start();
                }
            }

            calcMoveTarget();
            if (buildingTarget.getBlocksPlaced() <= 0) {
                stopAttacking();
            }
            if (isAttacking()) {
                BlockPos bp = buildingTarget.centrePos;
                this.mob.getLookControl().setLookAt(bp.getX(), bp.getY(), bp.getZ());
                mob.getLookControl().lookAtCooldown = 20;

                // a unit with 1 attack damage @ 20 attack cd and building damage multiplier 1.0 will destroy a block once per second
                // if the damage multiplier leaves a fraction remainder, treat that as a chance to destroy an additional block
                // eg. if a unit with 3 damage attacks a building with 0.5 multiplier, always destroy 1 block + 50% chance to destroy 2 blocks
                ticksToNextBlockBreak -= 1;
                if (ticksToNextBlockBreak <= 0)
                    doBuildingAttack();
            }
        } else {
            this.moveTarget = null;
        }
    }

    protected void doBuildingAttack() {
        if (mob instanceof IronGolemUnit ||
                mob instanceof HoglinUnit ||
                mob instanceof ZoglinUnit ||
                mob instanceof RavagerUnit ||
                mob instanceof WardenUnit) {
            mob.handleEntityEvent((byte) 4);
            UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.NON_KEYFRAME_ATTACK, mob);
        }
        else
            this.mob.swing(InteractionHand.MAIN_HAND);

        AttackerUnit unit = (AttackerUnit) mob;
        ticksToNextBlockBreak = (int) unit.getAttackCooldown();
        double damageFloat = unit.getUnitAttackDamage() * buildingTarget.getMeleeDamageMult();
        damageFloat *= unit.getBuildingDamageMultiplier();

        double damageFloor = Math.floor(damageFloat);
        int damageInt = (int) damageFloor;
        if (new Random().nextDouble(1.0f) < damageFloat - damageFloor)
            damageInt += 1;
        buildingTarget.destroyRandomBlocks(damageInt);
        buildingTarget.lastAttacker = this.mob;

        if (unit instanceof MarauderUnit marauderUnit) {
            marauderUnit.decrementAttacks();
        }

        if (mob instanceof Slime slime && slime.onGround())
            slime.jumpFromGround();
    }

    private void calcMoveTarget() {
        if (this.buildingTarget != null)
            this.moveTarget = this.buildingTarget.getClosestGroundPos(mob.getOnPos(), 1);
    }

    // only count as building if in range of the target - building is actioned in Building.tick()
    public boolean isAttacking() {
        if (buildingTarget != null && this.moveTarget != null)
            return MiscUtil.isMobInRangeOfPos(moveTarget, mob, 2);
        return false;
    }

    public void setBuildingTarget(BlockPos blockPos) {
        if (blockPos != null) {
            if (this.mob.level().isClientSide()) {
                BuildingPlacement b = BuildingUtils.findBuilding(this.mob.level().isClientSide(), blockPos);
                if (b != null && !b.getBuilding().invulnerable) {
                    this.buildingTarget = b;
                    MiscUtil.addUnitCheckpoint(((Unit) mob), new BlockPos(
                            buildingTarget.centrePos.getX(),
                            buildingTarget.originPos.getY() + 1,
                            buildingTarget.centrePos.getZ()),
                            false
                    );
                }
            }
            else {
                BuildingPlacement b = BuildingUtils.findBuilding(this.mob.level().isClientSide(), blockPos);
                if (b != null && !b.getBuilding().invulnerable) {
                    this.buildingTarget = b;
                    if (this.mob.isVehicle() && this.mob.getFirstPassenger() instanceof AttackerUnit aUnit &&
                            aUnit.getAttackBuildingGoal() instanceof RangedAttackBuildingGoal<?> rabg)
                        rabg.setBuildingTarget(this.buildingTarget);
                }
            }
            calcMoveTarget();
            this.start();
        }
    }

    public BuildingPlacement getBuildingTarget() { return buildingTarget; }

    // if we override stop() it for some reason is called after start() and we can never begin this goal...
    public void stopAttacking() {
        buildingTarget = null;
        super.stopMoving();
    }
}
