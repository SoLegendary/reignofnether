package com.solegendary.reignofnether.unit.goals;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Material;

import java.util.List;
import java.util.function.Predicate;

// Move towards the nearest open resource blocks and start gathering them
// Can be toggled between food, wood and ore, and disabled by clicking
// TODO: make class for blocks individual resource value and hardness

public class GatherResourcesGoal extends MoveToTargetBlockGoal {

    private final List<Material> CLEAR_MATERIALS = List.of(Material.LEAVES, Material.WATER, Material.AIR, Material.GRASS);
    private final List<Block> FOOD_BLOCKS = List.of(Blocks.WHEAT, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.SWEET_BERRY_BUSH, Blocks.POTATOES, Blocks.CARROTS, Blocks.BEETROOTS, Blocks.SUGAR_CANE);
    private final List<Block> WOOD_BLOCKS = List.of(Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.JUNGLE_LOG, Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG);
    private final List<Block> ORE_BLOCKS = List.of(Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.EMERALD_ORE, Blocks.COPPER_ORE, Blocks.DIAMOND_ORE, Blocks.REDSTONE_ORE, Blocks.GOLD_ORE, Blocks.LAPIS_ORE);

    private final int BLOCK_BREAK_TICKS_MAX = 10;
    private int breakTicksLeft = BLOCK_BREAK_TICKS_MAX;

    private String targetResourceName = "None";
    private List<Block> targetResourceBlocks = null;

    public GatherResourcesGoal(PathfinderMob mob, double speedModifier) {
        super(mob, true, speedModifier, 2);
    }

    // move towards
    public void tick() {
        if (moveTarget == null && targetResourceBlocks != null) {

            // not covered by solid blocks and not targeted by another worker
            Predicate<BlockPos> condition = bp -> {
                boolean hasClearNeighbour = false;
                for (BlockPos adjBp : List.of(bp.north(), bp.south(), bp.east(), bp.west(), bp.above(), bp.below()))
                    if (CLEAR_MATERIALS.contains(mob.level.getBlockState(adjBp).getMaterial()))
                        hasClearNeighbour = true;
                if (!hasClearNeighbour)
                    return false;
                for (int unitId : UnitServerEvents.getAllUnitIds()) {
                    Unit unit = (Unit) mob.level.getEntity(unitId);
                    if (unit != null && unit.isWorker() && unit.getGatherResourceGoal() != null)
                        if (unit.getGatherResourceGoal().getGatherTarget().equals(moveTarget))
                            return false;
                }
                return true;
            };

            this.moveTarget = MiscUtil.findNearestBlock(
                (ServerLevel) mob.level,
                new Vec3i(
                    mob.getEyePosition().x,
                    mob.getEyePosition().y,
                    mob.getEyePosition().z
                ), 5,
                targetResourceBlocks,
                condition);

            this.mob.getLookControl().setLookAt(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ());
        }

        if (moveTarget != null) {
            breakTicksLeft -= 1;
            if (breakTicksLeft <= 0) {
                breakTicksLeft = BLOCK_BREAK_TICKS_MAX;
                mob.level.destroyBlock(moveTarget, false);
                ResourcesServerEvents.addSubtractResources(new Resources(
                    ((Unit) mob).getOwnerName(),
                    targetResourceName.equals("Food") ? 10 : 0,
                    targetResourceName.equals("Wood") ? 10 : 0,
                    targetResourceName.equals("Ore") ? 10 : 0
                ));
            }
        }
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
            case "Food" -> targetResourceBlocks = FOOD_BLOCKS;
            case "Wood" -> targetResourceBlocks = WOOD_BLOCKS;
            case "Ore" -> targetResourceBlocks = ORE_BLOCKS;
        }
    }

    public String getTargetResourceName() {
        return targetResourceName;
    }

    public void stopGathering() {
        setTargetResource("None");
        this.stop();
    }

    public BlockPos getGatherTarget() {
        return moveTarget;
    }
}
