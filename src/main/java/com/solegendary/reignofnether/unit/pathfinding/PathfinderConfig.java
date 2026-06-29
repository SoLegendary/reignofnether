package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.core.BlockPos;

public final class PathfinderConfig {
    private PathfinderConfig() {}

    public static final int MAX_RADIUS = 96;
    // worst-case node budget per A* segment, only fully spent on hard detours / unreachable targets. high
    // enough to flood the reachable area within MAX_RADIUS so units round obstacles instead of stopping at the
    // near wall. past ~200k the radius cap is the real limit, not this (raise MAX_RADIUS + MIN_DILATION).
    public static final int MAX_NODES = 250000;
    // budget for a search whose goal cell has no standable spot nearby (eg. an airborne canopy leaf). a normal
    // search would flood the full MAX_NODES (~250ms) just to prove it's unreachable. cap it low: the unit still
    // best-effort approaches and returns in a few ms. chaining is skipped here too (nothing to chain toward).
    public static final int MAX_NODES_UNREACHABLE_GOAL = 8000;
    public static final int MAX_CHAIN_SEGMENTS = 10;
    public static final int MIN_DILATION = 48;
    public static final int QUEUE_BACKPRESSURE_CAP = 500;
    // walkability-grid building (getBlockState/getCollisionShape + crowd[] precompute) must run on the main
    // thread, so classifying a whole corridor in one tick spikes TPS. cap cold (uncached) chunks classified
    // per tick; cache hits are free and don't count. lower = smoother TPS but slower first path, higher = the
    // reverse. kept modest since each cold build also bakes crowd[] (a ~5x5 scan per cell), so a chunk is
    // pricier than it used to be - a batch of fresh path orders shouldn't classify many chunks in one tick.
    public static final int MAX_CHUNK_BUILDS_PER_TICK = 8;
    // a block change marks its chunk dirty (not evicted) with the changed-cell bbox; the START-phase drain
    // patches only that region via reclassifyRegion (clone + small reclassify, not a whole-chunk build). cap
    // distinct dirty chunks patched per tick; the rest stay stale-but-walkable (never removed, so units are
    // never stranded) until a later tick. same order as MAX_CHUNK_BUILDS_PER_TICK. tunable.
    public static final int MAX_CHUNK_RECLASSIFY_PER_TICK = 8;
    // cross-tick coalescing for the dirty drain: a chunk is only rebuilt once it's settled (no block change for
    // this many ticks), so a building placing 1 block/tick rebuilds ~once when done instead of every tick. stays
    // stale-but-walkable in the meantime.
    public static final int WALKABILITY_SETTLE_TICKS = 4;
    // hard ceiling on the settle wait: a chunk that never goes quiet (active farm, long mining job) is force-
    // reclassified once continuously dirty this long, bounding staleness (~2s) under perpetual churn.
    public static final int WALKABILITY_MAX_DEFER_TICKS = 40;
    // A* stays near the surface, so only classify a Y band around the path, not the full column.
    // SLACK covers the Y+-1 step nodes and goal snapping that reach just outside the band.
    public static final int VERTICAL_RADIUS = 24;
    public static final int VERTICAL_WINDOW_SLACK = 8;

    // clearance the crowding malus is precomputed with, since the cached per-cell crowd[] is shared by all
    // units. 2 = common/minimum clearance; taller units get a slightly softer steering malus but never a
    // correctness change (wideFits/headBlocked stay exact and per-unit).
    public static final int MALUS_CLEARANCE = 2;

    // the neighbour model only steps +-1 in Y, so it can't express a sheer 2+ block drop (a tall staircase):
    // a unit stalls at the lip and a wide body spins on the ledge. vanilla allows multi-block falls, so do we,
    // up to MAX_FALL_DROP, with a small per-block malus so a unit prefers a gentle route when one is comparably
    // short but still commits to the drop. 3 matches vanilla's roughly-safe fall threshold.
    public static final int MAX_FALL_DROP = 3;
    public static final float FALL_COST_PER_BLOCK = 0.3f;

    // bound on the per-level chunk cache. sized to hold a whole RTS map (world border <= 1024 blocks = 64x64
    // chunks) plus margin for capture dilation past the border edge: 72*72 = (64 + 8 each side). since the
    // prewarmed map fits, chunks never LRU-evict; they only change when marked dirty and the region rebuilds.
    public static final int MAX_CACHED_CHUNKS = 5184;

    // cost multiplier a non-fire-immune unit pays to cross a fire/magma/campfire cell (when its DAMAGE_FIRE
    // malus marks fire dangerous). fire-immune units pay 1x (see RtsPathfinder.fireCostFor) and cross freely.
    public static final float FIRE_AVOID_COST = 50.0f;
    public static final float SLIME_AVOID_COST = 10.0f;

    // A* over an immutable snapshot is embarrassingly parallel; scale workers with cores.
    public static final int WORKER_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

    // within this squared distance of its target a unit is "arrived": separation stops pushing and the move
    // goal stops recalculating, so a settled group doesn't jitter. shared by UnitSeparation and
    // MoveToTargetBlockGoal.getMinDistToRecalculateSqr so the thresholds can't drift.
    public static final double ARRIVAL_SETTLE_SQ = 2.25; // 1.5 blocks

    // Per-tick separation steering between crowding, same-owner, moving units.
    public static final double SEPARATION_RADIUS = 1.6;
    public static final double SEPARATION_STRENGTH = 0.04;
    public static final int SEPARATION_MAX_PER_TICK = 400;
    public static final int SEPARATION_CELL_SIZE = 2;

    public static int dilationFor(BlockPos start, BlockPos target) {
        int manhattan = Math.abs(start.getX() - target.getX()) + Math.abs(start.getZ() - target.getZ());
        return Math.min(MAX_RADIUS, Math.max(MIN_DILATION, manhattan / 2));
    }
}
