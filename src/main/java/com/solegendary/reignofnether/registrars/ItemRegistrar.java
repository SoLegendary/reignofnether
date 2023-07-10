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

    public static final RegistryObject<ForgeSpawnEggItem> SPIDER_UNIT_SPAWN_EGG =
            ITEMS.register("spider_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.SPIDER_UNIT,
                    0x322B26, 0x840B0B, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<ForgeSpawnEggItem> POISON_SPIDER_UNIT_SPAWN_EGG =
            ITEMS.register("poison_spider_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.POISON_SPIDER_UNIT,
                    0x0B3F4A, 0x840B0B, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<ForgeSpawnEggItem> VILLAGER_UNIT_SPAWN_EGG =
            ITEMS.register("villager_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.VILLAGER_UNIT,
                    0x523632, 0x946F66, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<ForgeSpawnEggItem> ZOMBIE_VILLAGER_UNIT_SPAWN_EGG =
            ITEMS.register("zombie_villager_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.ZOMBIE_VILLAGER_UNIT,
                    0x523632, 0x647E51, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<ForgeSpawnEggItem> VINDICATOR_UNIT_SPAWN_EGG =
            ITEMS.register("vindicator_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.VINDICATOR_UNIT,
                    0x8B8F90, 0x1F4952, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<ForgeSpawnEggItem> PILLAGER_UNIT_SPAWN_EGG =
            ITEMS.register("pillager_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.PILLAGER_UNIT,
                    0x502C34, 0x757D78, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<ForgeSpawnEggItem> IRON_GOLEM_UNIT_SPAWN_EGG =
            ITEMS.register("iron_golem_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.IRON_GOLEM_UNIT,
                    0x101010, 0x757D78, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<ForgeSpawnEggItem> WITCH_UNIT_SPAWN_EGG =
            ITEMS.register("witch_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.WITCH_UNIT,
                    0x330000, 0x3A732D, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<ForgeSpawnEggItem> ENDERMAN_UNIT_SPAWN_EGG =
            ITEMS.register("enderman_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.ENDERMAN_UNIT,
                    0x1E1E1E, 0x000000, new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static void init() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
