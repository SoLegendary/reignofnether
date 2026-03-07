package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.ability.heroAbilities.enchanter.CivilEnchantment;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.unit.TargetResourcesSave;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.solegendary.reignofnether.resources.BlockUtils.isFallingLogBlock;
import static com.solegendary.reignofnether.resources.BlockUtils.isLogBlock;

// Move towards the nearest open resource blocks and start gathering them
// Can be toggled between food, wood and ore, and disabled by clicking

public class GatherResourcesGoal extends MoveToTargetBlockGoal {

    // resource targets that we are actively targeting
    public TargetResourcesSave data = new TargetResourcesSave();
    // copy of activeData that is saved temporarily, reset on resetBeheaviours, used for returning resources
    public TargetResourcesSave saveData = new TargetResourcesSave();
    // copy of saveData that is never erased, used for Call to Arms / Back to Work
    public TargetResourcesSave permSaveData = new TargetResourcesSave();

    private static final int REACH_RANGE = 5;
    private static final int DEFAULT_MAX_GATHER_TICKS = 600; // ticks to gather blocks - actual ticks may be lower, depending on the ResourceSource targeted
    private float gatherTicksLeft = DEFAULT_MAX_GATHER_TICKS;
    private static final int MAX_SEARCH_CD_TICKS = 40; // while idle, worker will look for a new block once every this number of ticks (searching is expensive!)
    private int searchCdTicksLeft = 0;
    private int failedSearches = 0; // number of times we've failed to search for a new block - as this increases slow down or stop searching entirely to prevent lag
    private static final int MAX_FAILED_SEARCHES = 4;
    private static final float TICK_CD = 20; // only tick down gather time once this many ticks to reduce processing requirements
    private float cdTicksLeft = TICK_CD;
    public static final int NO_TARGET_TIMEOUT = 50; // if we reach this time without progressing a gather tick while having navigation done, then switch a new target
    public static final int IDLE_TIMEOUT = 200; // ticks spent without a target to be considered idle
    private int ticksWithoutTarget = 0; // ticks spent without an active gather target (only increments serverside)
    private int ticksIdle = IDLE_TIMEOUT; // ticksWithoutTarget but never reset unless we've reacquired a target - used for idle checks
    private int ticksStationaryWithTarget = 0; // ticks that the worker hasn't moved and gatherTarget != null
    public static final int TICKS_STATIONARY_TIMEOUT = 100; // ticks that the worker hasn't moved and gatherTarget != null
    private BlockPos lastOnPos = null;
    private BlockPos altSearchPos = null; // block search origin that may be used instead of the mob position

    // whenever we attempt to assign a block as a target it must pass this test
    private final Predicate<BlockPos> BLOCK_CONDITION = bp -> {
        BlockState bs = mob.level().getBlockState(bp);
        BlockState bsAbove = mob.level().getBlockState(bp.above());
        ResourceSource resBlock = ResourceSources.getFromBlockPos(bp, mob.level());

        if (!mob.level().getWorldBorder().isWithinBounds(bp))
            return false;

        // is a valid resource block and meets the target ResourceSource's blockstate condition
        if (resBlock == null || resBlock.resourceName != data.targetResourceName) // || resBlock.name.equals("Leaves")
            return false;
        if (!resBlock.blockStateTest.test(bs))
            return false;

        // if the worker is farming, stick to only the assigned farm
        if (data.targetFarm != null && !data.targetFarm.isPosInsideBuilding(bp))
            return false;

        if (bs.getBlock() == Blocks.FARMLAND || bs.getBlock() == Blocks.SOUL_SAND) {
            if (!bsAbove.isAir() || !canAffordReplant() || !BuildingUtils.isPosInsideAnyBuilding(mob.level().isClientSide(), bp))
                return false;
        }
        // is not part of a building (unless farming)
        else if (data.targetFarm == null && BuildingUtils.isPosInsideAnyNonBridgeBuilding(mob.level().isClientSide(), bp))
            return false;

        // not covered by solid blocks
        boolean hasClearNeighbour = false;
        for (BlockPos adjBp : List.of(bp.above(), bp.north(), bp.south(), bp.east(), bp.west())) {
            if (ResourceSources.isClearMaterial(mob.level().getBlockState(adjBp))) {
                hasClearNeighbour = true;
                break;
            }
        }
        if (!hasClearNeighbour)
            return false;

        // not targeted by another nearby worker
        AABB aabb = AABB.ofSize(this.mob.position(), REACH_RANGE * 2,REACH_RANGE * 2,REACH_RANGE * 2);
        for (LivingEntity entity : this.mob.level().getNearbyEntities(LivingEntity.class, TargetingConditions.forNonCombat(), this.mob, aabb)) {
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
    public GatherResourcesGoal(Mob mob) {
        super(mob, true, REACH_RANGE - 1);
    }

    public void syncFromServer(ResourceName gatherName, BlockPos gatherPos, int gatherTicks) {
        this.data.targetResourceName = gatherName;
        this.data.gatherTarget = gatherPos;
        this.gatherTicksLeft = gatherTicks;
        this.data.targetResourceSource = ResourceSources.getFromBlockPos(data.gatherTarget, mob.level());
    }

    public void tickClient() {
        if (data.targetResourceSource != null && this.data.gatherTarget != null && isGathering() && FogOfWarClientEvents.isInBrightChunk(this.data.gatherTarget)) {
            gatherTicksLeft = Math.min(gatherTicksLeft, data.targetResourceSource.ticksToGather);
            gatherTicksLeft -= 1;
            if (gatherTicksLeft <= 0)
                gatherTicksLeft = data.targetResourceSource.ticksToGather;
            int gatherProgress = Math.round((data.targetResourceSource.ticksToGather - gatherTicksLeft) / (float) data.targetResourceSource.ticksToGather * 10);
            this.mob.level().destroyBlockProgress(this.mob.getId(), data.gatherTarget, gatherProgress);
        }
    }

    // move towards the targeted block and start gathering it
    public void tick() {
        if (this.mob.level().isClientSide()) {
            tickClient();
            return;
        }

        cdTicksLeft -= 1;
        if (cdTicksLeft <= 0)
            cdTicksLeft = TICK_CD;
        else
            return;

        if (data.gatherTarget == null && data.targetResourceName != ResourceName.NONE) {
            searchCdTicksLeft -= (TICK_CD / 2); // for some this is run twice as fast as we expect

            // prioritise gathering adjacent targets first
            for (BlockPos todoBp : data.todoGatherTargets)
                if (BLOCK_CONDITION.test(todoBp)) {
                    data.gatherTarget = todoBp;
                    break;
                }

            if (data.gatherTarget != null)
                data.todoGatherTargets.remove(data.gatherTarget);

            if (data.gatherTarget == null && searchCdTicksLeft <= 0) {
                if (data.targetFarm != null) {
                    for (BuildingBlock block : data.targetFarm.getBlocks()) {
                        if (BLOCK_CONDITION.test(block.getBlockPos())) {
                            data.gatherTarget = block.getBlockPos();
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
                        if (failedSearches > MAX_FAILED_SEARCHES) {
                            //System.out.println("Failed too many searches.");
                            stopGathering();
                            ticksIdle += 200;
                        }

                        bpOpt = BlockPos.findClosestMatch(
                            new BlockPos(
                                    (int) mob.getEyePosition().x,
                                    (int) mob.getEyePosition().y,
                                    (int) mob.getEyePosition().z
                            ), range, range,
                            BLOCK_CONDITION);
                    }

                    bpOpt.ifPresentOrElse(
                        blockPos -> {
                            data.gatherTarget = blockPos;
                            failedSearches = 0;
                        },
                        () -> {
                            failedSearches += 1;
                        }
                    );
                }
                searchCdTicksLeft = MAX_SEARCH_CD_TICKS;
            }
            if (data.gatherTarget != null)
                data.targetResourceSource = ResourceSources.getFromBlockPos(data.gatherTarget, mob.level());
        }

        if (data.gatherTarget != null) {

            // if the mob is not gathering and not moving, it's likely trying to reach a block out of reach
            // so remove the target and start looking passively after a timeout
            if (this.mob.getOnPos().equals(lastOnPos) && !isGathering())
                ticksStationaryWithTarget += TICK_CD;
            else
                ticksStationaryWithTarget = 0;

            if (ticksStationaryWithTarget >= TICKS_STATIONARY_TIMEOUT) {
                ticksStationaryWithTarget = 0;
                data.gatherTarget = null;
                return;
            }
            lastOnPos = this.mob.getOnPos();

            // if the block is no longer valid (destroyed or somehow badly targeted)
            if (!BLOCK_CONDITION.test(this.data.gatherTarget))
                removeGatherTarget();
            else // keep persistently moving towards the target
                super.setMoveTarget(data.gatherTarget);

            if (isGathering()) {
                ticksIdle = 0;

                // need to manually set cooldown higher (default is 2) or else we don't have enough time
                // for the mob to turn before behaviour is reset
                mob.getLookControl().setLookAt(data.gatherTarget.getX(), data.gatherTarget.getY(), data.gatherTarget.getZ());
                mob.getLookControl().lookAtCooldown = 20;

                BlockState bsTarget = mob.level().getBlockState(data.gatherTarget);

                // replant crops on empty farmland
                if (bsTarget.getBlock() == Blocks.FARMLAND || bsTarget.getBlock() == Blocks.SOUL_SAND) {
                    gatherTicksLeft -= (TICK_CD / 2);
                    gatherTicksLeft = Math.min(gatherTicksLeft, ResourceSources.REPLANT_TICKS_MAX);
                    if (gatherTicksLeft <= 0) {
                        gatherTicksLeft = DEFAULT_MAX_GATHER_TICKS;

                        if (canAffordReplant()) {
                            ResourcesServerEvents.addSubtractResources(new Resources(((Unit) mob).getOwnerName(), 0, -ResourceCosts.REPLANT_WOOD_COST, 0));
                            mob.level().setBlockAndUpdate(data.gatherTarget.above(), ((WorkerUnit) mob).getReplantBlockState());
                            removeGatherTarget();
                        }
                    }
                }
                else {
                    float ticksToProgress;

                    if (ResearchServerEvents.playerHasCheat(((Unit) mob).getOwnerName(), "operationcwal"))
                        ticksToProgress = (TICK_CD / 2) * 10;
                    else
                        ticksToProgress = (TICK_CD / 2);

                    if (mob instanceof VillagerUnit vUnit) {
                        if (ResourceSources.getBlockResourceName(getGatherTarget(), mob.level()) == ResourceName.WOOD &&
                            vUnit.getUnitProfession() == VillagerUnitProfession.LUMBERJACK) {
                            if (vUnit.isVeteran())
                                ticksToProgress *= VillagerUnit.LUMBERJACK_SPEED_MULT_VETERAN;
                            else
                                ticksToProgress *= VillagerUnit.LUMBERJACK_SPEED_MULT;
                        }
                        else if (ResourceSources.getBlockResourceName(getGatherTarget(), mob.level()) == ResourceName.ORE &&
                                vUnit.getUnitProfession() == VillagerUnitProfession.MINER) {
                            if (vUnit.isVeteran())
                                ticksToProgress *= VillagerUnit.MINER_SPEED_MULT_VETERAN;
                            else
                                ticksToProgress *= VillagerUnit.MINER_SPEED_MULT;
                        }
                        ticksToProgress *= CivilEnchantment.getEfficiencyMultiplier(vUnit);
                    }

                    this.gatherTicksLeft -= ticksToProgress;

                    gatherTicksLeft = Math.min(gatherTicksLeft, data.targetResourceSource != null ? data.targetResourceSource.ticksToGather : Integer.MAX_VALUE);
                    if (gatherTicksLeft <= 0) {
                        gatherTicksLeft = DEFAULT_MAX_GATHER_TICKS;

                        BlockState bs = this.mob.level().getBlockState(data.gatherTarget);
                        boolean isLogBlock = isLogBlock(bs);
                        boolean isFallingLogBlock = isFallingLogBlock(bs);
                        if (isLogBlock)
                            ResourcesServerEvents.fellAdjacentLogs(data.gatherTarget, new ArrayList<>(), this.mob.level());

                        ResourceName expName = ResourceName.NONE;
                        if (ResourceSources.getBlockResourceName(getGatherTarget(), mob.level()) == ResourceName.FOOD && isFarming())
                            expName = ResourceName.FOOD;
                        else if (ResourceSources.getBlockResourceName(getGatherTarget(), mob.level()) == ResourceName.WOOD && (isLogBlock || isFallingLogBlock))
                            expName = ResourceName.WOOD;
                        else if (ResourceSources.getBlockResourceName(getGatherTarget(), mob.level()) == ResourceName.ORE && bs.getBlock() != Blocks.POINTED_DRIPSTONE)
                            expName = ResourceName.ORE;

                        if (mob.level().destroyBlock(data.gatherTarget, false)) {

                            if (mob instanceof VillagerUnit vUnit) {
                                if (expName == ResourceName.FOOD)
                                    vUnit.incrementFarmerExp();
                                else if (expName == ResourceName.WOOD)
                                    vUnit.incrementLumberjackExp();
                                else if (expName == ResourceName.ORE)
                                    vUnit.incrementMinerExp();
                            }

                            // replace workers' mine ores with cobble to prevent creating potholes
                            if (data.targetResourceSource != null && data.targetResourceSource.resourceName == ResourceName.ORE && bsTarget.getBlock() != Blocks.POINTED_DRIPSTONE) {
                                BlockState replaceBs;
                                if (BuildingUtils.isInNetherRange(mob.level().isClientSide(), data.gatherTarget))
                                    replaceBs = BlockRegistrar.WALKABLE_MAGMA_BLOCK.get().defaultBlockState();
                                else if (bsTarget.getBlock().getName().getString().toLowerCase().contains("deepslate"))
                                    replaceBs = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                                else
                                    replaceBs = Blocks.COBBLESTONE.defaultBlockState();
                                this.mob.level().setBlockAndUpdate(data.gatherTarget, replaceBs);
                            }

                            // prioritise gathering adjacent targets first
                            data.todoGatherTargets.remove(data.gatherTarget);
                            for (BlockPos pos : MiscUtil.findAdjacentBlocks(data.gatherTarget, BLOCK_CONDITION))
                                if (!data.todoGatherTargets.contains(pos))
                                    data.todoGatherTargets.add(pos);

                            Unit unit = (Unit) mob;
                            unit.getItems().add(new ItemStack(data.targetResourceSource.items.get(0)));
                            UnitSyncClientboundPacket.sendSyncResourcesPacket(unit);

                            // if at max resources, go to drop off automatically, then return to this gather goal
                            if (Unit.atThresholdResources(unit))
                                saveAndReturnResources();

                            removeGatherTarget();
                        }
                    }
                }
            }
            else {
                // track how long we've been without a target
                // if we have spent too long still then we are stuck andreevaulate our gather target
                if (mob.getNavigation().isDone())
                    ticksWithoutTarget += (TICK_CD / 2);
                if (ticksWithoutTarget >= NO_TARGET_TIMEOUT)
                    this.removeGatherTarget();
            }
        } else {
            ticksIdle += (TICK_CD / 2);
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
        saveData.todoGatherTargets.clear();
        saveData.todoGatherTargets.addAll(data.todoGatherTargets);
        saveData.gatherTarget = data.gatherTarget;
        saveData.targetResourceName = data.targetResourceName;
        saveData.targetResourceSource = data.targetResourceSource;
        saveData.targetFarm = data.targetFarm;
        savePermState();
    }

    public void savePermState() {
        if (data.hasData()) {
            permSaveData.todoGatherTargets.clear();
            permSaveData.todoGatherTargets.addAll(data.todoGatherTargets);
            permSaveData.gatherTarget = data.gatherTarget;
            permSaveData.targetResourceName = data.targetResourceName;
            permSaveData.targetResourceSource = data.targetResourceSource;
            permSaveData.targetFarm = data.targetFarm;
        }
    }

    public void loadState() {
        data.todoGatherTargets.clear();
        data.todoGatherTargets.addAll(saveData.todoGatherTargets);
        data.gatherTarget = saveData.gatherTarget;
        data.targetResourceName = saveData.targetResourceName;
        data.targetResourceSource = saveData.targetResourceSource;
        data.targetFarm = saveData.targetFarm;
    }


    private boolean isBlockInRange(BlockPos target) {
        int reachRangeBonus = (int) Math.min(5, ticksWithoutTarget / TICK_CD);
        return target.distToCenterSqr(mob.getX(), mob.getEyeY(), mob.getZ()) <= Math.pow(REACH_RANGE + reachRangeBonus, 2);
    }

    // only count as gathering if in range of the target
    public boolean isGathering() {
        if (!Unit.atMaxResources((Unit) mob) && data.gatherTarget != null && this.mob.level().isClientSide())
            return isBlockInRange(data.gatherTarget);

        if (!Unit.atMaxResources((Unit) mob) && this.data.gatherTarget != null && this.data.targetResourceSource != null &&
            ResourceSources.getBlockResourceName(this.data.gatherTarget, mob.level()) != ResourceName.NONE)
            return isBlockInRange(data.gatherTarget);
        return false;
    }

    private boolean canAffordReplant() {
        return ResourcesServerEvents.canAfford(((Unit) mob).getOwnerName(), ResourceName.WOOD, ResourceCosts.REPLANT_WOOD_COST);
    }

    public void setTargetResourceName(ResourceName resourceName) {
        data.targetResourceName = resourceName;
    }

    public ResourceName getTargetResourceName() {
        return data.targetResourceName;
    }

    @Override
    public void setMoveTarget(@Nullable BlockPos bp) {
        if (bp != null) {
            MiscUtil.addUnitCheckpoint((Unit) mob, bp, true);
        }
        super.setMoveTarget(bp);
        if (BLOCK_CONDITION.test(bp)) {
            this.data.gatherTarget = bp;
            this.data.targetResourceSource = ResourceSources.getFromBlockPos(data.gatherTarget, this.mob.level());
        }
    }

    public boolean isFarming() {
        return this.data.targetFarm != null;
    }

    @Nullable public BuildingPlacement getTargetFarm() {
        return this.data.targetFarm;
    }

    // locks the worker to only gather from this specific building
    public void setTargetFarm(BuildingPlacement building) {
        if (building != null) {
            MiscUtil.addUnitCheckpoint((Unit) mob, building.centrePos, true);
        }
        this.data.targetFarm = building;
    }

    // stop attempting to gather the current target but continue searching
    public void removeGatherTarget() {
        data.gatherTarget = null;
        data.targetResourceSource = null;
        gatherTicksLeft = DEFAULT_MAX_GATHER_TICKS;
        searchCdTicksLeft = 0;
        ticksWithoutTarget = 0;
    }

    // stop gathering and searching entirely, and remove saved data for
    public void stopGathering() {
        ticksIdle = IDLE_TIMEOUT;
        failedSearches = 0;
        this.savePermState();
        this.mob.level().destroyBlockProgress(this.mob.getId(), new BlockPos(0,0,0), 0);
        data.todoGatherTargets.clear();
        data.targetFarm = null;
        removeGatherTarget();
        this.setTargetResourceName(ResourceName.NONE);
        super.stopMoving();
    }

    public BlockPos getGatherTarget() {
        return data.gatherTarget;
    }

    public int getGatherTicksLeft() {
        return (int) gatherTicksLeft;
    }

    public boolean isIdle() {
        return ticksIdle > IDLE_TIMEOUT;
    }
}
