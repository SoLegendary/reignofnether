package com.solegendary.reignofnether.util;

import com.solegendary.reignofnether.ReignOfNether;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 1.21.1 deleted {@code MinecraftServer#getLootData()} and turned loot tables into a dynamic
 * registry ({@link Registries#LOOT_TABLE}) loaded by the server's reloadable resources. The
 * lookup is therefore {@code getServerResources().managers().fullRegistries().getLootTable(key)},
 * wrapped here so call sites keep reading as "server, id, table".
 */
public final class LootUtils {
    private LootUtils() {}

    /** Replaces {@code server.getLootData().getLootTable(id)}. */
    public static LootTable getLootTable(MinecraftServer server, ResourceLocation id) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);
        return server.getServerResources().managers().fullRegistries().getLootTable(key);
    }

    /** Convenience overload for ids the mod itself builds. */
    public static LootTable getLootTable(MinecraftServer server, String path) {
        return getLootTable(server, ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, path));
    }
}
