package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.core.BlockPos;

public final class PathfinderConfig {
    private PathfinderConfig() {}

    public static final int MAX_RADIUS = 96;
    // Worst-case node budget per A* segment; only fully spent on hard detours / unreachable targets. High
    // enough to flood the reachable area within MAX_RADIUS so units round obstacles instead of giving up at
    // the near wall. Past ~200k the radius cap, not this, is the limit (raise MAX_RADIUS + MIN_DILATION).
    public static final int MAX_NODES = 250000;
    public static final int MAX_CHAIN_SEGMENTS = 10;
    public static final int MIN_DILATION = 48;
    public static final int QUEUE_BACKPRESSURE_CAP = 500;
    // A* searches stay near the surface; only classify a Y band around the path rather than the full
    // world column. SLACK covers the Y+-1 step nodes and goal snapping that reach just outside the band.
    public static final int VERTICAL_RADIUS = 24;
    public static final int VERTICAL_WINDOW_SLACK = 8;

    // Bound the per-level walkability chunk cache so long games don't grow it without limit.
    public static final int MAX_CACHED_CHUNKS = 1024;

    // Cost multiplier a unit pays to path across a fire/magma/campfire cell when it is NOT fire-immune and
    // its DAMAGE_FIRE pathfinding malus marks fire as dangerous. Fire-immune units pay 1x instead (see
    // RtsPathfinder.fireCostFor), so they cross fire freely while everyone else routes around it.
    public static final float FIRE_AVOID_COST = 50.0f;

    // A* over an immutable snapshot is embarrassingly parallel; scale workers with cores.
    public static final int WORKER_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

    // A unit within this squared distance of its move target is "arrived": separation stops pushing it and
    // the move goal stops recalculating, so a settled/formed-up group doesn't jitter on the spot. Shared by
    // UnitSeparation and MoveToTargetBlockGoal.getMinDistToRecalculateSqr so the two thresholds can't drift.
    public static final double ARRIVAL_SETTLE_SQ = 2.25; // 1.5 blocks

    // Per-tick separation steering between crowding, same-owner, moving units.
    public static final double SEPARATION_RADIUS = 1.6;
    public static final double SEPARATION_STRENGTH = 0.04;
    public static final int SEPARATION_MAX_PER_TICK = 400;
    public static final int SEPARATION_CELL_SIZE = 2;

    public static boolean isRtsEnabled() { return UnitServerEvents.rtsPathfinding; }

    public static int dilationFor(BlockPos start, BlockPos target) {
        int manhattan = Math.abs(start.getX() - target.getX()) + Math.abs(start.getZ() - target.getZ());
        return Math.min(MAX_RADIUS, Math.max(MIN_DILATION, manhattan / 2));
    }
}
