package com.solegendary.reignofnether.restrictions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RestrictionsSaveData extends SavedData {

    public final Map<String, Set<ResourceLocation>> denied = new HashMap<>();

    private static RestrictionsSaveData create() {
        return new RestrictionsSaveData();
    }

    @Nonnull
    public static RestrictionsSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(RestrictionsSaveData::load, RestrictionsSaveData::create, "saved-restrictions-data");
    }

    // { playerName: [id, ...], "*": [...] }
    public static RestrictionsSaveData load(CompoundTag tag) {
        RestrictionsSaveData data = create();
        for (String playerName : tag.getAllKeys()) {
            Set<ResourceLocation> ids = new HashSet<>();
            for (Tag id : tag.getList(playerName, Tag.TAG_STRING)) {
                ResourceLocation rl = ResourceLocation.tryParse(id.getAsString());
                if (rl != null)
                    ids.add(rl);
            }
            data.denied.put(playerName, ids);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        denied.forEach((playerName, ids) -> {
            ListTag list = new ListTag();
            ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
            tag.put(playerName, list);
        });
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
