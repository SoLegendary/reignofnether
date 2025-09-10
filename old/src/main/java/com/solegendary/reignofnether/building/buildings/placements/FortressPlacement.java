package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;

public class FortressPlacement extends ProductionPlacement implements GarrisonableBuilding {
    public final static int MAX_OCCUPANTS = 7;
    public FortressPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    @Override
    public BlockPos getIndoorSpawnPoint(ServerLevel level) {
        return this.originPos.offset(getExitPosition());
    }

    // don't use this for abilities as it may not be balanced
    public int getAttackRange() { return 30; }
    // bonus for units attacking garrisoned units
    public int getExternalAttackRangeBonus() { return 15; }

    public boolean canDestroyBlock(BlockPos relativeBp) {
        return relativeBp.getY() != 16 &&
                relativeBp.getY() != 17;
    }

    @Override
    public BlockPos getEntryPosition() {
        if (this.rotation == Rotation.NONE) {
            return new BlockPos(5,17,5);
        } else if (this.rotation == Rotation.CLOCKWISE_90) {
            return new BlockPos(-5,17,5);
        } else if (this.rotation == Rotation.CLOCKWISE_180) {
            return new BlockPos(-5,17,-5);
        } else {
            return new BlockPos(5,17,-5);
        }
    }

    @Override
    public BlockPos getExitPosition() {
        if (this.rotation == Rotation.NONE) {
            return new BlockPos(5,1,5);
        } else if (this.rotation == Rotation.CLOCKWISE_90) {
            return new BlockPos(-5,1,5);
        } else if (this.rotation == Rotation.CLOCKWISE_180) {
            return new BlockPos(-5,1,-5);
        } else {
            return new BlockPos(5,1,-5);
        }
    }

    @Override
    public boolean isFull() { return GarrisonableBuilding.getNumOccupants(this) >= MAX_OCCUPANTS; }
}
