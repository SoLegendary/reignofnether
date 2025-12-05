package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.Resources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class CustomBuildingSaveData extends SavedData {

    public final ArrayList<CustomBuildingSave> customBuildings = new ArrayList<>();

    private static CustomBuildingSaveData create() {
        return new CustomBuildingSaveData();
    }

    @Nonnull
    public static CustomBuildingSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return server.overworld()
            .getDataStorage()
            .computeIfAbsent(CustomBuildingSaveData::load, CustomBuildingSaveData::create, "saved-custom-building-data");
    }

    public static CustomBuildingSaveData load(CompoundTag tag) {
        ReignOfNether.LOGGER.info("CustomBuildingSaveData.load");

        CustomBuildingSaveData data = create();
        ListTag ltag = (ListTag) tag.get("custombuildings");

        if (ltag != null) {
            for (Tag ctag : ltag) {
                CompoundTag btag = (CompoundTag) ctag;
                String buildingName = btag.getString("buildingName");
                Vec3i structureSize = new Vec3i(btag.getInt("structureSizeX"), btag.getInt("structureSizeY"), btag.getInt("structureSizeZ"));
                CompoundTag structureNbt = btag.getCompound("structureNbt");
                CompoundTag attributesNbt = btag.getCompound("attributesNbt");

                data.customBuildings.add(new CustomBuildingSave(
                        structureNbt,
                        buildingName,
                        structureSize,
                        attributesNbt
                ));
                ReignOfNether.LOGGER.info("CustomBuildingSaveData.load: " + buildingName);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        this.customBuildings.forEach(b -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putString("buildingName", b.buildingName);
            cTag.putInt("structureSizeX", b.structureSize.getX());
            cTag.putInt("structureSizeY", b.structureSize.getY());
            cTag.putInt("structureSizeZ", b.structureSize.getZ());
            cTag.put("structureNbt", b.structureNbt);
            cTag.put("attributesNbt", b.attributesNbt);
            list.add(cTag);
        });
        tag.put("custombuildings", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
