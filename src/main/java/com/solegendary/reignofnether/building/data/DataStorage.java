package com.solegendary.reignofnether.building.data;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DataStorage {
    Object2ObjectMap<DataType<?>, Object> data = new Object2ObjectArrayMap<>();
    public static DataStorage read(ListTag dataStorageTag, MinecraftServer server) {
        DataStorage dataStorage = new DataStorage();

        for (Tag tag : dataStorageTag) {
            CompoundTag element = (CompoundTag) tag;
            ResourceLocation rl = ResourceLocation.parse(element.getString("key"));
            DataType<?> type = ReignOfNetherRegistries.DATA_TYPE.get(rl);
            if (type != null) {
                Object data = type.decode.apply(element.getCompound("data"), server);
                dataStorage.data.put(type, data);
            }else {
                ReignOfNether.LOGGER.error("Unknown DataType " + rl);
            }
        }

        return dataStorage;
    }

    public ListTag write() {
        ListTag dataStorageTag = new ListTag();

        for (Map.Entry<DataType<?>, Object> dataTypeObjectEntry : data.entrySet()) {
            DataType<Object> type = (DataType<Object>) dataTypeObjectEntry.getKey();
            ResourceLocation rl = ReignOfNetherRegistries.DATA_TYPE.getKey(type);
            CompoundTag element = new CompoundTag();
            element.putString("key", rl.toString());
            element.put("data", type.encode.apply(dataTypeObjectEntry.getValue()));
            dataStorageTag.add(element);
        }

        return dataStorageTag;
    }

    @Nullable
    public <T> T getData(DataType<T> type) {
        ensureRegistered(type);

        return (T) data.computeIfAbsent(type, (unused) -> type.getDefaultValue());
    }

    public <T> void setData(DataType<T> type, T data) {
        ensureRegistered(type);

        this.data.put(type, data);
    }

    private static void ensureRegistered(DataType<?> type) {
        if (ReignOfNetherRegistries.DATA_TYPE.getId(type) == -1) {
            throw new IllegalArgumentException("Used Datatype is not registered");
        }
    }
}
