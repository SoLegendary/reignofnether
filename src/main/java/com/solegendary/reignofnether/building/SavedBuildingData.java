package com.solegendary.reignofnether.building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class SavedBuildingData extends SavedData {

    private final ArrayList<SavedBuilding> buildings = new ArrayList<>();

    public ArrayList<SavedBuilding> getBuildings() {
        return this.buildings;
    }

    private static SavedBuildingData create() {
        return new SavedBuildingData();
    }

    @Nonnull
    public static SavedBuildingData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return server.overworld().getDataStorage().computeIfAbsent(SavedBuildingData::load, SavedBuildingData::create, "saved-building-data");
    }

    public static SavedBuildingData load(CompoundTag tag) {
        System.out.println("SavedBuildingData.load");

        SavedBuildingData data = create();
        ListTag ltag = (ListTag) tag.get("buildings");

        for (Tag ctag : ltag) {
            CompoundTag btag = (CompoundTag) ctag;
            BlockPos pos = new BlockPos(btag.getInt("x"), btag.getInt("y"), btag.getInt("z"));
            Level level = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
            String name = btag.getString("buildingName");
            String ownerName = btag.getString("ownerName");
            Rotation rotation = Rotation.valueOf(btag.getString("rotation"));
            boolean isDiagonalBridge = btag.getBoolean("isDiagonalBridge");
            data.getBuildings().add(new SavedBuilding(pos, level, name, ownerName, rotation, isDiagonalBridge));

            System.out.println("SavedBuildingData.load: " + pos);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        System.out.println("SavedBuildingData.save");

        ListTag list = new ListTag();
        this.getBuildings().forEach(b -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putString("buildingName", b.name);
            cTag.putInt("x", b.originPos.getX());
            cTag.putInt("y", b.originPos.getY());
            cTag.putInt("z", b.originPos.getZ());
            cTag.putString("rotation", b.rotation.name());
            cTag.putString("ownerName", b.ownerName);
            cTag.putBoolean("isDiagonalBridge", b.isDiagonalBridge);
            list.add(cTag);

            System.out.println("SavedBuildingData.save: " + b.originPos);
        });
        tag.put("buildings", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
