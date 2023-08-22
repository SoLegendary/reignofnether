package com.solegendary.reignofnether.nether;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// mappings of which overworld blocks will transform into which nether blocks when near a netherling base

public class NetherBlocks {

    public static BlockState getNetherBlock(Level level, BlockPos overworldBp) {
        BlockState overworldBs = level.getBlockState(overworldBp);
        if (!overworldBs.isAir())
            for (Map.Entry<Block, List<Block>> entrySet : CONVERSION_MAPPINGS.entrySet())
                for (Block block : entrySet.getValue())
                    if (overworldBs.getBlock().getName().getString().equals(block.getName().getString()))
                        return entrySet.getKey().defaultBlockState();
        return null;
    }

    public static final Map<Block, List<Block>> CONVERSION_MAPPINGS = new HashMap<>();

    static {
        CONVERSION_MAPPINGS.put(Blocks.LAVA_CAULDRON,
            List.of(Blocks.WATER_CAULDRON)
        );
        CONVERSION_MAPPINGS.put(Blocks.MAGMA_BLOCK,
            List.of(Blocks.COBBLESTONE)
        );
        CONVERSION_MAPPINGS.put(Blocks.SHROOMLIGHT,
            List.of(Blocks.BEE_NEST)
        );
        CONVERSION_MAPPINGS.put(Blocks.SOUL_SOIL,
            List.of(Blocks.SAND)
        );
        CONVERSION_MAPPINGS.put(Blocks.LAVA,
            List.of(
                Blocks.OBSIDIAN, // converting water -> lava alone generates a lot of obsidian
                Blocks.WATER
            ));
        CONVERSION_MAPPINGS.put(Blocks.CRIMSON_NYLIUM,
            List.of(
                Blocks.GRASS_BLOCK,
                Blocks.DIRT,
                Blocks.COARSE_DIRT,
                Blocks.DIRT_PATH,
                Blocks.ROOTED_DIRT
            ));
        CONVERSION_MAPPINGS.put(Blocks.NETHERRACK,
            List.of(
                Blocks.STONE,
                Blocks.TERRACOTTA,
                Blocks.GRANITE
            ));
        CONVERSION_MAPPINGS.put(Blocks.BASALT,
            List.of(
                Blocks.DIORITE,
                Blocks.ANDESITE,
                Blocks.CALCITE
            ));
        CONVERSION_MAPPINGS.put(Blocks.BLACKSTONE,
            List.of(
                Blocks.DEEPSLATE,
                Blocks.TUFF
            ));
        CONVERSION_MAPPINGS.put(Blocks.WARPED_FUNGUS,
            List.of(
                Blocks.SUNFLOWER,
                Blocks.DANDELION,
                Blocks.CORNFLOWER,
                Blocks.BLUE_ORCHID,
                Blocks.POPPY,
                Blocks.PEONY,
                Blocks.LILAC,
                Blocks.ALLIUM,
                Blocks.AZURE_BLUET,
                Blocks.OXEYE_DAISY,
                Blocks.LILY_OF_THE_VALLEY,
                Blocks.ORANGE_TULIP,
                Blocks.WHITE_TULIP,
                Blocks.RED_TULIP,
                Blocks.PINK_TULIP
            ));
        CONVERSION_MAPPINGS.put(Blocks.SOUL_SAND,
            List.of(
                Blocks.SANDSTONE,
                Blocks.CHISELED_SANDSTONE
            ));
        CONVERSION_MAPPINGS.put(Blocks.WARPED_STEM,
            List.of(
                Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG,
                Blocks.JUNGLE_LOG, Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG
            ));
        CONVERSION_MAPPINGS.put(Blocks.WARPED_WART_BLOCK,
            List.of(
                Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
                Blocks.JUNGLE_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.SPRUCE_LEAVES
            ));
        CONVERSION_MAPPINGS.put(Blocks.WARPED_ROOTS,
            List.of(
                Blocks.GRASS,
                Blocks.TALL_GRASS
            ));
        CONVERSION_MAPPINGS.put(Blocks.TWISTING_VINES,
            List.of(
                Blocks.FERN,
                Blocks.LARGE_FERN,
                Blocks.ROSE_BUSH,
                Blocks.DEAD_BUSH,
                Blocks.SWEET_BERRY_BUSH,
                Blocks.MANGROVE_ROOTS,
                Blocks.MUDDY_MANGROVE_ROOTS,
                Blocks.AZALEA,
                Blocks.FLOWERING_AZALEA
            ));
    }
}
