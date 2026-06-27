package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;

import java.util.function.Consumer;

// Facade: turn a (mob, target) into a vanilla Path delivered to a callback.
// Snaps the goal, captures a snapshot, hands it to the worker pool.
public final class RtsPathfinder {
    private RtsPathfinder() {}

    public static final byte TYPE_VANILLA = 0;
    public static final byte TYPE_ASTAR = 1;
    public static final byte TYPE_FAILED = 2;

    public static final int SNAP_RADIUS = 4;

    public static void requestPath(Mob mob, BlockPos target, int reach, MobilityClass mobility, Consumer<Path> onReady) {
        Level level = mob.level();
        if (level == null) { onReady.accept(null); return; }
        BlockPos start = mob.blockPosition();
        if (isUnloaded(level, start) || isUnloaded(level, target)) {
            onReady.accept(null);
            return;
        }
        int clearanceCells = Math.max(2, Mth.ceil(mob.getBbHeight()));
        float fireCost = fireCostFor(mob);
        // Only spiders with wall-climbing toggled on may scale vertical walls; PoisonSpiderUnit inherits this.
        boolean canClimb = mob instanceof SpiderUnit su && su.isWallClimbing();
        int footprintRadius = footprintRadiusFor(mob);
        target = snapToWalkable(level, target, mobility, footprintRadius, clearanceCells, fireCost, SNAP_RADIUS);
        if (PathfinderWorkerPool.isInitialised()) {
            PathfinderWorkerPool.submit(level, start, target, reach, mobility, clearanceCells, footprintRadius, fireCost, canClimb, mob::isAlive, onReady);
        } else {
            try {
                int dilation = PathfinderConfig.dilationFor(start, target);
                ChunkSnapshot snap = ChunkSnapshot.capture(level, start, target, dilation, mobility, clearanceCells, footprintRadius, fireCost, canClimb);
                GridAStar.Result r = GridAStar.search(snap, start, target, reach, PathfinderConfig.MAX_RADIUS, PathfinderConfig.MAX_NODES);
                onReady.accept(PathConverter.toMcPath(r.waypoints, target, r.reached, snap));
            } catch (Throwable t) {
                ReignOfNether.LOGGER.error("Sync pathfinder failed", t);
                onReady.accept(null);
            }
        }
    }

    // Tile footprint radius for this mob, vanilla style: floor(width + 1) - 1. 0 for a <=1-wide unit, 1 for a
    // bear. Used by both requestPath and the debug overlay, so the overlay scores cells exactly like A* does.
    // A climbing spider is pathed 1-wide (radius 0): a 3x3 footprint can't perch on a cliff edge (it overhangs
    // the drop), so the climb up the wall could never connect to the plateau. 1-wide fixes that and is cheaper
    // (skips the per-node wideFits 3x3 check) - the nimble-spider trade-off.
    public static int footprintRadiusFor(Mob mob) {
        if (mob instanceof SpiderUnit su && su.isWallClimbing()) return 0;
        return Math.max(0, Mth.floor(mob.getBbWidth() + 1.0f) - 1);
    }

    // Fire/magma cost for this unit: the DAMAGE_FIRE malus plus per-unit fire immunity, so fire-immune units
    // cross fire freely and everyone else routes around it. Called once per request (never per A* cell), since
    // fireImmune() can do an expensive building lookup.
    private static float fireCostFor(Mob mob) {
        if (mob.fireImmune()) return 1.0f;
        float malus = mob.getPathfindingMalus(BlockPathTypes.DAMAGE_FIRE);
        return malus <= 0f ? 1.0f : PathfinderConfig.FIRE_AVOID_COST;
    }

    public static BlockPos snapToWalkable(Level level, BlockPos bp, MobilityClass mobility, int footprintRadius,
                                          int clearanceCells, float fireCost, int radius) {
        // Snap over a small snapshot so the goal reuses the EXACT walkability + footprint logic A* uses (the
        // snapped goal can never be a cell A* would reject); the +footprintRadius+1 dilation keeps wideFits
        // from reading an uncaptured (BLOCKED) edge cell. canClimb=false: a goal must snap to standable ground,
        // never to a mid-air wall-cling cell, so snapping always uses the ground footprint gate.
        ChunkSnapshot view = ChunkSnapshot.capture(level, bp, bp, radius + footprintRadius + 1,
                mobility, clearanceCells, footprintRadius, fireCost, false);
        if (standable(view, bp.getX(), bp.getY(), bp.getZ())) return bp;
        int bestDistSq = Integer.MAX_VALUE;
        BlockPos best = bp;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int nx = bp.getX() + dx;
                    int ny = bp.getY() + dy;
                    int nz = bp.getZ() + dz;
                    if (!standable(view, nx, ny, nz)) continue;
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq < bestDistSq) { bestDistSq = distSq; best = new BlockPos(nx, ny, nz); }
                }
            }
        }
        return best;
    }

    // The unit can stand (and its whole footprint fits) at this cell - the same gate A* applies to a node.
    private static boolean standable(WalkabilityView view, int x, int y, int z) {
        if (Float.isInfinite(view.mobility().costFor(view.kindAt(x, y, z), view.fireCost()))) return false;
        return GridNeighbors.wideFits(view, x, y, z);
    }

    private static boolean isUnloaded(Level level, BlockPos bp) {
        if (level instanceof ServerLevel sl) {
            ChunkPos cp = new ChunkPos(bp);
            LevelChunk c = sl.getChunkSource().getChunkNow(cp.x, cp.z);
            return c == null;
        }
        return false;
    }
}
