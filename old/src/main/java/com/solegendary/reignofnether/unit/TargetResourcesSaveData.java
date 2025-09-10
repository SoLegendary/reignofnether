package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.resources.ResourceName;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class TargetResourcesSaveData extends SavedData {

    public final ArrayList<TargetResourcesSave> targetData = new ArrayList<>();

    private static TargetResourcesSaveData create() {
        return new TargetResourcesSaveData();
    }

    @Nonnull
    public static TargetResourcesSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return server.overworld()
            .getDataStorage()
            .computeIfAbsent(TargetResourcesSaveData::load, TargetResourcesSaveData::create, "saved-target-resources-data");
    }

    public static TargetResourcesSaveData load(CompoundTag tag) {
        ReignOfNether.LOGGER.info("TargetResourcesSaveData.load");

        TargetResourcesSaveData data = create();
        ListTag ltag = (ListTag) tag.get("targetResourcesData");

        if (ltag != null) {
            for (Tag ctag : ltag) {
                CompoundTag tdTag = (CompoundTag) ctag;

                String unitUUID = tdTag.getString("uuid");
                BlockPos gatherTarget = null;
                if (tdTag.contains("gatherTargetX") &&
                    tdTag.contains("gatherTargetY") &&
                    tdTag.contains("gatherTargetZ")) {
                    gatherTarget = new BlockPos(
                        tdTag.getInt("gatherTargetX"),
                        tdTag.getInt("gatherTargetY"),
                        tdTag.getInt("gatherTargetZ")
                    );
                }
                ResourceName resourceName = ResourceName.valueOf(tdTag.getString("targetResourceName"));
                BuildingPlacement targetFarm = null;
                if (tdTag.contains("farmX") &&
                    tdTag.contains("farmY") &&
                    tdTag.contains("farmZ")) {
                    BlockPos farmPos = new BlockPos(
                        tdTag.getInt("farmX"),
                        tdTag.getInt("farmY"),
                        tdTag.getInt("farmZ")
                    );
                    targetFarm = BuildingUtils.findBuilding(false, farmPos);
                }

                TargetResourcesSave resData = new TargetResourcesSave();
                resData.unitUUID = unitUUID;
                resData.gatherTarget = gatherTarget;
                resData.targetResourceName = resourceName;
                //if (gatherTarget != null)
                //    resData.targetResourceSource = ResourceSources.getFromBlockPos(gatherTarget, level);
                resData.targetFarm = targetFarm;

                data.targetData.add(resData);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        //ReignOfNether.LOGGER.info("TargetResourcesSaveData.save");

        ListTag list = new ListTag();
        this.targetData.forEach(td -> {
            CompoundTag cTag = new CompoundTag();

            cTag.putString("uuid", td.unitUUID);
            if (td.gatherTarget != null) {
                cTag.putInt("gatherTargetX", td.gatherTarget.getX());
                cTag.putInt("gatherTargetY", td.gatherTarget.getY());
                cTag.putInt("gatherTargetZ", td.gatherTarget.getZ());
            }
            cTag.putString("targetResourceName", td.targetResourceName.toString());
            if (td.targetFarm != null) {
                cTag.putInt("farmX", td.targetFarm.originPos.getX());
                cTag.putInt("farmY", td.targetFarm.originPos.getY());
                cTag.putInt("farmZ", td.targetFarm.originPos.getZ());
            }
            list.add(cTag);

            //ReignOfNether.LOGGER.info("TargetResourcesSaveData.save: " + td.unitUUID + "|" + td.gatherTarget);
        });
        tag.put("targetResourcesData", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
