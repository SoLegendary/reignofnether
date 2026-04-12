package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.data.DataStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

// contains all necessary data for a restarted server to recreate all buildings in the level
public class BuildingSave {

    public BlockPos originPos;
    public Level level;
    public Building building;
    public String ownerName;
    public Rotation rotation;
    public BlockPos rallyPoint;
    public boolean isBuilt;
    public boolean isDiagonalBridge;
    public int upgradeLevel; // castle flag, lab rod, etc.
    PortalPlacement.PortalType portalType;
    public BlockPos portalDestination;
    public int scenarioRoleIndex;
    public DataStorage dataStorage;

    public BuildingSave(BlockPos originPos, Level level, Building building, String ownerName, Rotation rotation,
                        BlockPos rallyPoint, boolean isDiagonalBridge, boolean isBuilt, int upgradeLevel,
                        PortalPlacement.PortalType portalType, BlockPos portalDestination, int scenarioRoleIndex, DataStorage dataStorage) {
        this.originPos = originPos;
        this.level = level;
        this.building = building;
        this.ownerName = ownerName;
        this.rotation = rotation;
        this.rallyPoint = rallyPoint;
        this.isDiagonalBridge = isDiagonalBridge;
        this.isBuilt = isBuilt;
        this.upgradeLevel = upgradeLevel;
        this.portalType = portalType;
        this.portalDestination = portalDestination;
        this.scenarioRoleIndex = scenarioRoleIndex;
        this.dataStorage = dataStorage;
    }
}
