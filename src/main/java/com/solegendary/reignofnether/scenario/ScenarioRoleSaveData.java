package com.solegendary.reignofnether.scenario;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.resources.Resources;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;

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
        return server.overworld()
            .getDataStorage()
            .computeIfAbsent(ScenarioRoleSaveData::load, ScenarioRoleSaveData::create, "saved-scenario-data");
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
    public CompoundTag save(CompoundTag tag) {
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
