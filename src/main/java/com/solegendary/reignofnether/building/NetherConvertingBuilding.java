package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.nether.NetherBlocks;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Random;

public interface NetherConvertingBuilding {

    int NETHER_CONVERT_TICKS_MAX = 20;

    Random random = new Random();

    // randomly convert nearby overworld blocks into a nether block
    // returns true if any block was converted
    default boolean randomConvertTick(Building building, double range, double rangeMax) {
        if (!building.isBuilt || building.getLevel().isClientSide())
            return false;

        BlockPos bpOrigin = new BlockPos(
                building.centrePos.getX(),
                building.originPos.getY(),
                building.centrePos.getZ()
        );
        ArrayList<BlockPos> bps = new ArrayList<>();
        for (double x = -range; x < range; x++)
            for (double z = -range; z < range; z++)
                bps.add(bpOrigin.offset(x, 0, z));

        for (BlockPos bp : bps) {
            double distSqr = bp.distSqr(bpOrigin);
            double rangeSqr = range * range;
            double rangeMaxSqr = rangeMax * rangeMax;
            if (distSqr > rangeSqr)
                continue;

            // at half distance, chance = 50%
            double chance = (1 - (distSqr / rangeMaxSqr)) / 10;
            if (random.nextDouble() > chance)
                continue;

            for (int i = -3; i < 3; i++) {
                BlockPos bpToUpdate = bp.offset(0,i,0);
                BlockState bs = NetherBlocks.getNetherBlock(building.getLevel(), bpToUpdate);
                if (bs != null && !BuildingUtils.isPosInsideAnyBuilding(building.getLevel().isClientSide(), bpToUpdate))
                    building.getLevel().setBlockAndUpdate(bpToUpdate, bs);
            }
        }
        return true;
    }

}
