package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.resources.BlockUtils;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SculkCatalystBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

// classifies one cell (x, y, z) as LAND / WATER / FIRE / LAVA / BLOCKED.
// a cell is occupiable when feet (y) and head (y+1) are non-solid AND the floor (y-1) supports.
public final class WalkabilityBuilder {
    private WalkabilityBuilder() {}

    public static final byte KIND_BLOCKED = 0;
    public static final byte KIND_LAND    = 1;
    public static final byte KIND_WATER   = 2;
    public static final byte KIND_FIRE    = 3;
    public static final byte KIND_LAVA    = 4;
    public static final byte KIND_SLIME    = 5;

    // hot path (per cell during a chunk build); reuse mutable positions to avoid BlockPos allocations.
    private static final ThreadLocal<BlockPos.MutableBlockPos> FEET  = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<BlockPos.MutableBlockPos> HEAD  = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<BlockPos.MutableBlockPos> FLOOR = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    public static byte classify(Level level, int wx, int y, int wz) {
        BlockPos.MutableBlockPos feet  = FEET.get().set(wx, y, wz);
        BlockPos.MutableBlockPos head  = HEAD.get().set(wx, y + 1, wz);
        BlockPos.MutableBlockPos floor = FLOOR.get().set(wx, y - 1, wz);

        BlockState feetBs = level.getBlockState(feet);
        BlockState headBs = level.getBlockState(head);
        // feet/head can't sit inside a body-blocking block. leaves report non-solid but block the body, and
        // fence-likes are impassable barriers (see isFenceLike), so reject both explicitly.
        if (MiscUtil.isSolidBlocking(level, feet) || BlockUtils.isLeafBlock(feetBs) || isFenceLike(feetBs)) return KIND_BLOCKED;
        if (MiscUtil.isSolidBlocking(level, head) || BlockUtils.isLeafBlock(headBs) || isFenceLike(headBs)) return KIND_BLOCKED;

        BlockState floorBs = level.getBlockState(floor);
        if (floorBs.getBlock() == Blocks.WATER && feetBs.getBlock() == Blocks.LILY_PAD)
            return KIND_LAND;

        FluidState feetFluid = feetBs.getFluidState();
        if (feetFluid.is(FluidTags.LAVA)) return KIND_LAVA;
        if (!feetFluid.isEmpty())          return KIND_WATER;

        if (floorBs.getBlock() == Blocks.SLIME_BLOCK) return KIND_SLIME;
        if (feetBs.getBlock() == Blocks.FIRE || feetBs.getBlock() == Blocks.SOUL_FIRE) return KIND_FIRE;
        if (floorBs.getBlock() == Blocks.MAGMA_BLOCK
                || floorBs.getBlock() == Blocks.CAMPFIRE
                || floorBs.getBlock() == Blocks.SOUL_CAMPFIRE) return KIND_FIRE;

        // a fence/wall/closed-gate floor isn't standable (can't perch on a railing; vanilla agrees), and a leaf
        // floor must not count either or units path up over the tree canopy (and goal snapping lands a worker on
        // the very leaf it's told to mine). don't count these as support: the cell falls through to "no floor" so
        // the unit routes around and stands on the real ground below. BlockTags.LEAVES is allocation-free, unlike
        // BlockUtils.isLeafBlock whose List.of fallback would allocate on this hot path for every solid floor.
        if (MiscUtil.isSolidBlocking(level, floor) && !isFenceLike(floorBs) && !floorBs.is(BlockTags.LEAVES)) return KIND_LAND;
        if (floorBs.getFluidState().is(FluidTags.LAVA)) return KIND_LAVA;
        if (!floorBs.getFluidState().isEmpty()) return KIND_WATER;
        return KIND_BLOCKED; // no floor support
    }

    // fences, walls and closed fence gates are 1.5-block barriers units can't pass or stand on (open gates are
    // walkable, so excluded). mirrors vanilla WalkNodeEvaluator's BlockPathTypes.FENCE handling.
    public static boolean isFenceLike(BlockState bs) {
        if (bs.is(BlockTags.FENCES) || bs.is(BlockTags.WALLS) || bs.getBlock() instanceof SculkCatalystBlock || bs.getBlock() instanceof SculkShriekerBlock) return true;
        return bs.getBlock() instanceof FenceGateBlock && !bs.getValue(FenceGateBlock.OPEN);
    }
}
