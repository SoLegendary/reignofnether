package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit;
import com.solegendary.reignofnether.unit.units.monsters.ZombieUnit;
import com.solegendary.reignofnether.unit.units.monsters.ZombieVillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VindicatorUnit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;


public class EntityRegistrar {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ReignOfNether.MOD_ID);

    // Entity Types

    public static final RegistryObject<EntityType<ZombieVillagerUnit>> ZOMBIE_VILLAGER_UNIT = ENTITIES.register("zombie_villager_unit",
            () -> EntityType.Builder.of(ZombieVillagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOMBIE_VILLAGER.getWidth(), EntityType.ZOMBIE_VILLAGER.getHeight())
                    .build(new ResourceLocation(ReignOfNether.MOD_ID, "zombie_villager_unit").toString()));

    public static final RegistryObject<EntityType<ZombieUnit>> ZOMBIE_UNIT = ENTITIES.register("zombie_unit",
            // can add other attributes here like sized() for hitbox, no summon, fire immunity, etc.
            () -> EntityType.Builder.of(ZombieUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.ZOMBIE.getWidth(), EntityType.ZOMBIE.getHeight())
                    .build(new ResourceLocation(ReignOfNether.MOD_ID, "zombie_unit").toString()));

    public static final RegistryObject<EntityType<SkeletonUnit>> SKELETON_UNIT = ENTITIES.register("skeleton_unit",
            () -> EntityType.Builder.of(SkeletonUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.SKELETON.getWidth(), EntityType.SKELETON.getHeight())
                    .build(new ResourceLocation(ReignOfNether.MOD_ID, "skeleton_unit").toString()));

    public static final RegistryObject<EntityType<CreeperUnit>> CREEPER_UNIT = ENTITIES.register("creeper_unit",
            () -> EntityType.Builder.of(CreeperUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.CREEPER.getWidth(), EntityType.CREEPER.getHeight())
                    .build(new ResourceLocation(ReignOfNether.MOD_ID, "creeper_unit").toString()));

    public static final RegistryObject<EntityType<VillagerUnit>> VILLAGER_UNIT = ENTITIES.register("villager_unit",
            () -> EntityType.Builder.of(VillagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VILLAGER.getWidth(), EntityType.VILLAGER.getHeight())
                    .build(new ResourceLocation(ReignOfNether.MOD_ID, "villager_unit").toString()));

    public static final RegistryObject<EntityType<VindicatorUnit>> VINDICATOR_UNIT = ENTITIES.register("vindicator_unit",
            () -> EntityType.Builder.of(VindicatorUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VINDICATOR.getWidth(), EntityType.VINDICATOR.getHeight())
                    .build(new ResourceLocation(ReignOfNether.MOD_ID, "vindicator_unit").toString()));

    public static final RegistryObject<EntityType<PillagerUnit>> PILLAGER_UNIT = ENTITIES.register("pillager_unit",
            () -> EntityType.Builder.of(PillagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.PILLAGER.getWidth(), EntityType.PILLAGER.getHeight())
                    .build(new ResourceLocation(ReignOfNether.MOD_ID, "pillager_unit").toString()));

    public static void init() {
        ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}