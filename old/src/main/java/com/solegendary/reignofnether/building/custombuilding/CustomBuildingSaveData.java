package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.ReignOfNether;
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

                BlockPos pos = new BlockPos(btag.getInt("originPosX"), btag.getInt("originPosY"), btag.getInt("originPosZ"));
                Level level = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
                String ownerName = btag.getString("ownerName");
                String structureName = btag.getString("structureName");
                String buildingName = btag.getString("buildingName");
                BlockPos structurePos = new BlockPos(btag.getInt("structurePosX"), btag.getInt("structurePosY"), btag.getInt("structurePosZ"));
                Vec3i structureSize = new Vec3i(btag.getInt("structureSizeX"), btag.getInt("structureSizeY"), btag.getInt("structureSizeZ"));

                data.customBuildings.add(new CustomBuildingSave(
                    pos,
                    level,
                    ownerName,
                    structureName,
                    buildingName,
                    structurePos,
                    structureSize
                ));
                ReignOfNether.LOGGER.info("CustomBuildingSaveData.load: " + ownerName + "|" + structureName);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        this.customBuildings.forEach(b -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putInt("originPosX", b.originPos.getX());
            cTag.putInt("originPosY", b.originPos.getY());
            cTag.putInt("originPosZ", b.originPos.getZ());
            cTag.putString("ownerName", b.ownerName);
            cTag.putString("structureName", b.structureName);
            cTag.putString("buildingName", b.buildingName);
            cTag.putInt("structurePosX", b.structurePos.getX());
            cTag.putInt("structurePosY", b.structurePos.getY());
            cTag.putInt("structurePosZ", b.structurePos.getZ());
            cTag.putInt("structureSizeX", b.structureSize.getX());
            cTag.putInt("structureSizeY", b.structureSize.getY());
            cTag.putInt("structureSizeZ", b.structureSize.getZ());
            list.add(cTag);
        });
        tag.put("custombuildings", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
