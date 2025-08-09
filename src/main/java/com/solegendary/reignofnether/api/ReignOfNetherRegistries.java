package com.solegendary.reignofnether.api;

import com.mojang.serialization.Lifecycle;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.production.ProductionItem;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class ReignOfNetherRegistries {
    public static final MappedRegistry<Building> BUILDING = createMappedRegistry(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "building"));
    public static final MappedRegistry<ProductionItem> PRODUCTION_ITEM = createMappedRegistry(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "production_item"));

    private static <T> MappedRegistry<T> createMappedRegistry(ResourceLocation id) {
        ResourceKey<Registry<T>> key = ResourceKey.createRegistryKey(id);
        return new MappedRegistry<T>(key, Lifecycle.experimental(), false);
    }
}
