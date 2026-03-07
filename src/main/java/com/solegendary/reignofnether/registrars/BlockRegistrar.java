package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.blocks.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class BlockRegistrar {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ReignOfNether.MOD_ID);

    private static FallingRotatedPillarBlock fallingLog(MapColor pTopColor, MapColor pBarkColor) {
        return new FallingRotatedPillarBlock(BlockBehaviour.Properties.of().mapColor((p_152624_) -> {
            return p_152624_.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? pTopColor : pBarkColor;
        }).strength(2.0F).sound(SoundType.WOOD));
    }

    private static FallingRotatedPillarBlock fallingNetherStem(MapColor pMapColor) {
        return new FallingRotatedPillarBlock(BlockBehaviour.Properties.of().mapColor((p_152620_) -> {
            return pMapColor;
        }).strength(2.0F).sound(SoundType.STEM));
    }

    public static final RegistryObject<Block> DECAYABLE_NETHER_WART_BLOCK = registerBlock("decayable_nether_wart_block",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.ACACIA_LEAVES).mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .randomTicks()
                    .mapColor(MapColor.COLOR_RED)
                    .sound(SoundType.WART_BLOCK)),
            CreativeModeTabs.BUILDING_BLOCKS
    );

    public static final RegistryObject<Block> DECAYABLE_WARPED_WART_BLOCK = registerBlock("decayable_warped_wart_block",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.ACACIA_LEAVES).mapColor(MapColor.WARPED_WART_BLOCK)
                    .strength(1.0F)
                    .randomTicks()
                    .mapColor(MapColor.WARPED_WART_BLOCK)
                    .sound(SoundType.WART_BLOCK)),
            CreativeModeTabs.BUILDING_BLOCKS
    );

    public static final RegistryObject<Block> FALLING_OAK_LOG = registerBlock("falling_oak_log",
            () -> fallingLog(MapColor.WOOD, MapColor.PODZOL),
            CreativeModeTabs.BUILDING_BLOCKS
    );
    public static final RegistryObject<Block> FALLING_SPRUCE_LOG = registerBlock("falling_spruce_log",
            () -> fallingLog(MapColor.PODZOL, MapColor.COLOR_BROWN),
            CreativeModeTabs.BUILDING_BLOCKS
    );
    public static final RegistryObject<Block> FALLING_BIRCH_LOG = registerBlock("falling_birch_log",
            () -> fallingLog(MapColor.SAND, MapColor.QUARTZ),
            CreativeModeTabs.BUILDING_BLOCKS
    );
    public static final RegistryObject<Block> FALLING_JUNGLE_LOG = registerBlock("falling_jungle_log",
            () -> fallingLog(MapColor.DIRT, MapColor.PODZOL),
            CreativeModeTabs.BUILDING_BLOCKS
    );
    public static final RegistryObject<Block> FALLING_ACACIA_LOG = registerBlock("falling_acacia_log",
            () -> fallingLog(MapColor.COLOR_ORANGE, MapColor.STONE),
            CreativeModeTabs.BUILDING_BLOCKS
    );
    public static final RegistryObject<Block> FALLING_DARK_OAK_LOG = registerBlock("falling_dark_oak_log",
            () -> fallingLog(MapColor.COLOR_BROWN, MapColor.COLOR_BROWN),
            CreativeModeTabs.BUILDING_BLOCKS
    );
    public static final RegistryObject<Block> FALLING_MANGROVE_LOG = registerBlock("falling_mangrove_log",
            () -> fallingLog(MapColor.COLOR_RED, MapColor.PODZOL),
            CreativeModeTabs.BUILDING_BLOCKS
    );
    public static final RegistryObject<Block> FALLING_WARPED_STEM = registerBlock("falling_warped_stem",
            () -> fallingNetherStem(MapColor.WARPED_STEM),
            CreativeModeTabs.BUILDING_BLOCKS
    );
    public static final RegistryObject<Block> FALLING_CRIMSON_STEM = registerBlock("falling_crimson_stem",
            () -> fallingNetherStem(MapColor.CRIMSON_STEM),
            CreativeModeTabs.BUILDING_BLOCKS
    );
    public static final RegistryObject<Block> WALKABLE_MAGMA_BLOCK = registerBlock("walkable_magma_block", () ->
                    new WalkableMagmaBlock(BlockBehaviour
                            .Properties.copy(Blocks.STONE).mapColor(MapColor.NETHER)
                            .requiresCorrectToolForDrops()
                            .lightLevel((p_50828_) -> 3)
                            .randomTicks().strength(0.5F)
                            .isValidSpawn((p_187421_, p_187422_, p_187423_, p_187424_) -> p_187424_.fireImmune())
                            .hasPostProcess(BlockRegistrar::always).emissiveRendering(BlockRegistrar::always)),
            CreativeModeTabs.BUILDING_BLOCKS);

    public static final RegistryObject<Block> WRAITH_SNOW_LAYER = registerBlock("wraith_snow_layer_block",
            () -> new WraithSnowLayerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
                    .replaceable()
                    .forceSolidOff()
                    .randomTicks()
                    .strength(0.1F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.SNOW)
                    .noCollission()
                    .isViewBlocking((bs, blockGetter, bp) -> bs.getValue(SnowLayerBlock.LAYERS) >= 8)
                    .pushReaction(PushReaction.DESTROY)),
            CreativeModeTabs.BUILDING_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_BLUE = registerBlock("rts_start_block_blue", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_BLUE)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_YELLOW = registerBlock("rts_start_block_yellow", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_YELLOW)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_GREEN = registerBlock("rts_start_block_green", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_GREEN)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_RED = registerBlock("rts_start_block_red", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_RED)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_ORANGE = registerBlock("rts_start_block_orange", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_ORANGE)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_CYAN = registerBlock("rts_start_block_cyan", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_CYAN)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_MAGENTA = registerBlock("rts_start_block_magenta", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_MAGENTA)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_BROWN = registerBlock("rts_start_block_brown", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_BROWN)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_WHITE = registerBlock("rts_start_block_white", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.SNOW)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_BLACK = registerBlock("rts_start_block_black", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_LIGHT_BLUE = registerBlock("rts_start_block_light_blue", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_LIME = registerBlock("rts_start_block_lime", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_LIGHT_GRAY = registerBlock("rts_start_block_light_gray", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_GRAY = registerBlock("rts_start_block_gray", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_PURPLE = registerBlock("rts_start_block_purple", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_START_BLOCK_PINK = registerBlock("rts_start_block_pink", () ->
            new RTSStartBlock(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_PINK)
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> RTS_STRUCTURE_BLOCK = registerBlock("rts_structure_block", () ->
            new RTSStructureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops()
                    .strength(-1.0F, 3600000.0F).noLootTable()), CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> GARRISON_ENTRY_BLOCK = registerBlock("garrison_entry_block", () ->
                    new GarrisonEntryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()
                            .noOcclusion()
                            .noCollission()),
            CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> GARRISON_EXIT_BLOCK = registerBlock("garrison_exit_block", () ->
                    new GarrisonExitBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()
                            .noOcclusion()
                            .noCollission()),
            CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> GARRISON_ZONE_BLOCK = registerBlock("garrison_zone_block", () ->
                    new GarrisonZoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()
                            .noOcclusion()
                            .noCollission()),
            CreativeModeTabs.FUNCTIONAL_BLOCKS);

    public static final RegistryObject<Block> UNEXTINGUISHABLE_SOUL_FIRE = registerBlock("unextinguishable_soul_fire", () ->
                    new UnextinguishableSoulFireBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .replaceable()
                            .noCollission()
                            .instabreak()
                            .randomTicks()
                            .lightLevel((p_152605_) -> 10)));

    private static boolean always(BlockState p_50775_, BlockGetter p_50776_, BlockPos p_50777_) {
        return true;
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block, ResourceKey<CreativeModeTab> tab) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn, tab);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block,
                                                                            ResourceKey<CreativeModeTab> tab) {
        return ItemRegistrar.ITEMS.register(name, () -> {
            BlockItem blockItem = new BlockItem(block.get(), new Item.Properties());
            blockItems.computeIfAbsent(tab, (k)-> new ArrayList<>()).add(blockItem);
            return blockItem;
        });
    }

    public static Map<ResourceKey<CreativeModeTab>, List<Item>> blockItems = new HashMap<>();

    public static void init(FMLJavaModLoadingContext context) {
        BLOCKS.register(context.getModEventBus());
    }
}
