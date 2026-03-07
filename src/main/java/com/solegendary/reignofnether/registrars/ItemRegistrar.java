package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.items.HeroExperienceBottleItem;
import com.solegendary.reignofnether.items.ThrowableTnt;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistrar {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ReignOfNether.MOD_ID);

    public static final RegistryObject<ForgeSpawnEggItem> ZOMBIE_UNIT_SPAWN_EGG =
            ITEMS.register("zombie_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.ZOMBIE_UNIT,
                    0x009999, 0x577048, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> HUSK_UNIT_SPAWN_EGG =
            ITEMS.register("husk_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.HUSK_UNIT,
                    0x71695B, 0xB7A276, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> DROWNED_UNIT_SPAWN_EGG =
            ITEMS.register("drowned_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.DROWNED_UNIT,
                    9433559, 7969893, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> ZOMBIE_PIGLIN_UNIT_SPAWN_EGG =
            ITEMS.register("zombie_piglin_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.ZOMBIE_PIGLIN_UNIT,
                    15373203, 5009705, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> ZOGLIN_UNIT =
            ITEMS.register("zoglin_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.ZOGLIN_UNIT,
                    13004373, 15132390, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> SKELETON_UNIT_SPAWN_EGG =
            ITEMS.register("skeleton_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.SKELETON_UNIT,
                    0xa7a7a7, 0x3a3a3a, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> STRAY_UNIT_SPAWN_EGG =
            ITEMS.register("stray_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.STRAY_UNIT,
                    0x5B6F6F, 0xAEB8B8, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> BOGGED_UNIT_SPAWN_EGG =
            ITEMS.register("bogged_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.BOGGED_UNIT,
                    0xa1a387, 0x3e4d12, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> CREEPER_UNIT_SPAWN_EGG =
            ITEMS.register("creeper_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.CREEPER_UNIT,
                    0x0c990a, 0x000000, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> SPIDER_UNIT_SPAWN_EGG =
            ITEMS.register("spider_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.SPIDER_UNIT,
                    0x322B26, 0x840B0B, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> POISON_SPIDER_UNIT_SPAWN_EGG =
            ITEMS.register("poison_spider_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.POISON_SPIDER_UNIT,
                    0x0B3F4A, 0x840B0B, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> VILLAGER_UNIT_SPAWN_EGG =
            ITEMS.register("villager_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.VILLAGER_UNIT,
                    0x523632, 0x946F66, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> MILITIA_UNIT_SPAWN_EGG =
            ITEMS.register("militia_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.MILITIA_UNIT,
                    0x523632, 0x946F66, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> ZOMBIE_VILLAGER_UNIT_SPAWN_EGG =
            ITEMS.register("zombie_villager_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.ZOMBIE_VILLAGER_UNIT,
                    0x523632, 0x647E51, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> VINDICATOR_UNIT_SPAWN_EGG =
            ITEMS.register("vindicator_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.VINDICATOR_UNIT,
                    0x8B8F90, 0x1F4952, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> PILLAGER_UNIT_SPAWN_EGG =
            ITEMS.register("pillager_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.PILLAGER_UNIT,
                    0x502C34, 0x757D78, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> IRON_GOLEM_UNIT_SPAWN_EGG =
            ITEMS.register("iron_golem_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.IRON_GOLEM_UNIT,
                    0x101010, 0x757D78, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> WITCH_UNIT_SPAWN_EGG =
            ITEMS.register("witch_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.WITCH_UNIT,
                    0x330000, 0x3A732D, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> EVOKER_UNIT_SPAWN_EGG =
            ITEMS.register("evoker_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.EVOKER_UNIT,
                    0x8D9393, 0x141414, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> ENDERMAN_UNIT_SPAWN_EGG =
            ITEMS.register("enderman_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.ENDERMAN_UNIT,
                    0x1E1E1E, 0x000000, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> WARDEN_UNIT_SPAWN_EGG =
            ITEMS.register("warden_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.WARDEN_UNIT,
                    0x0e4145, 0x2da7b0, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> RAVAGER_UNIT_SPAWN_EGG =
            ITEMS.register("ravager_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.RAVAGER_UNIT,
                    0x6e6d69, 0x413934, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> SILVERFISH_UNIT_SPAWN_EGG =
            ITEMS.register("silverfish_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.SILVERFISH_UNIT,
                    0x666666, 0x222222, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> GRUNT_UNIT_SPAWN_EGG =
            ITEMS.register("grunt_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.GRUNT_UNIT,
                    0x925A3D, 0xC9C685, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> BRUTE_UNIT_SPAWN_EGG =
            ITEMS.register("brute_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.BRUTE_UNIT,
                    0x57290f, 0xC9C685, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> HEADHUNTER_UNIT_SPAWN_EGG =
            ITEMS.register("headhunter_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.HEADHUNTER_UNIT,
                    0x57290f, 0xC9C685, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> MARAUDER_UNIT_SPAWN_EGG =
            ITEMS.register("marauder_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.MARAUDER_UNIT,
                    0x57290f, 0xC9C685, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> HOGLIN_UNIT_SPAWN_EGG =
            ITEMS.register("hoglin_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.HOGLIN_UNIT,
                    13004373, 6251620, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> BLAZE_UNIT_SPAWN_EGG =
            ITEMS.register("blaze_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.BLAZE_UNIT,
                    16167425, 16775294, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> WITHER_SKELETON_UNIT_SPAWN_EGG =
            ITEMS.register("wither_skeleton_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.WITHER_SKELETON_UNIT,
                    1315860, 4672845, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> GHAST_UNIT_SPAWN_EGG =
            ITEMS.register("ghast_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.GHAST_UNIT,
                    16382457, 12369084, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> MAGMA_CUBE_UNIT_SPAWN_EGG =
            ITEMS.register("magma_cube_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.MAGMA_CUBE_UNIT,
                    3080192, 11776768, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> SLIME_UNIT_SPAWN_EGG =
            ITEMS.register("slime_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.SLIME_UNIT,
                    5405768, 5018938, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> ROYAL_GUARD_UNIT_SPAWN_EGG =
            ITEMS.register("royal_guard_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.ROYAL_GUARD_UNIT,
                    0x959b9b, 0x014675, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> NECROMANCER_UNIT_SPAWN_EGG =
            ITEMS.register("necromancer_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.NECROMANCER_UNIT,
                    0x3f243d, 0x0b9cbb, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> PIGLIN_MERCHANT_UNIT_SPAWN_EGG =
            ITEMS.register("piglin_merchant_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.PIGLIN_MERCHANT_UNIT,
                    0x3d1f12, 0x91da2a, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> POLAR_BEAR_UNIT_SPAWN_EGG =
            ITEMS.register("polar_bear_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.POLAR_BEAR_UNIT,
                    0xe3e3e3, 0x6f6f6f, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> GRIZZLY_BEAR_UNIT_SPAWN_EGG =
            ITEMS.register("grizzly_bear_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.GRIZZLY_BEAR_UNIT,
                    0x665442, 0x543423, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> PANDA_UNIT_SPAWN_EGG =
            ITEMS.register("panda_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.PANDA_UNIT,
                    0xd9d9d9, 0x121218, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> WOLF_UNIT_SPAWN_EGG =
            ITEMS.register("wolf_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.WOLF_UNIT,
                    0xc3bfbf, 0x947e6c, new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> LLAMA_UNIT_SPAWN_EGG =
            ITEMS.register("llama_unit_spawn_egg", () -> new ForgeSpawnEggItem(EntityRegistrar.LLAMA_UNIT,
                    0xa6896c, 0x6e442e, new Item.Properties()));

    public static final RegistryObject<Item> THROWABLE_TNT =
            ITEMS.register("throwable_tnt", () -> new ThrowableTnt(new Item.Properties()));

    public static final RegistryObject<Item> THROWN_HERO_EXPERIENCE_BOTTLE =
            ITEMS.register("thrown_hero_experience_bottle", () -> new HeroExperienceBottleItem(new Item.Properties()));

    public static void init(FMLJavaModLoadingContext context) {
        ITEMS.register(context.getModEventBus());
    }
}
