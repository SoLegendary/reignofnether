package com.solegendary.reignofnether.building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
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
        SavedBuildingData data = create();

        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        Level level = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        String name = tag.getString("buildingName");
        String ownerName = tag.getString("ownerName");
        Rotation rotation = Rotation.valueOf(tag.getString("rotation"));
        boolean isDiagonalBridge = tag.getBoolean("isDiagonalBridge");

        data.getBuildings().add(new SavedBuilding(pos, level, name, ownerName, rotation, isDiagonalBridge));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        getBuildings().forEach(b -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putString("buildingName", b.name);
            cTag.putInt("x", b.pos.getX());
            cTag.putInt("y", b.pos.getY());
            cTag.putInt("z", b.pos.getZ());
            cTag.putString("rotation", b.name);
            cTag.putString("ownerName", b.ownerName);
            cTag.putBoolean("isDiagonalBridge", b.isDiagonalBridge);
        });
        tag.put("buildings", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
