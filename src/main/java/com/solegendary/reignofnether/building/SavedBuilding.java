package com.solegendary.reignofnether.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

// contains all necessary data for a restarted server to recreate all buildings in the level
public class SavedBuilding {

    public BlockPos originPos;
    public Level level;
    public String name;
    public String ownerName;
    public Rotation rotation;
    public boolean isDiagonalBridge;

    public SavedBuilding(BlockPos originPos, Level level, String name, String ownerName, Rotation rotation, boolean isDiagonalBridge) {
        this.originPos = originPos;
        this.level = level;
        this.name = name;
        this.ownerName = ownerName;
        this.rotation = rotation;
        this.isDiagonalBridge = isDiagonalBridge;
    }
}
