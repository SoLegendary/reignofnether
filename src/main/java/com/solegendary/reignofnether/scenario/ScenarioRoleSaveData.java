package com.solegendary.reignofnether.scenario;

import net.minecraft.core.HolderLookup;
import com.solegendary.reignofnether.ReignOfNether;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import com.solegendary.reignofnether.util.SavedDataCompat;

public class ScenarioRoleSaveData extends SavedData {

    public final ArrayList<ScenarioRole> scenarioRoleSaves = new ArrayList<>();

    private static ScenarioRoleSaveData create() {
        return new ScenarioRoleSaveData();
    }

    @Nonnull
    public static ScenarioRoleSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return SavedDataCompat.computeIfAbsent(server, "saved-scenario-data", ScenarioRoleSaveData::create, ScenarioRoleSaveData::load);
    }

    public static ScenarioRoleSaveData load(CompoundTag tag) {
        ReignOfNether.LOGGER.info("ScenarioSaveData.load");

        ScenarioRoleSaveData data = create();
        ListTag ltag = (ListTag) tag.get("scenarioRoles");

        if (ltag != null) {
            for (Tag rtag : ltag) {
                CompoundTag ctag = (CompoundTag) rtag;
                int index = ctag.getInt("index");
                data.scenarioRoleSaves.add(ScenarioRole.getFromSave(index, ctag));
                ReignOfNether.LOGGER.info("ScenarioSaveData.load: index " + index);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        this.scenarioRoleSaves.forEach(r -> {
            r.packNbt();
            list.add(r.nbt);
        });
        tag.put("scenarioRoles", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
