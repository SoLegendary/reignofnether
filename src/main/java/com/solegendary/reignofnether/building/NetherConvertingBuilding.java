package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.buildings.piglins.CentralPortal;
import com.solegendary.reignofnether.building.buildings.piglins.Portal;
import com.solegendary.reignofnether.nether.NetherBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Random;

public interface NetherConvertingBuilding {

    int NETHER_CONVERT_TICKS_MAX = 5;
    int MAX_CONVERTS_AFTER_MAX_RANGE = 60; // to prevent continuously processing

    Random random = new Random();

    double getMaxRange();

    // randomly convert nearby overworld blocks into a nether block
    // returns true if any block was converted
    default void netherConvertTick(Building building, double range, double rangeMax) {
        if (building.getLevel().isClientSide())
            return;

        BlockPos centrePos = building.centrePos;
        if (building instanceof CentralPortal)
            centrePos = centrePos.offset(0,-6,0);
        else if (building instanceof Portal)
            centrePos = centrePos.offset(0,-2,0);

        ArrayList<BlockPos> bps = new ArrayList<>();
        for (double x = -range; x < range; x++)
            for (double y = -range/2; y < range/2; y++)
                for (double z = -range; z < range; z++)
                    bps.add(centrePos.offset(x, y, z));

        for (BlockPos bp : bps) {
            double distSqr = bp.distSqr(centrePos);
            double rangeSqr = range * range;
            double rangeMaxSqr = getMaxRange() * getMaxRange();
            if (distSqr > rangeSqr)
                continue;

            // at half distance, chance = 50%
            double chance = (1 - (distSqr / rangeMaxSqr)) / 10;
            if (random.nextDouble() > chance)
                continue;

            BlockState bs = NetherBlocks.getNetherBlock(building.getLevel(), bp);
            BlockState bsPlant = NetherBlocks.getNetherPlantBlock(building.getLevel(), bp.above());
            if (bs != null && !BuildingUtils.isPosPartOfAnyBuilding(building.getLevel().isClientSide(), bp, true)) {
                building.getLevel().setBlockAndUpdate(bp, bs);
                if (bsPlant != null)
                    building.getLevel().setBlockAndUpdate(bp.above(), bsPlant);
            }
        }
    }
}
