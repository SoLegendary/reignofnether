package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.nether.NetherBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Random;

public class NetherConversionZone {

    private final int MAX_TICKS = 5;
    private final int MAX_CONVERTS_AFTER_CONSTANT_RANGE = 60; // to prevent continuously processing

    private final double maxRange;
    private final Level level;
    private final BlockPos origin;
    private final Random random = new Random();

    private boolean isRestoring = false; // if false, start converting Nether back into Overworld blocks
    private double range;
    private int ticksLeft = MAX_TICKS;
    private int convertsAfterConstantRange = 0; // after reaching max or min range, keep converting for a few more ticks

    public NetherConversionZone(Level level, BlockPos origin, double maxRange, double range) {
        this.level = level;
        this.origin = origin;
        this.maxRange = maxRange;
        this.range = range;
    }

    public double getMaxRange() {
        return maxRange;
    }

    public void startRestoring() {
        convertsAfterConstantRange = 0;
        isRestoring = true;
    }

    public boolean isDone() {
        return !level.isClientSide() && isRestoring && convertsAfterConstantRange >= MAX_CONVERTS_AFTER_CONSTANT_RANGE;
    }

    public void tick() {
        if (!level.isClientSide()) {
            ticksLeft -= 1;
            if (ticksLeft <= 0 && convertsAfterConstantRange < MAX_CONVERTS_AFTER_CONSTANT_RANGE) {
                if (!isRestoring) {
                    netherConvertTick();
                    if (range < maxRange)
                        range += 0.1f;
                    else
                        convertsAfterConstantRange += 1;
                }
                else {
                    overworldRestoreTick();
                    if (range > 0)
                        range -= 0.05f;
                    else
                        convertsAfterConstantRange += 1;
                }
                ticksLeft = MAX_TICKS;
            }
        }
    }

    // randomly convert nether blocks into overworld blocks at decreasing ranges
    private void overworldRestoreTick() {
        double restoreRange = range + 5;

        ArrayList<BlockPos> bps = new ArrayList<>();
        for (double x = -restoreRange; x < restoreRange; x++)
            for (double y = -restoreRange/2; y < restoreRange/2; y++)
                for (double z = -restoreRange; z < restoreRange; z++)
                    bps.add(origin.offset(x, y, z));

        for (BlockPos bp : bps) {
            double distSqr = bp.distSqr(origin);
            double rangeSqr = range * range;
            double rangeMaxSqr = maxRange * maxRange;
            if (distSqr < rangeSqr)
                continue;

            double chance = 0.15f;
            if (random.nextDouble() > chance)
                continue;

            BlockState bs = NetherBlocks.getOverworldBlock(level, bp);
            BlockState bsPlant = NetherBlocks.getOverworldPlantBlock(level, bp.above(), true);
            if (bs != null && !BuildingUtils.isPosPartOfAnyBuilding(level.isClientSide(), bp, true, (int) (maxRange * 2))) {
                level.setBlockAndUpdate(bp, bs);
                if (bsPlant != null)
                    level.setBlockAndUpdate(bp.above(), bsPlant);
            }
        }
    }

    // randomly convert overworld blocks into nether blocks at increasing ranges
    private void netherConvertTick() {
        ArrayList<BlockPos> bps = new ArrayList<>();
        for (double x = -range; x < range; x++)
            for (double y = -range/2; y < range/2; y++)
                for (double z = -range; z < range; z++)
                    bps.add(origin.offset(x, y, z));

        for (BlockPos bp : bps) {
            double distSqr = bp.distSqr(origin);
            double rangeSqr = range * range;
            double rangeMaxSqr = maxRange * maxRange;
            if (distSqr > rangeSqr)
                continue;

            // give a higher chance to convert blocks at closer distance
            double chance = (1 - (distSqr / rangeMaxSqr)) / 10;

            if (level.getBlockState(bp).getBlock() == Blocks.WATER) {
                int adjObs = 0;
                if (level.getBlockState(bp.north()).getBlock() == Blocks.OBSIDIAN) adjObs += 1;
                if (level.getBlockState(bp.south()).getBlock() == Blocks.OBSIDIAN) adjObs += 1;
                if (level.getBlockState(bp.east()).getBlock() == Blocks.OBSIDIAN) adjObs += 1;
                if (level.getBlockState(bp.west()).getBlock() == Blocks.OBSIDIAN) adjObs += 1;
                if (adjObs >= 3)
                    chance = 1.0f;
            }
            if (random.nextDouble() > chance)
                continue;

            BlockState bs = NetherBlocks.getNetherBlock(level, bp);
            BlockState bsPlant = NetherBlocks.getNetherPlantBlock(level, bp.above());
            if (bs != null && !BuildingUtils.isPosPartOfAnyBuilding(level.isClientSide(), bp, true, (int) (maxRange * 2))) {
                level.setBlockAndUpdate(bp, bs);
                if (bsPlant != null)
                    level.setBlockAndUpdate(bp.above(), bsPlant);
            }
        }
    }
}
