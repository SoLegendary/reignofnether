package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.blocks.RTSStartBlock;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeModeTabsRegistrar {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ReignOfNether.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CUSTOM_BUILDINGS = CREATIVE_MODE_TABS.register("custom_buildings",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(BlockRegistrar.RTS_STRUCTURE_BLOCK.get()))
                    .title(Component.translatable("creativetab.reignofnether.custom_buildings"))
                    .displayItems((parameters, output) -> {
                        output.accept(BlockRegistrar.RTS_STRUCTURE_BLOCK.get());
                        output.accept(BlockRegistrar.GARRISON_ENTRY_BLOCK.get());
                        output.accept(BlockRegistrar.GARRISON_EXIT_BLOCK.get());
                        output.accept(BlockRegistrar.GARRISON_ZONE_BLOCK.get());
                        output.accept(BlockRegistrar.PRODUCTION_SPAWN_BLOCK.get());
                        output.accept(BlockRegistrar.WALKABLE_MAGMA_BLOCK.get());
                        for (RegistryObject<Block> block : BlockRegistrar.BLOCKS.getEntries())
                            if (block.get() instanceof RTSStartBlock)
                                output.accept(block.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> UNIT_SPAWN_EGGS = CREATIVE_MODE_TABS.register("unit_spawn_eggs",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(ItemRegistrar.BRUTE_UNIT_SPAWN_EGG.get()))
                    .title(Component.translatable("creativetab.reignofnether.unit_spawn_eggs"))
                    .displayItems((parameters, output) -> {
                        for (RegistryObject<Item> item : ItemRegistrar.ITEMS.getEntries())
                            if (item.get() instanceof SpawnEggItem)
                                output.accept(item.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> UNIT_ITEMS = CREATIVE_MODE_TABS.register("unit_items",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(ItemRegistrar.HEART_MEDALLION.get()))
                    .title(Component.translatable("creativetab.reignofnether.unit_items"))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get());
                        for (RegistryObject<Item> item : ItemRegistrar.ITEMS.getEntries())
                            if (item.get() instanceof UnitGiveableItem)
                                output.accept(item.get());
                    })
                    .build());

    public static void init(FMLJavaModLoadingContext context) {
        CREATIVE_MODE_TABS.register(context.getModEventBus());
    }
}
