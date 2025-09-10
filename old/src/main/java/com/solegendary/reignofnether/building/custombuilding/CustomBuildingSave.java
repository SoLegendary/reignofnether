package com.solegendary.reignofnether.building.custombuilding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

public class CustomBuildingSave {

    public BlockPos originPos;
    public Level level;
    public String ownerName;
    public String structureName;
    public String buildingName;
    public BlockPos structurePos;
    public Vec3i structureSize;

    public CustomBuildingSave(BlockPos originPos, Level level, String ownerName, String structureName,
                              String buildingName, BlockPos structurePos, Vec3i structureSize) {
        this.originPos = originPos;
        this.level = level;
        this.ownerName = ownerName;
        this.structureName = structureName;
        this.buildingName = buildingName;
        this.structurePos = structurePos;
        this.structureSize = structureSize;
    }
}
