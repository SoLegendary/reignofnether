package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.Random;

public class TownCentrePlacement extends ProductionPlacement {

    public boolean trainsDogs; // if false, trains cats

    public TownCentrePlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks) {
        super(building, level, originPos, rotation, ownerName, blocks, true);
        trainsDogs = new Random().nextBoolean();
    }
}
