package com.solegendary.reignofnether.building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;

// contains all necessary data for a restarted server to recreate all buildings in the level
public class SavedBuilding {

    public BlockPos pos;
    public Level level;
    public String name;
    public String ownerName;
    public Rotation rotation;
    public boolean isDiagonalBridge;

    public SavedBuilding(BlockPos pos, Level level, String name, String ownerName, Rotation rotation, boolean isDiagonalBridge) {
        this.pos = pos;
        this.level = level;
        this.name = name;
        this.ownerName = ownerName;
        this.rotation = rotation;
        this.isDiagonalBridge = isDiagonalBridge;
    }
}
