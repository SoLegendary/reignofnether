package com.solegendary.reignofnether.building.custombuilding;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

// registry saves only - placements are saved as a regular BuildingSaves
public class CustomBuildingSave {

    public CompoundTag structureNbt;
    public String buildingName;
    public Vec3i structureSize;
    public CompoundTag attributesNbt;

    public CustomBuildingSave(CompoundTag structureNbt, String buildingName, Vec3i structureSize, CompoundTag attributesNbt) {
        this.structureNbt = structureNbt;
        this.buildingName = buildingName;
        this.structureSize = structureSize;
        this.attributesNbt = attributesNbt;
    }
}
