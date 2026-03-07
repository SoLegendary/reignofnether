package com.solegendary.reignofnether.building.data;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class DataType<T> {
    BiFunction<CompoundTag, MinecraftServer, T> decode;
    Function<T, CompoundTag> encode;
    Supplier<T> defaultValue;
    public DataType(BiFunction<CompoundTag, MinecraftServer, T> decode, Function<T, CompoundTag> encode) {
        this(decode, encode, null);
    }

    public DataType(BiFunction<CompoundTag, MinecraftServer, T> decode, Function<T, CompoundTag> encode, Supplier<T> defaultValue) {
        this.decode = decode;
        this.encode = encode;
        this.defaultValue = defaultValue;
    }

    public static <T> DataType<T> createRegistered(ResourceLocation rl, BiFunction<CompoundTag, MinecraftServer, T> decode, Function<T, CompoundTag> encode) {
        return createRegistered(rl, decode, encode, () -> null);
    }

    public static <T> DataType<T> createRegistered(ResourceLocation rl, BiFunction<CompoundTag, MinecraftServer, T> decode, Function<T, CompoundTag> encode, Supplier<T> defaultValue) {
        DataType<T> type = new DataType(decode, encode, defaultValue);
        Registry.register(ReignOfNetherRegistries.DATA_TYPE, rl, type);
        return type;
    }

    public T getDefaultValue() {
        return defaultValue.get();
    }
}
