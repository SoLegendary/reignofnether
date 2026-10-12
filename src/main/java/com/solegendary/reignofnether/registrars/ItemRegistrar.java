package com.solegendary.reignofnether.registrars;

import java.util.function.Supplier;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.items.FoilableItem;
import com.solegendary.reignofnether.items.HeroExperienceBottleItem;
import com.solegendary.reignofnether.items.ThrowableTnt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.property.Properties;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;

public class ItemRegistrar {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, ReignOfNether.MOD_ID);

    public static final Supplier<DeferredSpawnEggItem> ZOMBIE_UNIT_SPAWN_EGG =
            ITEMS.register("zombie_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.ZOMBIE_UNIT,
                    0x009999, 0x577048, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> HUSK_UNIT_SPAWN_EGG =
            ITEMS.register("husk_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.HUSK_UNIT,
                    0x71695B, 0xB7A276, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> DROWNED_UNIT_SPAWN_EGG =
            ITEMS.register("drowned_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.DROWNED_UNIT,
                    9433559, 7969893, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> ZOMBIE_PIGLIN_UNIT_SPAWN_EGG =
            ITEMS.register("zombie_piglin_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.ZOMBIE_PIGLIN_UNIT,
                    15373203, 5009705, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> ZOGLIN_UNIT =
            ITEMS.register("zoglin_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.ZOGLIN_UNIT,
                    13004373, 15132390, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> SKELETON_UNIT_SPAWN_EGG =
            ITEMS.register("skeleton_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.SKELETON_UNIT,
                    0xa7a7a7, 0x3a3a3a, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> STRAY_UNIT_SPAWN_EGG =
            ITEMS.register("stray_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.STRAY_UNIT,
                    0x5B6F6F, 0xAEB8B8, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> BOGGED_UNIT_SPAWN_EGG =
            ITEMS.register("bogged_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.BOGGED_UNIT,
                    0xa1a387, 0x3e4d12, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> CREEPER_UNIT_SPAWN_EGG =
            ITEMS.register("creeper_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.CREEPER_UNIT,
                    0x0c990a, 0x000000, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> SPIDER_UNIT_SPAWN_EGG =
            ITEMS.register("spider_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.SPIDER_UNIT,
                    0x322B26, 0x840B0B, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> POISON_SPIDER_UNIT_SPAWN_EGG =
            ITEMS.register("poison_spider_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.POISON_SPIDER_UNIT,
                    0x0B3F4A, 0x840B0B, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> WRAITH_UNIT_SPAWN_EGG =
            ITEMS.register("wraith_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.WRAITH_UNIT,
                    0xc3cdc9, 0x1a1862, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> VILLAGER_UNIT_SPAWN_EGG =
            ITEMS.register("villager_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.VILLAGER_UNIT,
                    0x523632, 0x946F66, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> MILITIA_UNIT_SPAWN_EGG =
            ITEMS.register("militia_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.MILITIA_UNIT,
                    0x523632, 0x946F66, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> ZOMBIE_VILLAGER_UNIT_SPAWN_EGG =
            ITEMS.register("zombie_villager_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.ZOMBIE_VILLAGER_UNIT,
                    0x523632, 0x647E51, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> VINDICATOR_UNIT_SPAWN_EGG =
            ITEMS.register("vindicator_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.VINDICATOR_UNIT,
                    0x8B8F90, 0x1F4952, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> PILLAGER_UNIT_SPAWN_EGG =
            ITEMS.register("pillager_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.PILLAGER_UNIT,
                    0x502C34, 0x757D78, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> WINDCALLER_UNIT_SPAWN_EGG =
            ITEMS.register("windcaller_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.WINDCALLER_UNIT,
                    0x502C34, 0x757D78, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> IRON_GOLEM_UNIT_SPAWN_EGG =
            ITEMS.register("iron_golem_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.IRON_GOLEM_UNIT,
                    0x101010, 0x757D78, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> WITCH_UNIT_SPAWN_EGG =
            ITEMS.register("witch_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.WITCH_UNIT,
                    0x330000, 0x3A732D, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> EVOKER_UNIT_SPAWN_EGG =
            ITEMS.register("evoker_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.EVOKER_UNIT,
                    0x8D9393, 0x141414, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> ENDERMAN_UNIT_SPAWN_EGG =
            ITEMS.register("enderman_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.ENDERMAN_UNIT,
                    0x1E1E1E, 0x000000, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> WARDEN_UNIT_SPAWN_EGG =
            ITEMS.register("warden_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.WARDEN_UNIT,
                    0x0e4145, 0x2da7b0, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> RAVAGER_UNIT_SPAWN_EGG =
            ITEMS.register("ravager_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.RAVAGER_UNIT,
                    0x6e6d69, 0x413934, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> SILVERFISH_UNIT_SPAWN_EGG =
            ITEMS.register("silverfish_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.SILVERFISH_UNIT,
                    0x666666, 0x222222, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> GRUNT_UNIT_SPAWN_EGG =
            ITEMS.register("grunt_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.GRUNT_UNIT,
                    0x925A3D, 0xC9C685, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> BRUTE_UNIT_SPAWN_EGG =
            ITEMS.register("brute_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.BRUTE_UNIT,
                    0x57290f, 0xC9C685, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> HEADHUNTER_UNIT_SPAWN_EGG =
            ITEMS.register("headhunter_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.HEADHUNTER_UNIT,
                    0x57290f, 0xC9C685, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> MARAUDER_UNIT_SPAWN_EGG =
            ITEMS.register("marauder_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.MARAUDER_UNIT,
                    0x57290f, 0xC9C685, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> HOGLIN_UNIT_SPAWN_EGG =
            ITEMS.register("hoglin_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.HOGLIN_UNIT,
                    13004373, 6251620, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> BLAZE_UNIT_SPAWN_EGG =
            ITEMS.register("blaze_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.BLAZE_UNIT,
                    16167425, 16775294, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> WITHER_SKELETON_UNIT_SPAWN_EGG =
            ITEMS.register("wither_skeleton_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.WITHER_SKELETON_UNIT,
                    1315860, 4672845, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> GHAST_UNIT_SPAWN_EGG =
            ITEMS.register("ghast_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.GHAST_UNIT,
                    16382457, 12369084, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> MAGMA_CUBE_UNIT_SPAWN_EGG =
            ITEMS.register("magma_cube_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.MAGMA_CUBE_UNIT,
                    3080192, 11776768, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> SLIME_UNIT_SPAWN_EGG =
            ITEMS.register("slime_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.SLIME_UNIT,
                    5405768, 5018938, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> ROYAL_GUARD_UNIT_SPAWN_EGG =
            ITEMS.register("royal_guard_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.ROYAL_GUARD_UNIT,
                    0x959b9b, 0x014675, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> NECROMANCER_UNIT_SPAWN_EGG =
            ITEMS.register("necromancer_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.NECROMANCER_UNIT,
                    0x3f243d, 0x0b9cbb, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> PIGLIN_MERCHANT_UNIT_SPAWN_EGG =
            ITEMS.register("piglin_merchant_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.PIGLIN_MERCHANT_UNIT,
                    0x3d1f12, 0x91da2a, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> POLAR_BEAR_UNIT_SPAWN_EGG =
            ITEMS.register("polar_bear_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.POLAR_BEAR_UNIT,
                    0xe3e3e3, 0x6f6f6f, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> GRIZZLY_BEAR_UNIT_SPAWN_EGG =
            ITEMS.register("grizzly_bear_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.GRIZZLY_BEAR_UNIT,
                    0x665442, 0x543423, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> PANDA_UNIT_SPAWN_EGG =
            ITEMS.register("panda_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.PANDA_UNIT,
                    0xd9d9d9, 0x121218, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> WOLF_UNIT_SPAWN_EGG =
            ITEMS.register("wolf_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.WOLF_UNIT,
                    0xc3bfbf, 0x947e6c, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> SCOUT_DOG_UNIT_SPAWN_EGG =
            ITEMS.register("scout_dog_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.SCOUT_DOG_UNIT,
                    0x665232, 0xccbda5, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> SCOUT_CAT_UNIT_SPAWN_EGG =
            ITEMS.register("scout_cat_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.SCOUT_CAT_UNIT,
                    0xcead7b, 0x755a44, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> STRIDER_UNIT_SPAWN_EGG =
            ITEMS.register("strider_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.STRIDER_UNIT,
                    0x8b2f31, 0x353535, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> BAT_UNIT_SPAWN_EGG =
            ITEMS.register("bat_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.BAT_UNIT,
                    0x473a2d, 0x0c0c0c, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> LLAMA_UNIT_SPAWN_EGG =
            ITEMS.register("llama_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.LLAMA_UNIT,
                    0xa6896c, 0x6e442e, new Item.Properties()));

    public static final Supplier<DeferredSpawnEggItem> BEE_UNIT_SPAWN_EGG =
            ITEMS.register("bee_unit_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegistrar.BEE_UNIT,
                    0xdbb544, 0x2a1711, new Item.Properties()));

    public static final Supplier<Item> THROWABLE_TNT =
            ITEMS.register("throwable_tnt", () -> new ThrowableTnt(new Item.Properties()));

    public static final Supplier<Item> THROWN_HERO_EXPERIENCE_BOTTLE =
            ITEMS.register("thrown_hero_experience_bottle", () -> new HeroExperienceBottleItem(new Item.Properties()));

    public static final Supplier<Item> STAFF_OF_LIGHTNING =
            ITEMS.register("staff_of_lightning", () -> new FoilableItem(new Item.Properties().rarity(Rarity.EPIC)));


    public static final Supplier<Item> HEART_MEDALLION = ITEMS.register("heart_medallion", () -> new FoilableItem(new Item.Properties()));
    public static final Supplier<Item> AZURE_MEDALLION = ITEMS.register("azure_medallion", () -> new FoilableItem(new Item.Properties()));
    public static final Supplier<Item> IRON_HIDE_AMULET = ITEMS.register("iron_hide_amulet", () -> new FoilableItem(new Item.Properties()));
    public static final Supplier<Item> SOUL_COLLECTOR = ITEMS.register("soul_collector", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> BROADSWORD = ITEMS.register("broadsword", () -> new FoilableItem(new Item.Properties()));
    public static final Supplier<Item> KATANA = ITEMS.register("katana", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> HEARTSTEALER = ITEMS.register("heartstealer", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> RITUAL_DAGGER = ITEMS.register("ritual_dagger", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> POWERSHAKER = ITEMS.register("powershaker", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> GREAT_HAMMER = ITEMS.register("great_hammer", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> SPARKLER = ITEMS.register("sparkler", () -> new FoilableItem(new Item.Properties()));
    public static final Supplier<Item> LIGHT_FEATHER = ITEMS.register("light_feather", () -> new FoilableItem(new Item.Properties()));
    public static final Supplier<Item> BOOTS_OF_SWIFTNESS = ITEMS.register("boots_of_swiftness", () -> new FoilableItem(new Item.Properties()));
    public static final Supplier<Item> FROST_WALKER_BOOTS = ITEMS.register("frost_walker_boots", () -> new FoilableItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final Supplier<Item> MAGMA_WALKER_BOOTS = ITEMS.register("magma_walker_boots", () -> new FoilableItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final Supplier<Item> SATCHEL_OF_SNACKS = ITEMS.register("satchel_of_snacks", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> BEENEST_ARMOUR = ITEMS.register("beenest_armour", () -> new FoilableItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final Supplier<Item> HEALTH_POTION = ITEMS.register("health_potion", () -> new FoilableItem(new Item.Properties()));
    public static final Supplier<Item> MANA_POTION = ITEMS.register("mana_potion", () -> new FoilableItem(new Item.Properties()));
    public static final Supplier<Item> GHOST_CLOAK = ITEMS.register("ghost_cloak", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> GONG_OF_WEAKENING = ITEMS.register("gong_of_weakening", () -> new FoilableItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final Supplier<Item> ICE_WAND = ITEMS.register("ice_wand", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> UPDRAFT_TOME = ITEMS.register("updraft_tome", () -> new FoilableItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final Supplier<Item> TOME_OF_DUPLICATION = ITEMS.register("tome_of_duplication", () -> new FoilableItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final Supplier<Item> SHADOW_SHIFTER = ITEMS.register("shadow_shifter", () -> new FoilableItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final Supplier<Item> POCKET_PORTAL = ITEMS.register("pocket_portal", () -> new FoilableItem(new Item.Properties()));
    public static final Supplier<Item> WAR_HORN = ITEMS.register("war_horn", () -> new FoilableItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final Supplier<Item> TOTEM_OF_REGENERATION = ITEMS.register("totem_of_regeneration", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> TOTEM_OF_SHIELDING = ITEMS.register("totem_of_shielding", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> TOTEM_OF_PROTECTION = ITEMS.register("totem_of_protection", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> TOTEM_OF_CASTING = ITEMS.register("totem_of_casting", () -> new FoilableItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // 1.5.0 hero armour set
    public static final Supplier<Item> HEROS_ARMOUR = ITEMS.register("heros_armour", () -> new FoilableItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final Supplier<Item> HUNGRY_HORROR_ARMOUR = ITEMS.register("hungry_horror_armour", () -> new FoilableItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final Supplier<Item> GOLDEN_PIGLIN_ARMOUR = ITEMS.register("golden_piglin_armour", () -> new FoilableItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final Supplier<Item> EVOCATION_ROBES = ITEMS.register("evocation_robes", () -> new FoilableItem(new Item.Properties().rarity(Rarity.RARE)));

    public static void init(ModContainer context) {
        ITEMS.register(context.getEventBus());
    }
}
