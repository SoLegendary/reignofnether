package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.buildings.piglins.Portal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

// contains all necessary data for a restarted server to recreate all buildings in the level
public class BuildingSave {

    public BlockPos originPos;
    public Level level;
    public String name;
    public String ownerName;
    public Rotation rotation;
    public boolean isDiagonalBridge;
    public boolean isStructureUpgraded; // castle flag, lab rod, etc.
    Portal.PortalType portalType;

    public BuildingSave(BlockPos originPos, Level level, String name, String ownerName, Rotation rotation,
                        boolean isDiagonalBridge, boolean isStructureUpgraded, Portal.PortalType portalType) {
        this.originPos = originPos;
        this.level = level;
        this.name = name;
        this.ownerName = ownerName;
        this.rotation = rotation;
        this.isDiagonalBridge = isDiagonalBridge;
        this.isStructureUpgraded = isStructureUpgraded;
        this.portalType = portalType;
    }
}
