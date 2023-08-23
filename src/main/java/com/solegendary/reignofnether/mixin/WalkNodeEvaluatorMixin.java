package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import static net.minecraft.world.level.pathfinder.WalkNodeEvaluator.checkNeighbourBlocks;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {

    public WalkNodeEvaluatorMixin() {
    }

    @Inject(
            method = "getBlockPathType(Lnet/minecraft/world/level/BlockGetter;III)Lnet/minecraft/world/level/pathfinder/BlockPathTypes;",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getBlockPathType(BlockGetter pLevel, int pX, int pY, int pZ, CallbackInfoReturnable<BlockPathTypes> cir) {
        if (!(this.mob instanceof Unit))
            return;
        cir.setReturnValue(getBlockPathTypeStatic2(pLevel, new BlockPos.MutableBlockPos(pX, pY, pZ)));
    }

    private static boolean isBurningBlock2(BlockState pState) {
        return CampfireBlock.isLitCampfire(pState) ||
                pState.is(BlockTags.FIRE) ||
                pState.is(Blocks.LAVA) || // removed magma
                pState.is(Blocks.LAVA_CAULDRON) ||
                pState.is(Blocks.ACACIA_LEAVES) || // adding leaves to prevent workers getting stuck in trees
                pState.is(Blocks.BIRCH_LEAVES) || // leaving out jungle/azalea leaves since they can be on the floor
                pState.is(Blocks.DARK_OAK_LEAVES) ||
                pState.is(Blocks.OAK_LEAVES) ||
                pState.is(Blocks.MANGROVE_LEAVES) ||
                pState.is(Blocks.SPRUCE_LEAVES);
    }

    // just a copy of the original so we don't have to change access levels
    private static BlockPathTypes getBlockPathTypeStatic2(BlockGetter pLevel, BlockPos.MutableBlockPos pPos) {
        int i = pPos.getX();
        int j = pPos.getY();
        int k = pPos.getZ();
        BlockPathTypes blockpathtypes = getBlockPathTypeRaw2(pLevel, pPos);
        if (blockpathtypes == BlockPathTypes.OPEN && j >= pLevel.getMinBuildHeight() + 1) {
            BlockPathTypes blockpathtypes1 = getBlockPathTypeRaw2(pLevel, pPos.set(i, j - 1, k));
            blockpathtypes = blockpathtypes1 != BlockPathTypes.WALKABLE && blockpathtypes1 != BlockPathTypes.OPEN && blockpathtypes1 != BlockPathTypes.WATER && blockpathtypes1 != BlockPathTypes.LAVA ? BlockPathTypes.WALKABLE : BlockPathTypes.OPEN;
            if (blockpathtypes1 == BlockPathTypes.DAMAGE_FIRE)
                blockpathtypes = BlockPathTypes.DAMAGE_FIRE;
            if (blockpathtypes1 == BlockPathTypes.DAMAGE_CACTUS)
                blockpathtypes = BlockPathTypes.DAMAGE_CACTUS;
            if (blockpathtypes1 == BlockPathTypes.DAMAGE_OTHER)
                blockpathtypes = BlockPathTypes.DAMAGE_OTHER;
            if (blockpathtypes1 == BlockPathTypes.STICKY_HONEY)
                blockpathtypes = BlockPathTypes.STICKY_HONEY;
            if (blockpathtypes1 == BlockPathTypes.POWDER_SNOW)
                blockpathtypes = BlockPathTypes.DANGER_POWDER_SNOW;
        }
        if (blockpathtypes == BlockPathTypes.WALKABLE)
            blockpathtypes = checkNeighbourBlocks(pLevel, pPos.set(i, j, k), blockpathtypes);

        return blockpathtypes;
    }

    // just a copy of the original so we don't have to change access levels
    private static BlockPathTypes getBlockPathTypeRaw2(BlockGetter pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        BlockPathTypes type = blockstate.getBlockPathType(pLevel, pPos, (Mob)null);
        if (type != null) {
            return type;
        } else {
            Block block = blockstate.getBlock();
            Material material = blockstate.getMaterial();
            if (blockstate.isAir()) {
                return BlockPathTypes.OPEN;
            } else if (!blockstate.is(BlockTags.TRAPDOORS) && !blockstate.is(Blocks.LILY_PAD) && !blockstate.is(Blocks.BIG_DRIPLEAF)) {
                if (blockstate.is(Blocks.POWDER_SNOW)) {
                    return BlockPathTypes.POWDER_SNOW;
                } else if (blockstate.is(Blocks.CACTUS)) {
                    return BlockPathTypes.DAMAGE_CACTUS;
                } else if (blockstate.is(Blocks.SWEET_BERRY_BUSH)) {
                    return BlockPathTypes.DAMAGE_OTHER;
                } else if (blockstate.is(Blocks.HONEY_BLOCK)) {
                    return BlockPathTypes.STICKY_HONEY;
                } else if (blockstate.is(Blocks.COCOA)) {
                    return BlockPathTypes.COCOA;
                } else {
                    FluidState fluidstate = pLevel.getFluidState(pPos);
                    BlockPathTypes nonLoggableFluidPathType = fluidstate.getBlockPathType(pLevel, pPos, (Mob)null, false);
                    if (nonLoggableFluidPathType != null) {
                        return nonLoggableFluidPathType;
                    } else if (fluidstate.is(FluidTags.LAVA)) {
                        return BlockPathTypes.LAVA;
                    } else if (isBurningBlock2(blockstate)) {
                        return BlockPathTypes.DAMAGE_FIRE;
                    } else if (DoorBlock.isWoodenDoor(blockstate) && !(Boolean)blockstate.getValue(DoorBlock.OPEN)) {
                        return BlockPathTypes.DOOR_WOOD_CLOSED;
                    } else if (block instanceof DoorBlock && material == Material.METAL && !(Boolean)blockstate.getValue(DoorBlock.OPEN)) {
                        return BlockPathTypes.DOOR_IRON_CLOSED;
                    } else if (block instanceof DoorBlock && (Boolean)blockstate.getValue(DoorBlock.OPEN)) {
                        return BlockPathTypes.DOOR_OPEN;
                    } else if (block instanceof BaseRailBlock) {
                        return BlockPathTypes.RAIL;
                    } else if (block instanceof LeavesBlock) {
                        return BlockPathTypes.LEAVES;
                    } else if (!blockstate.is(BlockTags.FENCES) && !blockstate.is(BlockTags.WALLS) && (!(block instanceof FenceGateBlock) || (Boolean)blockstate.getValue(FenceGateBlock.OPEN))) {
                        if (!blockstate.isPathfindable(pLevel, pPos, PathComputationType.LAND)) {
                            return BlockPathTypes.BLOCKED;
                        } else {
                            BlockPathTypes loggableFluidPathType = fluidstate.getBlockPathType(pLevel, pPos, (Mob)null, true);
                            if (loggableFluidPathType != null) {
                                return loggableFluidPathType;
                            } else {
                                return fluidstate.is(FluidTags.WATER) ? BlockPathTypes.WATER : BlockPathTypes.OPEN;
                            }
                        }
                    } else {
                        return BlockPathTypes.FENCE;
                    }
                }
            } else {
                return BlockPathTypes.TRAPDOOR;
            }
        }
    }
}
