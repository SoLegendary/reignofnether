package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.blocks.RTSStartBlock;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.item.Items;
import com.solegendary.reignofnether.items.FoilableItem;
import com.solegendary.reignofnether.items.CreativeModeTabsRegistrar;

/**
 * The mod's own creative tabs: structure blocks, unit spawn eggs, and unit items.
 *
 * <p>1.5.0 added these. Two port notes: {@code RegistryObject} no longer exists in NeoForge 21.1
 * (the deferred registers hand out {@link Supplier}), and {@code DeferredRegister#getEntries()}
 * is gone too - the tab contents are collected from the built registries instead, filtered to this
 * mod's namespace so vanilla items of the same kind stay out.
 */
public class CreativeModeTabsRegistrar {

    private static final String MOD_NAMESPACE = ReignOfNether.MOD_ID;

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ReignOfNether.MOD_ID);

    /** This mod's items, read from the built item registry. */
    private static Iterable<Item> modItems() {
        return BuiltInRegistries.ITEM.registryKeySet().stream()
                .filter(key -> key.location().getNamespace().equals(MOD_NAMESPACE))
                .map(BuiltInRegistries.ITEM::get)
                .toList();
    }

    /** This mod's blocks, read from the built block registry. */
    private static Iterable<Block> modBlocks() {
        return BuiltInRegistries.BLOCK.registryKeySet().stream()
                .filter(key -> key.location().getNamespace().equals(MOD_NAMESPACE))
                .map(BuiltInRegistries.BLOCK::get)
                .toList();
    }

    public static final Supplier<CreativeModeTab> CUSTOM_BUILDINGS =
            CREATIVE_MODE_TABS.register("custom_buildings",
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
                                output.accept(BlockRegistrar.TEMPORARY_WALKABLE_MAGMA_BLOCK.get());
                                output.accept(BlockRegistrar.HORIZONTAL_PORTAL.get());
                                for (var block : modBlocks())
                                    if (block instanceof RTSStartBlock)
                                        output.accept(block);
                            })
                            .build());

    public static final Supplier<CreativeModeTab> UNIT_SPAWN_EGGS =
            CREATIVE_MODE_TABS.register("unit_spawn_eggs",
                    () -> CreativeModeTab.builder()
                            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                            .icon(() -> new ItemStack(ItemRegistrar.BRUTE_UNIT_SPAWN_EGG.get()))
                            .title(Component.translatable("creativetab.reignofnether.unit_spawn_eggs"))
                            .displayItems((parameters, output) -> {
                                for (Item item : modItems())
                                    if (item instanceof SpawnEggItem)
                                        output.accept(item);
                            })
                            .build());

    public static final Supplier<CreativeModeTab> UNIT_ITEMS =
            CREATIVE_MODE_TABS.register("unit_items",
                    () -> CreativeModeTab.builder()
                            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                            .icon(() -> new ItemStack(ItemRegistrar.HEART_MEDALLION.get()))
                            .title(Component.translatable("creativetab.reignofnether.unit_items"))
                            .displayItems((parameters, output) -> {
                                for (Item item : modItems())
                                    if (item instanceof FoilableItem)
                                        output.accept(item);
                                output.accept(ItemRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get());
                                output.accept(net.minecraft.world.item.Items.BELL);
                                output.accept(net.minecraft.world.item.Items.SPYGLASS);
                                output.accept(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
                                output.accept(net.minecraft.world.item.Items.ENDER_PEARL);
                                output.accept(ItemRegistrar.THROWABLE_TNT.get());
                            })
                            .build());

    public static void init(ModContainer container) {
        CREATIVE_MODE_TABS.register(container.getEventBus());
    }
}
