package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.blocks.BlockUtils;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;

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
        // feet can't sit inside a body-blocking block. leaves report non-solid but block the body, and
        // fence-likes are impassable barriers (see isFenceLike), so reject both explicitly.
        if (MiscUtil.isSolidBlocking(level, feet) || BlockUtils.isLeafBlock(feetBs) || isFenceLike(feetBs) || isThinWall(feetBs)) return KIND_BLOCKED;

        BlockState floorBs = level.getBlockState(floor);
        boolean supportFloor = isSupportFloor(level, floor, floorBs);
        // standing on a half-height floor (eg. a bottom slab) lowers the feet, so a half-height ceiling (eg. a top
        // slab) in the head cell can still clear a 2-tall body. anything that isn't ground support counts as full.
        int floorTop8 = supportFloor ? top8(floorBs.getCollisionShape(level, floor)) : SHAPE_STEPS;
        if (headBlocks(level, head, headBs, floorTop8)) return KIND_BLOCKED;

        if (floorBs.getBlock() == Blocks.WATER && feetBs.getBlock() == Blocks.LILY_PAD)
            return KIND_LAND;

        FluidState feetFluid = feetBs.getFluidState();
        if (feetFluid.is(FluidTags.LAVA)) return KIND_LAVA;
        // the water surface is the floor: units float, so the walkable cell is the air on top of the water (see
        // the floor fluid check below), never a cell inside it. only exceptions: waterfalls, and a low ceiling
        // (eg. a bridge) that leaves no room for the cell on top.
        if (!feetFluid.isEmpty()) {
            if (coversFluid(headBs.getFluidState())) return KIND_BLOCKED;
            return isFalling(feetFluid) || MiscUtil.isSolidBlocking(level, head.above()) ? KIND_WATER : KIND_BLOCKED;
        }

        if (floorBs.getBlock() == Blocks.SLIME_BLOCK) return KIND_SLIME;
        if (feetBs.getBlock() == Blocks.FIRE || feetBs.getBlock() == Blocks.SOUL_FIRE) return KIND_FIRE;
        if (floorBs.getBlock() == Blocks.MAGMA_BLOCK
                || floorBs.getBlock() == Blocks.CAMPFIRE
                || floorBs.getBlock() == Blocks.SOUL_CAMPFIRE) return KIND_FIRE;

        if (supportFloor) return KIND_LAND;

        if (floorBs.getFluidState().is(FluidTags.LAVA)) return KIND_LAVA;
        if (!floorBs.getFluidState().isEmpty()) return KIND_WATER;
        return KIND_BLOCKED; // no floor support
    }

    // a fence/wall/closed-gate floor isn't standable (can't perch on a railing; vanilla agrees), and a leaf
    // floor must not count either or units path up over the tree canopy (and goal snapping lands a worker on
    // the very leaf it's told to mine). don't count these as support: the cell falls through to "no floor" so
    // the unit routes around and stands on the real ground below. BlockTags.LEAVES is allocation-free, unlike
    // BlockUtils.isLeafBlock whose List.of fallback would allocate on this hot path for every solid floor.
    // a ground lantern is a half-height floor like a bottom slab; a hanging one dangles from above, so isn't ground.
    private static boolean isSupportFloor(Level level, BlockPos floor, BlockState floorBs) {
        return MiscUtil.isSolidBlocking(level, floor) &&
                !isFenceLike(floorBs) &&
                !floorBs.is(BlockTags.LEAVES) &&
                !(floorBs.getBlock() instanceof LanternBlock && floorBs.getValue(LanternBlock.HANGING));
    }

    // the head cell stops a 2-tall body unless its collision starts at or above the floor's top: the body then
    // spans [floorTop-1, floorTop+1] relative to the feet cell, which tops out at the head cell's own base. a full
    // block (bottom 0) therefore only passes a floor of top 0, ie. never - same as before half-heights existed.
    private static boolean headBlocks(Level level, BlockPos head, BlockState headBs, int floorTop8) {
        if (BlockUtils.isLeafBlock(headBs) || isFenceLike(headBs) || isThinWall(headBs)) return true;
        if (!MiscUtil.isSolidBlocking(level, head)) return false;
        return bottom8(headBs.getCollisionShape(level, head)) < floorTop8;
    }

    // Collision extent of a cell's block in eighths of a block. Packed into WalkabilityGridChunk.solid[] beside the
    // solid flag so A* can tell a half-height block (slab, lantern) from a full one without a second per-cell array.
    // Rounded toward the conservative side (ceilings read lower, floors read higher), so a unit is never routed
    // through a gap a body could clip. Bits: 0 = solid, 1-3 = bottom8 (0..7), 4-6 = top8 - 1 (top8 1..8).
    public static final int SHAPE_STEPS = 8;
    public static final byte FULL_BLOCK = (byte) (1 | (SHAPE_STEPS - 1) << 4);

    // non-empty collision shape only (callers gate on isSolidBlocking)
    public static byte packSolid(VoxelShape shape) {
        return (byte) (1 | bottom8(shape) << 1 | (top8(shape) - 1) << 4);
    }

    // eighths from the cell's base to the lowest collision; SHAPE_STEPS if there's none
    public static int bottom8(VoxelShape shape) {
        if (shape.isEmpty()) return SHAPE_STEPS;
        return Mth.clamp(Mth.floor(shape.min(Direction.Axis.Y) * SHAPE_STEPS), 0, SHAPE_STEPS - 1);
    }

    // eighths from the cell's base to the highest collision (capped at a full block); 0 if there's none
    public static int top8(VoxelShape shape) {
        if (shape.isEmpty()) return 0;
        return Mth.clamp(Mth.ceil(shape.max(Direction.Axis.Y) * SHAPE_STEPS), 1, SHAPE_STEPS);
    }

    public static int bottom8Of(byte packed) {
        return packed == 0 ? SHAPE_STEPS : (packed >> 1) & 7;
    }

    public static int top8Of(byte packed) {
        return packed == 0 ? 0 : ((packed >> 4) & 7) + 1;
    }

    // true if the fluid in a cell puts the cell below it underwater. flowing water counts too, or a current on top
    // of a lake leaves a walkable layer under it. falling water doesn't, so waterfalls stay passable.
    public static boolean coversFluid(FluidState above) {
        return !above.isEmpty() && !isFalling(above);
    }

    private static boolean isFalling(FluidState fluid) {
        return fluid.hasProperty(FlowingFluid.FALLING) && fluid.getValue(FlowingFluid.FALLING);
    }

    // glass panes and iron bars are thin, but run to the cell edge on every side they connect to, so a unit can't
    // cross the cell through them. whether vanilla flags their collision as "solid" depends on the shape of each
    // connection state, so classify them explicitly rather than rely on isSolidBlocking.
    public static boolean isThinWall(BlockState bs) {
        return bs.getBlock() instanceof IronBarsBlock;
    }

    // fences, walls and closed fence gates are 1.5-block barriers units can't pass or stand on (open gates are
    // walkable, so excluded). mirrors vanilla WalkNodeEvaluator's BlockPathTypes.FENCE handling.
    public static boolean isFenceLike(BlockState bs) {
        if (bs.is(BlockTags.FENCES) || bs.is(BlockTags.WALLS) || bs.getBlock() instanceof SculkCatalystBlock || bs.getBlock() instanceof SculkShriekerBlock) return true;
        return bs.getBlock() instanceof FenceGateBlock && !bs.getValue(FenceGateBlock.OPEN);
    }
}
