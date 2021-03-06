package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemRegistrar {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ReignOfNether.MOD_ID);

    public static final RegistryObject<ForgeSpawnEggItem> SKELETON_UNIT_SPAWN_EGG =
            ITEMS.register("skeleton_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.SKELETON_UNIT,
                    0xa7a7a7, 0x3a3a3a, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<ForgeSpawnEggItem> ZOMBIE_UNIT_SPAWN_EGG =
            ITEMS.register("zombie_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.ZOMBIE_UNIT,
                    0x009999, 0x577048, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<ForgeSpawnEggItem> CREEPER_UNIT_SPAWN_EGG =
            ITEMS.register("creeper_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.CREEPER_UNIT,
                    0x0c990a, 0x000000, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static void init() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
