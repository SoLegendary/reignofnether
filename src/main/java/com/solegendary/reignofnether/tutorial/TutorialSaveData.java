package com.solegendary.reignofnether.tutorial;

import net.minecraft.core.HolderLookup;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import com.solegendary.reignofnether.util.SavedDataCompat;

public class TutorialSaveData extends SavedData {

    public TutorialStage stage;

    private static TutorialSaveData create() {
        return new TutorialSaveData();
    }

    @Nonnull
    public static TutorialSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return SavedDataCompat.computeIfAbsent(server, "saved-tutorial-data", TutorialSaveData::create, TutorialSaveData::load);
    }

    public static TutorialSaveData load(CompoundTag tag) {
        TutorialSaveData data = create();
        data.stage = TutorialStage.valueOf(tag.getString("stage"));
        ReignOfNether.LOGGER.info("TutorialSaveData.load: " + data.stage);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        //ReignOfNether.LOGGER.info("TutorialSaveData.save: " + stage);
        tag.putString("stage", this.stage.name());
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
