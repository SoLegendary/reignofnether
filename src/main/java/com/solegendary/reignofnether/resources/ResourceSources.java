package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.IPlantable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ResourceSources {

    public static final List<Predicate<BlockState>> CLEAR_MATERIALS = List.of(
            (s)->s.getFluidState().is(FluidTags.WATER),
            (s)->s.isAir(),
            (s)->s.is(BlockTags.LEAVES),
            (s)->s.getBlock() instanceof IPlantable
    );

    public static boolean isClearMaterial(BlockState state){
        for (Predicate<BlockState> predicate : CLEAR_MATERIALS){
            if(predicate.test(state)){
                return true;
            }
        }
        return false;
    }

    public static final List<Block> GATHERABLE_PLANTS = List.of(
            Blocks.SWEET_BERRY_BUSH, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM
    );

    public static final int TICKS_PER_SECOND = 20;

    public static boolean isHuntableAnimal(LivingEntity entity) {
        if (!(entity instanceof Animal))
            return false;
        if (getFoodItemsFromAnimal((Animal) entity).size() == 0)
            return false;
        return true;
    }

    public static ResourceSource getFromBlockPos(BlockPos bp, Level level) {
        BlockState bs = level.getBlockState(bp);
        Block block = bs.getBlock();

        for (List<ResourceSource> resourceSources : List.of(FOOD_BLOCKS, WOOD_BLOCKS, ORE_BLOCKS))
            for (ResourceSource resourceSource : resourceSources)
                if (resourceSource.validBlocks.contains(block))
                    return resourceSource;
        if (bs.getTags().collect(Collectors.toSet()).contains(BlockTags.LOGS))
            return GENERIC_LOG_BLOCK;
        if (bs.getTags().collect(Collectors.toSet()).contains(BlockTags.LEAVES))
            return GENERIC_LEAVES_BLOCK;
        return null;
    }

    public static ResourceSource getFromBlockState(BlockState bs) {
        for (List<ResourceSource> resourceSources : List.of(FOOD_BLOCKS, WOOD_BLOCKS, ORE_BLOCKS))
            for (ResourceSource resourceSource : resourceSources)
                if (resourceSource.validBlocks.contains(bs.getBlock()))
                    return resourceSource;
        if (bs.getTags().collect(Collectors.toSet()).contains(BlockTags.LOGS))
            return GENERIC_LOG_BLOCK;
        if (bs.getTags().collect(Collectors.toSet()).contains(BlockTags.LEAVES))
            return GENERIC_LEAVES_BLOCK;
        return null;
    }

    public static ResourceName getBlockResourceName(BlockPos bp, Level level) {
        Block block = level.getBlockState(bp).getBlock();

        if (block == Blocks.FARMLAND)
            return ResourceName.FOOD;

        ResourceSource resBlock = getFromBlockPos(bp, level);
        if (resBlock != null)
            return resBlock.resourceName;

        return ResourceName.NONE;
    }

    // is the given item an item that is worth resources?
    // used for unit item pickups and player resource deposits
    public static ResourceSource getFromItem(Item item) {
        for (List<ResourceSource> resourceSources : List.of(FOOD_BLOCKS, WOOD_BLOCKS, ORE_BLOCKS))
            for (ResourceSource resourceSource : resourceSources) {
                if (resourceSource.items.contains(item))
                    return resourceSource;
                for (Block block : resourceSource.validBlocks)
                    if (block.asItem() == item)
                        return resourceSource;
            }
        return null;
    }

    // return a list of food items that a worker gets when killing a huntable animal to make it more consistent
    public static List<ItemStack> getFoodItemsFromAnimal(Animal animal) {
        if (animal instanceof MushroomCow) {
            return List.of(new ItemStack(Items.BROWN_MUSHROOM, 3), new ItemStack(Items.RED_MUSHROOM, 4)); // 175 food / 10hp
        } else if (animal instanceof Cow) {
            return List.of(new ItemStack(Items.BEEF, 2), new ItemStack(Items.LEATHER, 2)); // 150 food / 10hp
        } else if (animal instanceof Pig) {
            return List.of(new ItemStack(Items.PORKCHOP, 3)); // 150 food / 10hp
        } else if (animal instanceof Goat) {
            return List.of(new ItemStack(Items.MUTTON, 2), new ItemStack(Items.LEATHER, 2)); // 150 food / 10hp
        } else if (animal instanceof Sheep) {
            return List.of(new ItemStack(Items.MUTTON, 2), new ItemStack(Items.LEATHER, 1)); // 125 food / 8hp
        } else if (animal instanceof Chicken) {
            return List.of(new ItemStack(Items.CHICKEN, 1)); // 50 food / 4hp (but usually lays eggs around)
        } else if (animal instanceof Rabbit) {
            return List.of(new ItemStack(Items.RABBIT, 1)); // 50 food / 3hp
        } else if (animal instanceof Horse) {
            return List.of(new ItemStack(Items.LEATHER, 2)); // 50 food
        } else if (animal instanceof Donkey) {
            return List.of(new ItemStack(Items.LEATHER, 2)); // 50 food
        } else if (animal instanceof Mule) {
            return List.of(new ItemStack(Items.LEATHER, 2)); // 50 food
        }
        return List.of();
    }

    private static List<Item> edibleFoods = List.of(
        Items.COOKED_BEEF,
        Items.COOKED_CHICKEN,
        Items.COOKED_COD,
        Items.COOKED_PORKCHOP,
        Items.COOKED_RABBIT,
        Items.COOKED_SALMON,
        Items.COOKED_MUTTON,
        Items.COOKIE,
        Items.BREAD,
        Items.PUMPKIN_PIE,
        Items.MUSHROOM_STEW,
        Items.RABBIT_STEW,
        Items.BEETROOT_SOUP,
        Items.BAKED_POTATO,
        Items.GOLDEN_APPLE,
        Items.ENCHANTED_GOLDEN_APPLE,
        Items.GOLDEN_CARROT
    );

    public static boolean isPreparedFood(Item item) {
        return item.isEdible() && edibleFoods.contains(item);
    }

    public static final int REPLANT_TICKS_MAX = 10;

    public static final ResourceSource GENERIC_LOG_BLOCK = new ResourceSource("Unknown Logs",
            List.of(),
            List.of(Items.OAK_LOG),
            TICKS_PER_SECOND * 12,
            15,
            ResourceName.WOOD
    );
    public static final ResourceSource GENERIC_LEAVES_BLOCK = new ResourceSource("Unknown Leaves",
            List.of(),
            List.of(Items.OAK_LEAVES),
            8,
            1,
            ResourceName.WOOD
    );

    public static final List<ResourceSource> FOOD_BLOCKS = List.of(
            new ResourceSource("Sugar", // item only
                    List.of(),
                    List.of(Items.SUGAR),
                    0,
                    1,
                    ResourceName.FOOD
            ),
            new ResourceSource("Farmland",
                    List.of(Blocks.FARMLAND),
                    List.of(),
                    0,
                    0,
                    ResourceName.FOOD,
                    (bs) -> bs.getValue(BlockStateProperties.MOISTURE) == 7
            ),
            new ResourceSource("Soul Sand",
                    List.of(Blocks.SOUL_SAND),
                    List.of(),
                    0,
                    0,
                    ResourceName.FOOD
            ),
            // VILLAGER FARMS
            new ResourceSource("Wheat",
                    List.of(Blocks.WHEAT),
                    List.of(Items.WHEAT),
                    TICKS_PER_SECOND * 2,
                    4,
                    ResourceName.FOOD,
                    (bs) -> bs.getValue(BlockStateProperties.AGE_7) == 7
            ),
            // PIGLIN FARMS
            new ResourceSource("Netherwart",
                    List.of(Blocks.NETHER_WART),
                    List.of(Items.NETHER_WART),
                    TICKS_PER_SECOND * 2,
                    5,
                    ResourceName.FOOD,
                    (bs) -> bs.getValue(BlockStateProperties.AGE_3) == 3
            ),
            // MONSTER FARMS
            new ResourceSource("Gourds",
                    List.of(Blocks.MELON, Blocks.PUMPKIN, Blocks.CARVED_PUMPKIN),
                    List.of(Items.MELON, Items.PUMPKIN, Items.CARVED_PUMPKIN),
                    TICKS_PER_SECOND * 4,
                    6,
                    ResourceName.FOOD
            ),
            new ResourceSource("Carrots",
                    List.of(Blocks.CARROTS),
                    List.of(Items.CARROT),
                    TICKS_PER_SECOND * 2,
                    5,
                    ResourceName.FOOD,
                    (bs) -> bs.getValue(BlockStateProperties.AGE_7) == 7
            ),
            new ResourceSource("Potatoes",
                    List.of(Blocks.POTATOES),
                    List.of(Items.POTATO),
                    TICKS_PER_SECOND * 2,
                    6,
                    ResourceName.FOOD,
                    (bs) -> bs.getValue(BlockStateProperties.AGE_7) == 7
            ),
            new ResourceSource("Beetroots",
                    List.of(Blocks.BEETROOTS),
                    List.of(Items.BEETROOT),
                    TICKS_PER_SECOND * 2,
                    5,
                    ResourceName.FOOD,
                    (bs) -> bs.getValue(BlockStateProperties.AGE_3) == 3
            ),
            new ResourceSource("Mushrooms",
                    List.of(Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM),
                    List.of(Items.RED_MUSHROOM, Items.BROWN_MUSHROOM),
                    TICKS_PER_SECOND * 5,
                    25,
                    ResourceName.FOOD
            ),
            new ResourceSource("Misc. Forageable",
                    List.of(Blocks.SUGAR_CANE, Blocks.SWEET_BERRY_BUSH),
                    List.of(Items.SWEET_BERRIES, Items.SUGAR_CANE),
                    TICKS_PER_SECOND * 5,
                    20,
                    ResourceName.FOOD
            ),
            new ResourceSource("Mushroom Stem",
                    List.of(Blocks.MUSHROOM_STEM),
                    List.of(Items.MUSHROOM_STEM),
                    TICKS_PER_SECOND * 10,
                    10,
                    ResourceName.FOOD
            ),
            new ResourceSource("Red Mushroom Block",
                    List.of(Blocks.RED_MUSHROOM_BLOCK),
                    List.of(Items.RED_MUSHROOM_BLOCK),
                    TICKS_PER_SECOND * 10,
                    10,
                    ResourceName.FOOD
            ),
            new ResourceSource("Brown Mushroom Block",
                    List.of(Blocks.BROWN_MUSHROOM_BLOCK),
                    List.of(Items.BROWN_MUSHROOM_BLOCK),
                    TICKS_PER_SECOND * 10,
                    10,
                    ResourceName.FOOD
            ),
            new ResourceSource("Bee nest",
                    List.of(Blocks.BEE_NEST),
                    List.of(Items.BEE_NEST),
                    TICKS_PER_SECOND * 10,
                    100,
                    ResourceName.FOOD
            ),
            new ResourceSource("Shroomlight",
                    List.of(Blocks.SHROOMLIGHT),
                    List.of(Items.SHROOMLIGHT),
                    TICKS_PER_SECOND * 10,
                    120,
                    ResourceName.FOOD
            ),
            new ResourceSource("Misc food item",
                    List.of(),
                    List.of(Items.CHICKEN),
                    0,
                    75,
                    ResourceName.FOOD
            ),
            new ResourceSource("Large food item",
                    List.of(),
                    List.of(Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.RABBIT),
                    0,
                    50,
                    ResourceName.FOOD
            ),
            new ResourceSource("Medium food item",
                    List.of(),
                    List.of(Items.LEATHER, Items.EGG, Items.APPLE, Items.HONEY_BOTTLE, Items.COD, Items.SALMON, Items.GLOW_BERRIES),
                    0,
                    25,
                    ResourceName.FOOD
            )
    );

    public static final List<ResourceSource> WOOD_BLOCKS = List.of(
            new ResourceSource("Stick", // item only
                    List.of(),
                    List.of(Items.STICK),
                    0,
                    1,
                    ResourceName.WOOD
            ),
            new ResourceSource("Saplings", // item only
                    List.of(),
                    List.of(Items.ACACIA_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING, Items.OAK_SAPLING, Items.OAK_SAPLING, Items.DARK_OAK_SAPLING, Items.JUNGLE_SAPLING),
                    0,
                    5,
                    ResourceName.WOOD
            ),
            /*
            new ResourceSource("Planks",
                    List.of(Blocks.OAK_PLANKS, Blocks.BIRCH_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.JUNGLE_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.SPRUCE_PLANKS),
                    List.of(Items.OAK_PLANKS, Items.BIRCH_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.JUNGLE_PLANKS, Items.MANGROVE_PLANKS, Items.SPRUCE_PLANKS),
                    TICKS_PER_SECOND * 2,
                    3,
                    ResourceName.WOOD
            ), */
            new ResourceSource("Logs",
                    List.of(Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.JUNGLE_LOG, Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG,
                            BlockRegistrar.FALLING_OAK_LOG.get(),
                            BlockRegistrar.FALLING_BIRCH_LOG.get(),
                            BlockRegistrar.FALLING_ACACIA_LOG.get(),
                            BlockRegistrar.FALLING_DARK_OAK_LOG.get(),
                            BlockRegistrar.FALLING_JUNGLE_LOG.get(),
                            BlockRegistrar.FALLING_MANGROVE_LOG.get(),
                            BlockRegistrar.FALLING_SPRUCE_LOG.get(),
                            Blocks.OAK_WOOD, Blocks.BIRCH_WOOD, Blocks.ACACIA_WOOD, Blocks.DARK_OAK_WOOD, Blocks.JUNGLE_WOOD, Blocks.MANGROVE_WOOD, Blocks.SPRUCE_WOOD),
                    List.of(Items.OAK_LOG, Items.BIRCH_LOG, Items.ACACIA_LOG, Items.DARK_OAK_LOG, Items.JUNGLE_LOG, Items.MANGROVE_LOG, Items.SPRUCE_LOG,
                            Items.OAK_WOOD, Items.BIRCH_WOOD, Items.ACACIA_WOOD, Items.DARK_OAK_WOOD, Items.JUNGLE_WOOD, Items.MANGROVE_WOOD, Items.SPRUCE_WOOD),
                    TICKS_PER_SECOND * 12,
                    15,
                    ResourceName.WOOD
            ),
            new ResourceSource("Roots",
                    List.of(Blocks.MUDDY_MANGROVE_ROOTS, Blocks.MANGROVE_ROOTS),
                    List.of(Items.MUDDY_MANGROVE_ROOTS, Items.MANGROVE_ROOTS),
                    TICKS_PER_SECOND * 12,
                    15,
                    ResourceName.WOOD
            ),
            new ResourceSource("Nether Logs",
                    List.of(Blocks.CRIMSON_STEM, Blocks.WARPED_STEM, Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE,
                            BlockRegistrar.FALLING_WARPED_STEM.get(),
                            BlockRegistrar.FALLING_CRIMSON_STEM.get()
                    ),
                    List.of(Items.CRIMSON_STEM, Items.WARPED_STEM, Items.CRIMSON_HYPHAE, Items.WARPED_HYPHAE),
                    TICKS_PER_SECOND * 12,
                    18,
                    ResourceName.WOOD
            ),
            new ResourceSource("Leaves",
                    List.of(Blocks.ACACIA_LEAVES, Blocks.AZALEA_LEAVES, Blocks.BIRCH_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.DARK_OAK_LEAVES,
                            Blocks.MANGROVE_LEAVES, Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, BlockRegistrar.DECAYABLE_NETHER_WART_BLOCK.get(), BlockRegistrar.DECAYABLE_NETHER_WART_BLOCK.get()),
                    List.of(Items.ACACIA_LEAVES, Items.AZALEA_LEAVES, Items.BIRCH_LEAVES, Items.FLOWERING_AZALEA_LEAVES, Items.JUNGLE_LEAVES, Items.DARK_OAK_LEAVES,
                            Items.MANGROVE_LEAVES, Items.OAK_LEAVES, Items.SPRUCE_LEAVES),
                    8,
                    1,
                    ResourceName.WOOD
            ),
            new ResourceSource("Leaves",
                    List.of(Blocks.VINE),
                    List.of(Items.VINE),
                    8,
                    1,
                    ResourceName.WOOD
            )
    );

    public static final List<ResourceSource> ORE_BLOCKS = List.of(
            new ResourceSource("Stone", // item only
                    List.of(),
                    List.of(Items.STONE),
                    0,
                    1,
                    ResourceName.ORE
            ),
            new ResourceSource("Dripstone",
                    List.of(Blocks.POINTED_DRIPSTONE),
                    List.of(Items.POINTED_DRIPSTONE),
                    20,
                    1,
                    ResourceName.ORE
            ),
            new ResourceSource("Tier 1 Nether Ores",
                    List.of(Blocks.NETHER_QUARTZ_ORE),
                    List.of(Items.QUARTZ),
                    TICKS_PER_SECOND * 45,
                    60,
                    ResourceName.ORE
            ),
            new ResourceSource("Tier 2 Nether Ores",
                    List.of(Blocks.NETHER_GOLD_ORE),
                    List.of(Items.NETHER_GOLD_ORE),
                    TICKS_PER_SECOND * 50,
                    78,
                    ResourceName.ORE
            ),
            new ResourceSource("Tier 3 Nether Ores",
                    List.of(Blocks.GILDED_BLACKSTONE),
                    List.of(Items.GILDED_BLACKSTONE),
                    TICKS_PER_SECOND * 55,
                    96,
                    ResourceName.ORE
            ),
            new ResourceSource("Tier 4 Nether Ores",
                    List.of(Blocks.ANCIENT_DEBRIS),
                    List.of(Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP),
                    TICKS_PER_SECOND * 60,
                    120,
                    ResourceName.ORE
            ),
            new ResourceSource("Tier 1 Ores",
                    List.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE),
                    List.of(Items.COAL),
                    TICKS_PER_SECOND * 45,
                    50,
                    ResourceName.ORE
            ),
            new ResourceSource("Tier 2 Ores",
                    List.of(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.IRON_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_REDSTONE_ORE),
                    List.of(Items.RAW_IRON, Items.RAW_COPPER, Items.LAPIS_LAZULI, Items.REDSTONE, Items.IRON_INGOT),
                    TICKS_PER_SECOND * 50,
                    65,
                    ResourceName.ORE
            ),
            new ResourceSource("Tier 3 Ores",
                    List.of(Blocks.GOLD_ORE, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_EMERALD_ORE),
                    List.of(Items.RAW_GOLD, Items.EMERALD, Items.GOLD_INGOT),
                    TICKS_PER_SECOND * 55,
                    80,
                    ResourceName.ORE
            ),
            new ResourceSource("Tier 4 Ores",
                    List.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
                    List.of(Items.DIAMOND),
                    TICKS_PER_SECOND * 60,
                    100,
                    ResourceName.ORE
            )
    );
}