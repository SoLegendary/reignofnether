package com.solegendary.reignofnether.nether;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Material;

import java.util.List;
import java.util.Map;

// mappings of which overworld blocks will transform into which nether blocks when near a netherling base

public class NetherBlockMappings {

    public static final Map<Block, List<Block>> CRIMSON_NYLIUM = Map.of(Blocks.CRIMSON_NYLIUM,
            List.of(
                    Blocks.GRASS_BLOCK,
                    Blocks.DIRT,
                    Blocks.COARSE_DIRT,
                    Blocks.DIRT_PATH,
                    Blocks.ROOTED_DIRT
            ));
    public static final Map<Block, List<Block>> WATER = Map.of(Blocks.WATER,
            List.of(
                    Blocks.LAVA
            ));
    public static final Map<Block, List<Block>> WATER_CAULDRON = Map.of(Blocks.WATER_CAULDRON,
            List.of(
                    Blocks.LAVA_CAULDRON
            ));
    public static final Map<Block, List<Block>> MAGMA_BLOCK = Map.of(Blocks.MAGMA_BLOCK,
            List.of(
                    Blocks.COBBLESTONE
            ));
    public static final Map<Block, List<Block>> SHROOMLIGHT = Map.of(Blocks.SHROOMLIGHT,
            List.of(
                    Blocks.BEE_NEST
            ));
    public static final Map<Block, List<Block>> NETHERRACK = Map.of(Blocks.NETHERRACK,
            List.of(
                    Blocks.STONE,
                    Blocks.TERRACOTTA,
                    Blocks.GRANITE
            ));
    public static final Map<Block, List<Block>> BASALT = Map.of(Blocks.BASALT,
            List.of(
                    Blocks.DIORITE,
                    Blocks.ANDESITE,
                    Blocks.CALCITE
            ));
    public static final Map<Block, List<Block>> BLACKSTONE = Map.of(Blocks.BLACKSTONE,
            List.of(
                    Blocks.DEEPSLATE,
                    Blocks.TUFF
            ));
    public static final Map<Block, List<Block>> OBSIDIAN = Map.of(Blocks.OBSIDIAN,
            List.of(
                    Blocks.CRYING_OBSIDIAN
            ));
    public static final Map<Block, List<Block>> SOUL_SOIL = Map.of(Blocks.SOUL_SOIL,
            List.of(
                    Blocks.SAND
            ));
    public static final Map<Block, List<Block>> SOUL_SAND = Map.of(Blocks.SOUL_SAND,
            List.of(
                    Blocks.SANDSTONE,
                    Blocks.CHISELED_SANDSTONE
            ));
    public static final Map<Block, List<Block>> CRIMSON_STEM = Map.of(Blocks.CRIMSON_STEM,
            List.of(
                    Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG,
                    Blocks.JUNGLE_LOG, Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG
            ));
    public static final Map<Block, List<Block>> NETHER_WART_BLOCK = Map.of(Blocks.NETHER_WART_BLOCK,
            List.of(
                    Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
                    Blocks.JUNGLE_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.SPRUCE_LEAVES
            ));
    public static final Map<Block, List<Block>> CRIMSON_ROOTS = Map.of(Blocks.CRIMSON_ROOTS,
            List.of(
                    Blocks.GRASS,
                    Blocks.TALL_GRASS
            ));
    public static final Map<Block, List<Block>> TWISTING_VINES = Map.of(Blocks.TWISTING_VINES,
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
    public static final Map<Block, List<Block>> CRIMSON_FUNGUS = Map.of(Blocks.CRIMSON_FUNGUS,
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
}
