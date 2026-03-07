package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;


public class RangedAttackBuildingGoal<T extends net.minecraft.world.entity.Mob> extends Goal {
    private final T mob;
    private BlockPos blockTarget = null;
    private UnitBowAttackGoal<?> bowAttackGoal = null;
    private UnitCrossbowAttackGoal<?> cbowAttackGoal = null;
    private BuildingPlacement buildingTarget = null;

    public RangedAttackBuildingGoal(T mob, UnitBowAttackGoal<?> bowAttackGoal) {
        this.mob = mob;
        this.bowAttackGoal = bowAttackGoal;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public RangedAttackBuildingGoal(T mob, UnitCrossbowAttackGoal<?> cbowAttackGoal) {
        this.mob = mob;
        this.cbowAttackGoal = cbowAttackGoal;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void setNextBlockTarget() {
        if (this.buildingTarget != null && !buildingTarget.getBlocks().isEmpty()) {
            Random rand = new Random();
            List<BuildingBlock> nonAirBlocks = new ArrayList<>();
            for (BuildingBlock b : buildingTarget.getBlocks()) {
                if (b.isPlaced(this.mob.level())) {
                    nonAirBlocks.add(b);
                }
            }

            int bound = nonAirBlocks.size();
            if (bound > 0) {
                int randIndex = rand.nextInt(bound);
                BuildingBlock block = nonAirBlocks.get(randIndex);
                this.blockTarget = block.getBlockPos();
            } else {
                this.blockTarget = null;
            }
        }
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
                    setNextBlockTarget();
                }
            }
            this.start();
        }
    }

    public void setBuildingTarget(BuildingPlacement building) {
        this.buildingTarget = building;
    }

    public BuildingPlacement getBuildingTarget() {
        return buildingTarget;
    }

    public boolean canUse() {
        return this.blockTarget != null;
    }

    public boolean canContinueToUse() {
        if (!this.canUse() && this.isDoneMoving())
            return false;
        return true;
    }

    public void start() {
        super.start();
        this.mob.setAggressive(true);
    }

    public void stop() {
        super.stop();
        blockTarget = null;
        buildingTarget = null;
        this.mob.setAggressive(false);
    }

    public void tick() {
        if (buildingTarget != null && buildingTarget.getBlocksPlaced() <= 0) {
            stop();
        }
        if (blockTarget != null) {
            float tx = blockTarget.getX() + 0.5f;
            float ty = blockTarget.getY() + 0.5f;
            float tz = blockTarget.getZ() + 0.5f;

            this.mob.getLookControl().setLookAt(tx, ty, tz);

            if (this.mob.level().isClientSide())
                return;

            float attackRange = ((AttackerUnit) this.mob).getAttackRange();

            double distToTarget = Math.sqrt(this.mob.distanceToSqr(tx, ty, tz));

            if ((distToTarget > attackRange - 1) &&
                !((Unit) this.mob).getHoldPosition()) {
                if (!mob.isPassenger())
                    this.moveTo(this.blockTarget);
            } else {
                if (!mob.isPassenger())
                    this.stopMoving();
            }
            if (distToTarget <= attackRange) { // start drawing bowstring
                if (bowAttackGoal != null) {
                    if (bowAttackGoal.getAttackCooldown() <= 0) {
                        if (mob instanceof RangedAttackerUnit rangedAttackerUnit) {
                            rangedAttackerUnit.performUnitRangedAttack(tx, ty, tz, 20);
                            if (!mob.level().isClientSide() && buildingTarget != null)
                                FogOfWarClientboundPacket.revealRangedUnit(buildingTarget.ownerName, mob.getId());
                        }
                        bowAttackGoal.setToMaxAttackCooldown();
                        setNextBlockTarget();
                    }
                }
            }
            // handle crossbow attacks (ie. pillagers attacking buildings) in UnitCrossbowAttackGoal
            // because we can't directly use mob.performUnitRangedAttack()
        }
    }

    // moveGoal controllers
    private boolean isDoneMoving() {
        Unit unit = (Unit) this.mob;
        if (unit.getMoveGoal() instanceof FlyingMoveToTargetGoal flyingMoveGoal)
            return flyingMoveGoal.isAtDestination();
        else
            return this.mob.getNavigation().isDone();
    }

    private void stopMoving() {
        Unit unit = (Unit) this.mob;
        if (unit.getMoveGoal() instanceof FlyingMoveToTargetGoal flyingMoveGoal)
            flyingMoveGoal.stopMoving();
        else
            this.mob.getNavigation().stop();
    }

    private void moveTo(BlockPos bp) {
        Unit unit = (Unit) this.mob;
        if (unit.getMoveGoal() instanceof FlyingMoveToTargetGoal flyingMoveGoal)
            flyingMoveGoal.setMoveTarget(bp);
        else
            this.mob.getNavigation().moveTo(bp.getX(), bp.getY(), bp.getZ(), 1.0f);
    }
}
