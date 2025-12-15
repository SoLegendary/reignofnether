package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;

public class StrongholdPlacement extends DarknessProductionBuilding {
    public final static int MAX_OCCUPANTS = 7;
    public StrongholdPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol, int range, boolean checkUpgraded, boolean checkBuiltServerside) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol, range, checkUpgraded, checkBuiltServerside);
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        updateBorderBps();
    }
}
