package com.solegendary.reignofnether.resources;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Material;

import java.util.List;

public class ResourceBlocks {
    public static final List<Material> CLEAR_MATERIALS = List.of(Material.WATER, Material.AIR, Material.GRASS);

    public static ResourceBlock getResourceBlock(BlockPos bp, Level level) {
        Block block = level.getBlockState(bp).getBlock();

        for (List<ResourceBlock> resourceBlocks : List.of(FOOD_BLOCKS, WOOD_BLOCKS, ORE_BLOCKS))
            for (ResourceBlock resourceBlock : resourceBlocks)
                if (resourceBlock.validBlocks.contains(block))
                    return resourceBlock;
        return null;
    }

    public static ResourceName getResourceBlockName(BlockPos bp, Level level) {
        Block block = level.getBlockState(bp).getBlock();

        if (block == Blocks.FARMLAND)
            return ResourceName.FOOD;

        ResourceBlock resBlock = getResourceBlock(bp, level);
        if (resBlock != null)
            return resBlock.resourceName;

        return ResourceName.NONE;
    }

    public static final int REPLANT_TICKS_MAX = 10;
    // TODO: define on worker unit side, possibly with mixed randomness?
    public static final BlockState REPLANT_BLOCKSTATE = Blocks.WHEAT.defaultBlockState();

    public static final List<ResourceBlock> FOOD_BLOCKS = List.of(
            new ResourceBlock("Farmland",
                    List.of(Blocks.FARMLAND),
                    0,
                    0,
                    ResourceName.FOOD,
                    (bs) -> bs.getValue(BlockStateProperties.MOISTURE) == 7
            ),
            new ResourceBlock("Wheat",
                    List.of(Blocks.WHEAT),
                    100,
                    20,
                    ResourceName.FOOD,
                    (bs) -> bs.getValue(BlockStateProperties.AGE_7) == 7
            ),
            new ResourceBlock("Carrots",
                    List.of(Blocks.CARROTS),
                    100,
                    25,
                    ResourceName.FOOD,
                    (bs) -> bs.getValue(BlockStateProperties.AGE_7) == 7
            ),
            new ResourceBlock("Potatoes",
                    List.of(Blocks.POTATOES),
                    100,
                    30,
                    ResourceName.FOOD,
                    (bs) -> bs.getValue(BlockStateProperties.AGE_7) == 7
            ),
            new ResourceBlock("Beetroots",
                    List.of(Blocks.BEETROOTS),
                    100,
                    20,
                    ResourceName.FOOD,
                    (bs) -> bs.getValue(BlockStateProperties.AGE_3) == 3
            ),
            new ResourceBlock("Gourds",
                    List.of(Blocks.MELON, Blocks.PUMPKIN),
                    100,
                    50,
                    ResourceName.FOOD
            ),
            new ResourceBlock("Mushrooms",
                    List.of(Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM),
                    50,
                    30,
                    ResourceName.FOOD
            ),
            new ResourceBlock("Misc. Forageable",
                    List.of(Blocks.SUGAR_CANE, Blocks.SWEET_BERRY_BUSH),
                    50,
                    30,
                    ResourceName.FOOD
            )
    );

    public static final List<ResourceBlock> WOOD_BLOCKS = List.of(
            new ResourceBlock("Logs",
                    List.of(Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.JUNGLE_LOG, Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG),
                    100,
                    25,
                    ResourceName.WOOD
            ),
            new ResourceBlock("Leaves",
                    List.of(Blocks.ACACIA_LEAVES, Blocks.AZALEA_LEAVES, Blocks.BIRCH_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES),
                    4,
                    1,
                    ResourceName.WOOD
            )
    );

    public static final List<ResourceBlock> ORE_BLOCKS = List.of(
            new ResourceBlock("Tier 1 Ores",
                    List.of(Blocks.COAL_ORE, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.NETHER_QUARTZ_ORE),
                    100,
                    25,
                    ResourceName.ORE
            ),
            new ResourceBlock("Tier 2 Ores",
                    List.of(Blocks.IRON_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.NETHER_GOLD_ORE),
                    100,
                    50,
                    ResourceName.ORE
            ),
            new ResourceBlock("Tier 3 Ores",
                    List.of(Blocks.GOLD_ORE, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_EMERALD_ORE),
                    100,
                    200,
                    ResourceName.ORE
            ),
            new ResourceBlock("Tier 4 Ores",
                    List.of(Blocks.DIAMOND_ORE, Blocks.ANCIENT_DEBRIS, Blocks.DEEPSLATE_DIAMOND_ORE),
                    100,
                    500,
                    ResourceName.ORE
            )
    );
}