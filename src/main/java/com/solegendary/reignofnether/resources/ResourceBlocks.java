package com.solegendary.reignofnether.resources;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Material;

import java.util.List;

// TODO: make class for blocks individual resource value and hardness
public class ResourceBlocks {
    public static final List<Material> CLEAR_MATERIALS = List.of(Material.LEAVES, Material.WATER, Material.AIR, Material.GRASS);
    public static final List<Block> FARMABLE_BLOCKS = List.of(Blocks.WHEAT, Blocks.POTATOES, Blocks.CARROTS, Blocks.BEETROOTS);
    public static final List<Block> FOOD_BLOCKS = List.of(Blocks.WHEAT, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.SWEET_BERRY_BUSH, Blocks.POTATOES, Blocks.CARROTS, Blocks.BEETROOTS, Blocks.SUGAR_CANE, Blocks.MELON, Blocks.PUMPKIN);
    public static final List<Block> WOOD_BLOCKS = List.of(Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.JUNGLE_LOG, Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG);
    public static final List<Block> ORE_BLOCKS = List.of(Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.EMERALD_ORE, Blocks.COPPER_ORE, Blocks.DIAMOND_ORE, Blocks.REDSTONE_ORE, Blocks.GOLD_ORE, Blocks.LAPIS_ORE);

    public static String getResourceBlockType(BlockPos bp, Level level) {
        Block block = level.getBlockState(bp).getBlock();

        if (ResourceBlocks.FOOD_BLOCKS.contains(block))
            return "Food";
        else if (ResourceBlocks.WOOD_BLOCKS.contains(block))
            return "Wood";
        else if (ResourceBlocks.ORE_BLOCKS.contains(block))
            return "Ore";
        return "None";
    }

}
