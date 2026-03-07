package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class BlockUtils {

    private static final Random RANDOM = new Random();

    public static boolean isLogBlock(BlockState bs) {
        return List.of(Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.JUNGLE_LOG, Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG,
                        Blocks.OAK_WOOD, Blocks.BIRCH_WOOD, Blocks.ACACIA_WOOD, Blocks.DARK_OAK_WOOD, Blocks.JUNGLE_WOOD, Blocks.MANGROVE_WOOD, Blocks.SPRUCE_WOOD,
                        Blocks.CRIMSON_STEM, Blocks.WARPED_STEM, Blocks.MUSHROOM_STEM, Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE)
                .contains(bs.getBlock()) || bs.getTags().collect(Collectors.toSet()).contains(BlockTags.LOGS);
    }
    public static boolean isFallingLogBlock(BlockState bs) {
        return List.of(
                        BlockRegistrar.FALLING_OAK_LOG.get(),
                        BlockRegistrar.FALLING_BIRCH_LOG.get(),
                        BlockRegistrar.FALLING_ACACIA_LOG.get(),
                        BlockRegistrar.FALLING_DARK_OAK_LOG.get(),
                        BlockRegistrar.FALLING_JUNGLE_LOG.get(),
                        BlockRegistrar.FALLING_MANGROVE_LOG.get(),
                        BlockRegistrar.FALLING_SPRUCE_LOG.get(),
                        BlockRegistrar.FALLING_CRIMSON_STEM.get(),
                        BlockRegistrar.FALLING_WARPED_STEM.get())
                .contains(bs.getBlock());
    }

    public static BlockState getNonFallingLog(BlockState bs) {
        BlockState nonFallingLogBlock = bs;
        if (bs.getBlock() == BlockRegistrar.FALLING_OAK_LOG.get()) nonFallingLogBlock = Blocks.OAK_LOG.defaultBlockState();
        else if (bs.getBlock() == BlockRegistrar.FALLING_BIRCH_LOG.get()) nonFallingLogBlock = Blocks.BIRCH_LOG.defaultBlockState();
        else if (bs.getBlock() == BlockRegistrar.FALLING_ACACIA_LOG.get()) nonFallingLogBlock = Blocks.ACACIA_LOG.defaultBlockState();
        else if (bs.getBlock() == BlockRegistrar.FALLING_DARK_OAK_LOG.get()) nonFallingLogBlock = Blocks.DARK_OAK_LOG.defaultBlockState();
        else if (bs.getBlock() == BlockRegistrar.FALLING_JUNGLE_LOG.get()) nonFallingLogBlock = Blocks.JUNGLE_LOG.defaultBlockState();
        else if (bs.getBlock() == BlockRegistrar.FALLING_MANGROVE_LOG.get()) nonFallingLogBlock = Blocks.MANGROVE_LOG.defaultBlockState();
        else if (bs.getBlock() == BlockRegistrar.FALLING_SPRUCE_LOG.get()) nonFallingLogBlock = Blocks.SPRUCE_LOG.defaultBlockState();
        else if (bs.getBlock() == BlockRegistrar.FALLING_CRIMSON_STEM.get()) nonFallingLogBlock = Blocks.CRIMSON_STEM.defaultBlockState();
        else if (bs.getBlock() == BlockRegistrar.FALLING_WARPED_STEM.get()) nonFallingLogBlock = Blocks.WARPED_STEM.defaultBlockState();
        return nonFallingLogBlock;
    }

    public static boolean isLeafBlock(BlockState bs) {
        if (bs.is(BlockTags.LEAVES))
            return true;
        return List.of(Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.SPRUCE_LEAVES,
                        BlockRegistrar.DECAYABLE_NETHER_WART_BLOCK.get(), BlockRegistrar.DECAYABLE_WARPED_WART_BLOCK.get(),
                        Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK)
                .contains(bs.getBlock());
    }

    public static boolean isBottomSlab(BlockState bs) {
        if (bs.getBlock() instanceof SlabBlock) {
            SlabType type = bs.getValue(SlabBlock.TYPE);
            return type == SlabType.BOTTOM;
        }
        return false;
    }


    private static boolean isWraithSnow(BlockState bs) {
        return bs.getBlock() == BlockRegistrar.WRAITH_SNOW_LAYER.get();
    }
    private static boolean isVanillaSnow(BlockState bs) {
        return bs.getBlock() == Blocks.SNOW;
    }
    public static int getWraithSnowLayers(BlockState bs) {
        return isWraithSnow(bs) ? bs.getValue(BlockStateProperties.LAYERS) : 0;
    }
    public static int getSnowLayers(BlockState bs) {
        return isWraithSnow(bs) || isVanillaSnow(bs) ? bs.getValue(BlockStateProperties.LAYERS) : 0;
    }
    public static boolean canPlaceSnow(Level level, BlockPos pos) {
        return !MiscUtil.isSolidBlocking(level, pos) &&
                MiscUtil.isSolidBlocking(level, pos.below()) &&
                !BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), pos);
    }
}
