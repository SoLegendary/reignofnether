package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.resources.BlockUtils;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

// Classifies one cell (x, y, z) as LAND / WATER / FIRE / LAVA / BLOCKED.
// A cell is occupiable when feet (y) and head (y+1) are non-solid AND the floor (y-1) supports.
public final class WalkabilityBuilder {
    private WalkabilityBuilder() {}

    public static final byte KIND_BLOCKED = 0;
    public static final byte KIND_LAND    = 1;
    public static final byte KIND_WATER   = 2;
    public static final byte KIND_FIRE    = 3;
    public static final byte KIND_LAVA    = 4;

    // Per-chunk build calls this ~98k times; reusable mutable positions avoid ~300k BlockPos allocations.
    private static final ThreadLocal<BlockPos.MutableBlockPos> FEET  = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<BlockPos.MutableBlockPos> HEAD  = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<BlockPos.MutableBlockPos> FLOOR = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    public static byte classify(Level level, int wx, int y, int wz) {
        BlockPos.MutableBlockPos feet  = FEET.get().set(wx, y, wz);
        BlockPos.MutableBlockPos head  = HEAD.get().set(wx, y + 1, wz);
        BlockPos.MutableBlockPos floor = FLOOR.get().set(wx, y - 1, wz);

        BlockState feetBs = level.getBlockState(feet);
        BlockState headBs = level.getBlockState(head);
        // Feet/head can't sit inside a body-blocking block; leaves report non-solid but block the body, so
        // include them explicitly (they're still never floor support, so units don't walk on them). Fences,
        // walls and closed fence gates are 1.5-tall barriers a unit can neither walk through nor stand on top
        // of (vanilla treats them as impassable FENCE), so block them too - otherwise units path onto railings.
        if (MiscUtil.isSolidBlocking(level, feet) || BlockUtils.isLeafBlock(feetBs) || isFenceLike(feetBs)) return KIND_BLOCKED;
        if (MiscUtil.isSolidBlocking(level, head) || BlockUtils.isLeafBlock(headBs) || isFenceLike(headBs)) return KIND_BLOCKED;

        BlockState floorBs = level.getBlockState(floor);

        FluidState feetFluid = feetBs.getFluidState();
        if (feetFluid.is(FluidTags.LAVA)) return KIND_LAVA;
        if (!feetFluid.isEmpty())          return KIND_WATER;

        if (feetBs.getBlock() == Blocks.FIRE || feetBs.getBlock() == Blocks.SOUL_FIRE) return KIND_FIRE;
        if (floorBs.getBlock() == Blocks.MAGMA_BLOCK
                || floorBs.getBlock() == Blocks.CAMPFIRE
                || floorBs.getBlock() == Blocks.SOUL_CAMPFIRE) return KIND_FIRE;

        // A fence/wall/closed-gate floor is NOT standable - a unit can't perch on a railing (vanilla agrees),
        // so don't count it as support; the cell falls through to "no floor" and the unit routes around.
        if (MiscUtil.isSolidBlocking(level, floor) && !isFenceLike(floorBs)) return KIND_LAND;
        if (floorBs.getFluidState().is(FluidTags.LAVA)) return KIND_LAVA;
        if (!floorBs.getFluidState().isEmpty()) return KIND_WATER;
        return KIND_BLOCKED; // no floor support
    }

    // Fences, walls and CLOSED fence gates are 1.5-block barriers units can't pass or stand on (open gates are
    // walkable, so they're excluded). Mirrors vanilla WalkNodeEvaluator's BlockPathTypes.FENCE handling.
    private static boolean isFenceLike(BlockState bs) {
        if (bs.is(BlockTags.FENCES) || bs.is(BlockTags.WALLS)) return true;
        return bs.getBlock() instanceof FenceGateBlock && !bs.getValue(FenceGateBlock.OPEN);
    }
}
