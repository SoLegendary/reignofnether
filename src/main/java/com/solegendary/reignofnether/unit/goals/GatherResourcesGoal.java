package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.resources.ResourceBlocks;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.Unit;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

// Move towards the nearest open resource blocks and start gathering them
// Can be toggled between food, wood and ore, and disabled by clicking

public class GatherResourcesGoal extends MoveToTargetBlockGoal {

    private static final int REACH_RANGE = 4;
    private static final int BLOCK_BREAK_TICKS_MAX = 100;
    private int breakTicksLeft = BLOCK_BREAK_TICKS_MAX;

    private BlockPos gatherTarget = null;
    private String targetResourceName = "None"; // if !None, will passively target blocks around it
    private List<Block> targetResourceBlocks = null;

    public GatherResourcesGoal(PathfinderMob mob, double speedModifier) {
        super(mob, true, speedModifier, REACH_RANGE);
    }

    // move towards the targeted block and start gathering it
    public void tick() {

        if (gatherTarget != null) {
            // if the block is no longer valid (destroyed or somehow badly targeted)
            String resourceBlockType = ResourceBlocks.getResourceBlockType(this.gatherTarget, mob.level);
            if (resourceBlockType.equals("None"))
                this.gatherTarget = null;
            else // keep persistently moving towards the target
                this.setMoveTarget(gatherTarget);
        }

        if (gatherTarget == null && targetResourceBlocks != null) {

            Predicate<BlockPos> condition = bp -> {
                // not covered by solid blocks and
                boolean hasClearNeighbour = false;
                for (BlockPos adjBp : List.of(bp.north(), bp.south(), bp.east(), bp.west(), bp.above(), bp.below()))
                    if (ResourceBlocks.CLEAR_MATERIALS.contains(mob.level.getBlockState(adjBp).getMaterial()))
                        hasClearNeighbour = true;
                if (!hasClearNeighbour)
                    return false;
                // not targeted by another worker
                for (int unitId : UnitServerEvents.getAllUnitIds()) {
                    Unit unit = (Unit) mob.level.getEntity(unitId);
                    if (unit != null && unit.isWorker() && unit.getGatherResourceGoal() != null && unitId != this.mob.getId()) {
                        BlockPos otherUnitTarget = unit.getGatherResourceGoal().getGatherTarget();
                        if (otherUnitTarget != null && otherUnitTarget.equals(bp))
                            return false;
                    }
                }
                return true;
            };

            this.gatherTarget = MiscUtil.findNearestBlock(
                mob.level,
                new Vec3i(
                    mob.getEyePosition().x,
                    mob.getEyePosition().y,
                    mob.getEyePosition().z
                ), 5,
                targetResourceBlocks,
                condition);
        }

        if (gatherTarget != null) {
            this.setMoveTarget(gatherTarget);

            // if the block is no longer valid (destroyed or somehow badly targeted)
            String resourceBlockType = ResourceBlocks.getResourceBlockType(this.gatherTarget, mob.level);
            if (resourceBlockType.equals("None"))
                this.gatherTarget = null;

            if (isGathering()) {
                if (!this.mob.level.isClientSide())
                {
                    breakTicksLeft -= 1;
                    if (breakTicksLeft <= 0) {
                        breakTicksLeft = BLOCK_BREAK_TICKS_MAX;
                        mob.level.destroyBlock(gatherTarget, false);
                        ResourcesServerEvents.addSubtractResources(new Resources(
                                ((Unit) mob).getOwnerName(),
                                resourceBlockType.equals("Food") ? 50 : 0,
                                resourceBlockType.equals("Wood") ? 10 : 0,
                                resourceBlockType.equals("Ore") ? 25 : 0
                        ));
                    }
                }
                this.mob.getLookControl().setLookAt(gatherTarget.getX(), gatherTarget.getY(), gatherTarget.getZ());
            }
        }
    }

    // only count as gathering if in range of the target
    public boolean isGathering() {
        if (this.gatherTarget != null && !ResourceBlocks.getResourceBlockType(this.gatherTarget, mob.level).equals("None"))
            return Math.sqrt(gatherTarget.distSqr(new Vec3i(mob.getX(), mob.getY(), mob.getZ()))) <= REACH_RANGE + 1;
        return false;
    }

    public void toggleTargetResource() {
        switch (targetResourceName) {
            case "None" -> setTargetResource("Food");
            case "Food" -> setTargetResource("Wood");
            case "Wood" -> setTargetResource("Ore");
            case "Ore" -> setTargetResource("None");
        }
    }

    public void setTargetResource(String resourceName) {
        targetResourceName = resourceName;
        switch(targetResourceName) {
            case "None" -> targetResourceBlocks = null;
            case "Food" -> targetResourceBlocks = ResourceBlocks.FOOD_BLOCKS;
            case "Wood" -> targetResourceBlocks = ResourceBlocks.WOOD_BLOCKS;
            case "Ore" -> targetResourceBlocks = ResourceBlocks.ORE_BLOCKS;
        }
    }

    public String getTargetResourceName() {
        return targetResourceName;
    }

    // is both the move and the gather target
    @Override
    public void setMoveTarget(@Nullable BlockPos bp) {
        super.setMoveTarget(bp);
        this.gatherTarget = bp;
        if (bp != null)
            this.setTargetResource(ResourceBlocks.getResourceBlockType(bp, this.mob.level));
    }

    public void stopGathering() {
        this.gatherTarget = null;
        this.setTargetResource("None");
        super.stop();
    }

    public BlockPos getGatherTarget() {
        return gatherTarget;
    }
}
