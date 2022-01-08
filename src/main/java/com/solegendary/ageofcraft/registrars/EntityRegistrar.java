package com.solegendary.ageofcraft.registrars;

import com.solegendary.ageofcraft.AgeOfCraft;
import com.solegendary.ageofcraft.items.ItemBase;
import com.solegendary.ageofcraft.units.VillagerUnit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;


public class EntityRegistrar {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, AgeOfCraft.MOD_ID);

    // Entity Types
    public static final RegistryObject<EntityType<VillagerUnit>> VILLAGER_UNIT = ENTITIES.register("villager_unit",
            // can add other attributes here like sized() for hitbox, no summon, fire immunity, etc.
            () -> EntityType.Builder.of(VillagerUnit::new, MobCategory.CREATURE)
                    .sized(0.8f,0.6f)
                    .build(new ResourceLocation(AgeOfCraft.MOD_ID, "villager_unit").toString()));

    public static void init() {
        ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}