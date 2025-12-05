package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;

public class BastionPlacement extends ProductionPlacement implements GarrisonableBuilding {
    public final static int MAX_OCCUPANTS = 4;

    public BastionPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    // don't use this for abilities as it may not be balanced
    public int getAttackRange() { return 24; }
    // bonus for units attacking garrisoned units
    public int getExternalAttackRangeBonus() { return 10; }

    public boolean canDestroyBlock(BlockPos relativeBp) {
        return relativeBp.getY() != 10 &&
                relativeBp.getY() != 11;
    }

    @Override
    public BlockPos getEntryPosition() {
        return originPos.offset(GarrisonableBuilding.rotatePos(new BlockPos(2,11,2), this.rotation));
    }

    @Override
    public BlockPos getExitPosition() {
        return originPos.offset(GarrisonableBuilding.rotatePos(new BlockPos(2,1,2), this.rotation));
    }

    @Override
    public int getCapacity() { return MAX_OCCUPANTS; }
}
