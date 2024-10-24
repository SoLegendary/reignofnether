package com.solegendary.reignofnether.research;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class ResearchSaveData extends SavedData {

    public final ArrayList<Pair<String, String>> researchItems = new ArrayList<>();

    private static ResearchSaveData create() {
        return new ResearchSaveData();
    }

    @Nonnull
    public static ResearchSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return server.overworld()
            .getDataStorage()
            .computeIfAbsent(ResearchSaveData::load, ResearchSaveData::create, "saved-research-data");
    }

    public static ResearchSaveData load(CompoundTag tag) {
        ReignOfNether.LOGGER.info("ResearchSaveData.load");

        ResearchSaveData data = create();
        ListTag ltag = (ListTag) tag.get("researchItems");

        if (ltag != null) {
            for (Tag ctag : ltag) {
                CompoundTag btag = (CompoundTag) ctag;
                String ownerName = btag.getString("ownerName");
                String researchName = btag.getString("researchName");
                data.researchItems.add(new Pair<>(ownerName, researchName));
                ReignOfNether.LOGGER.info("ResearchSaveData.load: " + ownerName + "|" + researchName);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ReignOfNether.LOGGER.info("ResearchSaveData.save");

        ListTag list = new ListTag();
        this.researchItems.forEach(b -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putString("ownerName", b.getFirst());
            cTag.putString("researchName", b.getSecond());
            list.add(cTag);
            ReignOfNether.LOGGER.info("ResearchSaveData.save: " + b.getFirst() + "|" + b.getSecond());
        });
        tag.put("researchItems", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
