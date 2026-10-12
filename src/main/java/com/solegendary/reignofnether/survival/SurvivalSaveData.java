package com.solegendary.reignofnether.survival;

import net.minecraft.core.HolderLookup;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import com.solegendary.reignofnether.util.SavedDataCompat;

public class SurvivalSaveData extends SavedData {

    public boolean isEnabled;
    public int waveNumber;
    public WaveDifficulty difficulty;
    public long randomSeed;

    private static SurvivalSaveData create() {
        return new SurvivalSaveData();
    }

    @Nonnull
    public static SurvivalSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return SavedDataCompat.computeIfAbsent(server, "saved-survival-data", SurvivalSaveData::create, SurvivalSaveData::load);
    }

    public static SurvivalSaveData load(CompoundTag tag) {
        SurvivalSaveData data = create();
        data.isEnabled = tag.getBoolean("isEnabled");
        data.waveNumber = tag.getInt("waveNumber");
        data.difficulty = WaveDifficulty.valueOf(tag.getString("difficulty"));
        data.randomSeed = tag.getLong("randomSeed");
        ReignOfNether.LOGGER.info("SurvivalSaveData.load: wave number: " + data.waveNumber);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        //ReignOfNether.LOGGER.info("SurvivalSaveData.save: " + waveNumber);
        tag.putBoolean("isEnabled", this.isEnabled);
        tag.putInt("waveNumber", this.waveNumber);
        tag.putString("difficulty", this.difficulty.name());
        tag.putLong("randomSeed", this.randomSeed);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
