package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.NightSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;

public class DarknessProductionBuilding extends RangeIndicatorProductionPlacement implements NightSource {
    boolean checkBuiltServerside;
    public DarknessProductionBuilding(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol, int range, boolean checkUpgraded, boolean checkBuiltServerside) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol, range, false, checkUpgraded);
    }

    @Override
    public int getRange() {
        return (!checkBuiltServerside || isBuiltServerside) ? super.getRange() : 0;
    }

    @Override
    public int getNightRange() {
        return getRange();
    }
}
