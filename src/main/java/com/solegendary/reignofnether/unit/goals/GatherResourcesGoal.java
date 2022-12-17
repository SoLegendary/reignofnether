package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// Move towards the nearest open resource blocks and start gathering them
// Can be toggled between food, wood and ore, and disabled by clicking

public class GatherResourcesGoal extends MoveToTargetBlockGoal {

    private static final int REACH_RANGE = 5;
    private static final int DEFAULT_MAX_TICKS = 100; // actual ticks may be lower, depending on the ResourceBlock targeted
    private int ticksLeft = DEFAULT_MAX_TICKS;

    private final ArrayList<BlockPos> todoGatherTargets = new ArrayList<>();
    private BlockPos gatherTarget = null;
    private ResourceName targetResourceName = ResourceName.NONE; // if !None, will passively target blocks around it
    private ResourceBlock targetResourceBlock = null;
    private Building targetFarm = null;

    // whenever we attempt to assign a block as a target it must pass this test
    private final Predicate<BlockPos> BLOCK_CONDITION = bp -> {
        BlockState bs = mob.level.getBlockState(bp);
        BlockState bsAbove = mob.level.getBlockState(bp.above());
        ResourceBlock resBlock = ResourceBlocks.getResourceBlock(bp, mob.level);

        // is a valid resource block and meets the target ResourceBlock's blockstate condition
        if (resBlock == null || resBlock.resourceName != targetResourceName)
            return false;
        if (!resBlock.blockStateTest.test(bs))
            return false;

        // if the worker is farming, stick to only the assigned farm
        if (targetFarm != null && !targetFarm.isPosInsideBuilding(bp))
            return false;

        if (bs.getBlock() == Blocks.FARMLAND) {
            if (!bsAbove.isAir() || !canAffordReplant())
                return false;
        }
        // is not part of a building (unless farming)
        else if (targetFarm == null && BuildingUtils.isPosPartOfAnyBuilding(mob.level, bp, true))
            return false;

        // not covered by solid blocks
        boolean hasClearNeighbour = false;
        for (BlockPos adjBp : List.of(bp.north(), bp.south(), bp.east(), bp.west(), bp.above(), bp.below()))
            if (ResourceBlocks.CLEAR_MATERIALS.contains(mob.level.getBlockState(adjBp).getMaterial()))
                hasClearNeighbour = true;
        if (!hasClearNeighbour)
            return false;

        // not targeted by another worker
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof Unit unit) {
                if (unit instanceof WorkerUnit workerUnit && workerUnit.getGatherResourceGoal() != null && entity.getId() != this.mob.getId()) {
                    BlockPos otherUnitTarget = workerUnit.getGatherResourceGoal().getGatherTarget();
                    if (otherUnitTarget != null && otherUnitTarget.equals(bp)) {
                        System.out.println("Conflicting gather targets!");
                        return false;
                    }
                }
            }
        }
        return true;
    };

    // set move goal as range -1, so we aren't slightly out of range
    public GatherResourcesGoal(PathfinderMob mob, double speedModifier) {
        super(mob, true, speedModifier, REACH_RANGE - 1);
    }

    // move towards the targeted block and start gathering it
    public void tick() {

        if (gatherTarget == null && targetResourceName != ResourceName.NONE) {

            // prioritise gathering adjacent targets first
            todoGatherTargets.removeIf(bp -> !BLOCK_CONDITION.test(bp) || !isBlockInRange(bp));
            if (todoGatherTargets.size() > 0)
                gatherTarget = todoGatherTargets.get(0);

            if (gatherTarget == null)
                gatherTarget = MiscUtil.findNearestBlock(
                        mob.level,
                        new Vec3i(
                                mob.getEyePosition().x,
                                mob.getEyePosition().y,
                                mob.getEyePosition().z
                        ), REACH_RANGE,
                        BLOCK_CONDITION);

            if (gatherTarget != null)
                targetResourceBlock = ResourceBlocks.getResourceBlock(gatherTarget, mob.level);
        }

        if (gatherTarget != null) {
            this.setMoveTarget(gatherTarget);

            // if the block is no longer valid (destroyed or somehow badly targeted)
            if (!BLOCK_CONDITION.test(this.gatherTarget))
                removeGatherTarget();
            else // keep persistently moving towards the target
                this.setMoveTarget(gatherTarget);

            if (isGathering()) {
                mob.getLookControl().setLookAt(gatherTarget.getX(), gatherTarget.getY(), gatherTarget.getZ());
                if (!mob.level.isClientSide())
                {
                    // replant crops on empty farmland
                    if (mob.level.getBlockState(gatherTarget).getBlock() == Blocks.FARMLAND) {
                        ticksLeft -= 1;
                        ticksLeft = Math.min(ticksLeft, ResourceBlocks.REPLANT_TICKS_MAX);
                        if (ticksLeft <= 0) {
                            ticksLeft = DEFAULT_MAX_TICKS;

                            if (canAffordReplant()) {
                                ResourcesServerEvents.addSubtractResources(new Resources(((Unit) mob).getOwnerName(), 0, -ResourceCosts.REPLANT_WOOD_COST, 0));
                                mob.level.setBlockAndUpdate(gatherTarget.above(), ((WorkerUnit) mob).getReplantBlockState());
                                removeGatherTarget();
                            }
                        }
                    }
                    else {
                        ticksLeft -= 1;
                        ticksLeft = Math.min(ticksLeft, targetResourceBlock.ticksToGather);
                        if (ticksLeft <= 0) {
                            ticksLeft = DEFAULT_MAX_TICKS;
                            ResourceName resourceBlockType = ResourceBlocks.getResourceBlockName(this.gatherTarget, mob.level);
                            if (mob.level.destroyBlock(gatherTarget, false)) {

                                // prioritise gathering adjacent targets first
                                todoGatherTargets.remove(gatherTarget);
                                ArrayList<BlockPos> adjTarget = MiscUtil.findAdjacentBlocks(
                                    mob.level,
                                    gatherTarget,
                                    BLOCK_CONDITION
                                );
                                todoGatherTargets.addAll(adjTarget);

                                ResourcesServerEvents.addSubtractResources(new Resources(
                                        ((Unit) mob).getOwnerName(),
                                        resourceBlockType.equals(ResourceName.FOOD) ? targetResourceBlock.resourceValue : 0,
                                        resourceBlockType.equals(ResourceName.WOOD) ? targetResourceBlock.resourceValue : 0,
                                        resourceBlockType.equals(ResourceName.ORE) ? targetResourceBlock.resourceValue : 0
                                ));
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isBlockInRange(BlockPos target) {
        return Math.sqrt(target.distSqr(new Vec3i(mob.getX(), mob.getEyeY(), mob.getZ()))) <= REACH_RANGE;
    }

    // only count as gathering if in range of the target
    public boolean isGathering() {
        if (this.gatherTarget != null && this.targetResourceBlock != null &&
            ResourceBlocks.getResourceBlockName(this.gatherTarget, mob.level) != ResourceName.NONE)
            return isBlockInRange(gatherTarget);
        return false;
    }

    private boolean canAffordReplant() {
        return ResourcesServerEvents.canAfford(((Unit) mob).getOwnerName(), ResourceName.WOOD, ResourceCosts.REPLANT_WOOD_COST);
    }

    public void setTargetResourceName(ResourceName resourceName) {
        targetResourceName = resourceName;
    }

    public ResourceName getTargetResourceName() {
        return targetResourceName;
    }

    @Override
    public void setMoveTarget(@Nullable BlockPos bp) {
        super.setMoveTarget(bp);
        if (BLOCK_CONDITION.test(bp)) {
            this.gatherTarget = bp;
            this.targetResourceBlock = ResourceBlocks.getResourceBlock(gatherTarget, this.mob.level);
        }
    }

    // locks the worker to only gather from this specific building
    public void setTargetFarm(Building building) {
        this.targetFarm = building;
    }

    // stop attempting to gather the current target but continue searching
    public void removeGatherTarget() {
        gatherTarget = null;
        targetResourceBlock = null;
        ticksLeft = DEFAULT_MAX_TICKS;
    }

    // stop gathering and searching entirely
    public void stopGathering() {
        targetFarm = null;
        removeGatherTarget();
        this.setTargetResourceName(ResourceName.NONE);
        super.stopMoving();
    }

    public BlockPos getGatherTarget() {
        return gatherTarget;
    }
}
