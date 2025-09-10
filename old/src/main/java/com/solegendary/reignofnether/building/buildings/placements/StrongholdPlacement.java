package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;

public class StrongholdPlacement extends DarknessProductionBuilding implements GarrisonableBuilding {
    public final static int MAX_OCCUPANTS = 7;
    public StrongholdPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol, int range, boolean checkUpgraded, boolean checkBuiltServerside) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol, range, checkUpgraded, checkBuiltServerside);
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        updateBorderBps();
    }

    // don't use this for abilities as it may not be balanced
    public int getAttackRange() {
        return 30;
    }

    // bonus for units attacking garrisoned units
    public int getExternalAttackRangeBonus() {
        return 15;
    }

    public boolean canDestroyBlock(BlockPos relativeBp) {
        return relativeBp.getY() != 13 && relativeBp.getY() != 14;
    }

    @Override
    public BlockPos getIndoorSpawnPoint(ServerLevel level) {
        return this.originPos.offset(getExitPosition());
    }

    @Override
    public BlockPos getEntryPosition() {
        if (this.rotation == Rotation.NONE) {
            return new BlockPos(5, 14, 5);
        } else if (this.rotation == Rotation.CLOCKWISE_90) {
            return new BlockPos(-5, 14, 5);
        } else if (this.rotation == Rotation.CLOCKWISE_180) {
            return new BlockPos(-5, 14, -5);
        } else {
            return new BlockPos(5, 14, -5);
        }
    }

    @Override
    public BlockPos getExitPosition() {
        if (this.rotation == Rotation.NONE) {
            return new BlockPos(5, 2, 6);
        } else if (this.rotation == Rotation.CLOCKWISE_90) {
            return new BlockPos(-5, 2, 6);
        } else if (this.rotation == Rotation.CLOCKWISE_180) {
            return new BlockPos(-5, 2, -6);
        } else {
            return new BlockPos(5, 2, -6);
        }
    }

    @Override
    public boolean isFull() {
        return GarrisonableBuilding.getNumOccupants(this) >= MAX_OCCUPANTS;
    }
}
