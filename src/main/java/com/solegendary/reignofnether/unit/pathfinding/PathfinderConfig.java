package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.core.BlockPos;

public final class PathfinderConfig {
    private PathfinderConfig() {}

    public static final int MAX_RADIUS = 96;
    public static final int MAX_NODES = 36000;
    public static final int MAX_CHAIN_SEGMENTS = 10;
    public static final int MIN_DILATION = 32;
    public static final int QUEUE_BACKPRESSURE_CAP = 500;
    public static final int WORKER_THREADS = 1;

    public static boolean isRtsEnabled() { return UnitServerEvents.rtsPathfinding; }

    public static int dilationFor(BlockPos start, BlockPos target) {
        int manhattan = Math.abs(start.getX() - target.getX()) + Math.abs(start.getZ() - target.getZ());
        return Math.min(MAX_RADIUS, Math.max(MIN_DILATION, manhattan / 2));
    }
}
