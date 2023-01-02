package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.research.ResearchServer;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitClientboundPacket;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

// Move towards the nearest open resource blocks and start gathering them
// Can be toggled between food, wood and ore, and disabled by clicking

public class GatherResourcesGoal extends MoveToTargetBlockGoal {

    private static final int REACH_RANGE = 5;
    private static final int REACH_RANGE_SQR = 25;
    private static final int DEFAULT_MAX_GATHER_TICKS = 300; // ticks to gather blocks - actual ticks may be lower, depending on the ResourceSource targeted
    private int gatherTicksLeft = DEFAULT_MAX_GATHER_TICKS;
    private static final int MAX_SEARCH_CD_TICKS = 40; // while idle, worker will look for a new block once every this number of ticks (searching is expensive!)
    private int searchCdTicksLeft = 0;
    private int failedSearches = 0; // number of times we've failed to search for a new block - as this increases slow down or stop searching entirely to prevent lag
    private static final int MAX_FAILED_SEARCHES = 3;
    private static final int TICK_CD = 5; // only tick down gather time once this many ticks to reduce processing requirements
    private int cdTicksLeft = TICK_CD;
    private BlockPos altSearchPos = null; // block search origin that may be used instead of the mob position

    private final ArrayList<BlockPos> todoGatherTargets = new ArrayList<>();
    private BlockPos gatherTarget = null;
    private ResourceName targetResourceName = ResourceName.NONE; // if !None, will passively target blocks around it
    private ResourceSource targetResourceSource = null;
    private Building targetFarm = null;

    // saved copies of the above so we can later return to
    private final ArrayList<BlockPos> todoGatherTargetsSaved = new ArrayList<>();
    private BlockPos gatherTargetSaved = null;
    private ResourceName targetResourceNameSaved = ResourceName.NONE;
    private ResourceSource targetResourceSourceSaved = null;
    private Building targetFarmSaved = null;

    // whenever we attempt to assign a block as a target it must pass this test
    private final Predicate<BlockPos> BLOCK_CONDITION = bp -> {
        BlockState bs = mob.level.getBlockState(bp);
        BlockState bsAbove = mob.level.getBlockState(bp.above());
        ResourceSource resBlock = ResourceSources.getFromBlockPos(bp, mob.level);

        // is a valid resource block and meets the target ResourceSource's blockstate condition
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
        else if (targetFarm == null && BuildingUtils.isPosInsideAnyBuilding(mob.level, bp))
            return false;

        // not covered by solid blocks
        boolean hasClearNeighbour = false;
        for (BlockPos adjBp : List.of(bp.north(), bp.south(), bp.east(), bp.west(), bp.above(), bp.below()))
            if (ResourceSources.CLEAR_MATERIALS.contains(mob.level.getBlockState(adjBp).getMaterial()))
                hasClearNeighbour = true;
        if (!hasClearNeighbour)
            return false;

        // not targeted by another nearby worker
        AABB aabb = AABB.ofSize(this.mob.position(), REACH_RANGE * 2,REACH_RANGE * 2,REACH_RANGE * 2);
        for (LivingEntity entity : this.mob.level.getNearbyEntities(LivingEntity.class, TargetingConditions.forNonCombat(), this.mob, aabb)) {
            if (entity instanceof Unit unit) {
                if (unit instanceof WorkerUnit workerUnit && workerUnit.getGatherResourceGoal() != null && entity.getId() != this.mob.getId()) {
                    BlockPos otherUnitTarget = workerUnit.getGatherResourceGoal().getGatherTarget();
                    if (otherUnitTarget != null && otherUnitTarget.equals(bp)) {
                        altSearchPos = bp;
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
        if (this.mob.level.isClientSide())
            return;

        cdTicksLeft -= 1;
        if (cdTicksLeft <= 0)
            cdTicksLeft = TICK_CD;
        else
            return;

        if (gatherTarget == null && targetResourceName != ResourceName.NONE) {
            searchCdTicksLeft -= TICK_CD;

            // prioritise gathering adjacent targets first
            todoGatherTargets.removeIf(bp -> !BLOCK_CONDITION.test(bp));
            if (todoGatherTargets.size() > 0)
                gatherTarget = todoGatherTargets.get(0);

            if (gatherTarget == null && searchCdTicksLeft <= 0) {
                if (targetFarm != null) {
                    for (BuildingBlock block : targetFarm.getBlocks()) {
                        if (BLOCK_CONDITION.test(block.getBlockPos())) {
                            gatherTarget = block.getBlockPos();
                            break;
                        }
                    }
                }
                else {
                    Optional<BlockPos> bpOpt;
                    if (altSearchPos != null) {
                        bpOpt = BlockPos.findClosestMatch(
                                altSearchPos, REACH_RANGE/2, REACH_RANGE/2,
                            BLOCK_CONDITION);
                        altSearchPos = null;
                    }
                    else {
                        // increase search range until we've maxed out (to prevent idle workers using up too much CPU)
                        int range = REACH_RANGE * (failedSearches + 1);
                        if (failedSearches == MAX_FAILED_SEARCHES)
                            range = REACH_RANGE;

                        bpOpt = BlockPos.findClosestMatch(
                            new BlockPos(
                                mob.getEyePosition().x,
                                mob.getEyePosition().y,
                                mob.getEyePosition().z
                            ), range, range,
                            BLOCK_CONDITION);
                    }

                    bpOpt.ifPresentOrElse(
                        blockPos -> {
                            gatherTarget = blockPos;
                            failedSearches = 0;
                        },
                        () -> {
                            if (failedSearches < MAX_FAILED_SEARCHES)
                                failedSearches += 1;
                        }
                    );
                }
                searchCdTicksLeft = MAX_SEARCH_CD_TICKS * (failedSearches + 1);
            }
            if (gatherTarget != null)
                targetResourceSource = ResourceSources.getFromBlockPos(gatherTarget, mob.level);
        }

        if (gatherTarget != null) {

            // if the block is no longer valid (destroyed or somehow badly targeted)
            if (!BLOCK_CONDITION.test(this.gatherTarget))
                removeGatherTarget();
            else // keep persistently moving towards the target
                this.setMoveTarget(gatherTarget);

            if (isGathering()) {
                mob.getLookControl().setLookAt(gatherTarget.getX(), gatherTarget.getY(), gatherTarget.getZ());
                // replant crops on empty farmland
                if (mob.level.getBlockState(gatherTarget).getBlock() == Blocks.FARMLAND) {
                    gatherTicksLeft -= TICK_CD;
                    gatherTicksLeft = Math.min(gatherTicksLeft, ResourceSources.REPLANT_TICKS_MAX);
                    if (gatherTicksLeft <= 0) {
                        gatherTicksLeft = DEFAULT_MAX_GATHER_TICKS;

                        if (canAffordReplant()) {
                            ResourcesServerEvents.addSubtractResources(new Resources(((Unit) mob).getOwnerName(), 0, -ResourceCosts.REPLANT_WOOD_COST, 0));
                            mob.level.setBlockAndUpdate(gatherTarget.above(), ((WorkerUnit) mob).getReplantBlockState());
                            removeGatherTarget();
                        }
                    }
                }
                else {
                    if (ResearchServer.playerHasCheat(((Unit) mob).getOwnerName(), "operationcwal"))
                        this.gatherTicksLeft -= TICK_CD * 10;
                    else
                        this.gatherTicksLeft -= TICK_CD;

                    gatherTicksLeft = Math.min(gatherTicksLeft, targetResourceSource.ticksToGather);
                    if (gatherTicksLeft <= 0) {
                        gatherTicksLeft = DEFAULT_MAX_GATHER_TICKS;
                        ResourceName resourceName = ResourceSources.getBlockResourceName(this.gatherTarget, mob.level);
                        if (mob.level.destroyBlock(gatherTarget, false)) {

                            // prioritise gathering adjacent targets first
                            todoGatherTargets.remove(gatherTarget);
                            for (BlockPos pos : MiscUtil.findAdjacentBlocks(gatherTarget, BLOCK_CONDITION))
                                if (!todoGatherTargets.contains(pos))
                                    todoGatherTargets.add(pos);

                            Unit unit = (Unit) mob;
                            unit.getItems().add(new ItemStack(targetResourceSource.items.get(0)));
                            UnitClientboundPacket.sendSyncResourcesPacket(mob);

                            // if at max resources, go to drop off automatically, then return to this gather goal
                            if (Unit.atMaxResources(unit))
                                saveAndReturnResources();
                        }
                    }
                }
            }
        }
    }

    public void saveAndReturnResources() {
        Unit unit = (Unit) mob;
        if (unit.getReturnResourcesGoal() != null) {
            this.saveState();
            unit.resetBehaviours();
            WorkerUnit.resetBehaviours((WorkerUnit) unit);
            unit.getReturnResourcesGoal().returnToClosestBuilding();
        }
    }

    private void saveState() {
        todoGatherTargetsSaved.clear();
        todoGatherTargetsSaved.addAll(todoGatherTargets);
        gatherTargetSaved = gatherTarget;
        targetResourceNameSaved = targetResourceName;
        targetResourceSourceSaved = targetResourceSource;
        targetFarmSaved = targetFarm;
    }
    public void loadState() {
        todoGatherTargets.clear();
        todoGatherTargets.addAll(todoGatherTargetsSaved);
        gatherTarget = gatherTargetSaved;
        targetResourceName = targetResourceNameSaved;
        targetResourceSource = targetResourceSourceSaved;
        targetFarm = targetFarmSaved;
    }
    public boolean hasSavedData() {
        return todoGatherTargetsSaved.size() > 0 ||
                gatherTargetSaved != null ||
                targetResourceNameSaved != ResourceName.NONE ||
                targetResourceSourceSaved != null ||
                targetFarmSaved != null;
    }
    public void deleteSavedState() {
        todoGatherTargetsSaved.clear();
        gatherTargetSaved = null;
        targetResourceNameSaved = ResourceName.NONE;
        targetResourceSourceSaved = null;
        targetFarmSaved = null;
    }

    private boolean isBlockInRange(BlockPos target) {
        return target.distSqr(new Vec3i(mob.getX(), mob.getEyeY(), mob.getZ())) <= REACH_RANGE_SQR;
    }

    // only count as gathering if in range of the target
    public boolean isGathering() {
        if (!Unit.atMaxResources((Unit) mob) && this.gatherTarget != null && this.targetResourceSource != null &&
            ResourceSources.getBlockResourceName(this.gatherTarget, mob.level) != ResourceName.NONE)
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
            this.targetResourceSource = ResourceSources.getFromBlockPos(gatherTarget, this.mob.level);
        }
    }

    // locks the worker to only gather from this specific building
    public void setTargetFarm(Building building) {
        this.targetFarm = building;
    }

    // stop attempting to gather the current target but continue searching
    public void removeGatherTarget() {
        gatherTarget = null;
        targetResourceSource = null;
        gatherTicksLeft = DEFAULT_MAX_GATHER_TICKS;
        searchCdTicksLeft = 0;
    }

    // stop gathering and searching entirely, and remove saved data for
    public void stopGathering() {
        todoGatherTargets.clear();
        targetFarm = null;
        removeGatherTarget();
        this.setTargetResourceName(ResourceName.NONE);
        super.stopMoving();
    }

    public BlockPos getGatherTarget() {
        return gatherTarget;
    }
}
