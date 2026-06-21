package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
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
        target = snapToWalkable(level, target, mobility, SNAP_RADIUS);
        if (PathfinderWorkerPool.isInitialised()) {
            PathfinderWorkerPool.submit(level, start, target, reach, mobility, onReady);
        } else {
            try {
                int dilation = PathfinderConfig.dilationFor(start, target);
                ChunkSnapshot snap = ChunkSnapshot.capture(level, start, target, dilation, mobility);
                GridAStar.Result r = GridAStar.search(snap, start, target, reach, PathfinderConfig.MAX_RADIUS, PathfinderConfig.MAX_NODES);
                onReady.accept(PathConverter.toMcPath(r.waypoints, target, r.reached, snap));
            } catch (Throwable t) {
                ReignOfNether.LOGGER.error("Sync pathfinder failed", t);
                onReady.accept(null);
            }
        }
    }

    public static BlockPos snapToWalkable(Level level, BlockPos bp, MobilityClass mobility, int radius) {
        WalkabilityGrid grid = WalkabilityGrid.get(level);
        int band = radius + PathfinderConfig.VERTICAL_WINDOW_SLACK;
        int wantMinY = bp.getY() - band;
        int wantMaxY = bp.getY() + band;
        int fr = mobility.footprintRadius();
        WalkabilityGridChunk c = grid.getOrBuild(level, bp.getX() >> 4, bp.getZ() >> 4, wantMinY, wantMaxY);
        if (mobility.costFor(c.kindAt(bp.getX(), bp.getY(), bp.getZ())) != Float.POSITIVE_INFINITY
                && fitsFootprint(grid, level, bp.getX(), bp.getY(), bp.getZ(), fr, wantMinY, wantMaxY)) {
            return bp;
        }
        int bestDistSq = Integer.MAX_VALUE;
        BlockPos best = bp;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int nx = bp.getX() + dx;
                    int ny = bp.getY() + dy;
                    int nz = bp.getZ() + dz;
                    WalkabilityGridChunk nc = grid.getOrBuild(level, nx >> 4, nz >> 4, wantMinY, wantMaxY);
                    if (mobility.costFor(nc.kindAt(nx, ny, nz)) == Float.POSITIVE_INFINITY) continue;
                    // A wide unit must be able to actually stand here, or A* can never finish on it.
                    if (!fitsFootprint(grid, level, nx, ny, nz, fr, wantMinY, wantMaxY)) continue;
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq < bestDistSq) { bestDistSq = distSq; best = new BlockPos(nx, ny, nz); }
                }
            }
        }
        return best;
    }

    // Mirror of GridNeighbors.footprintBlocked for the goal snap (no WalkabilityView available here):
    // a unit with body radius r can't stand at (x,y,z) if any cell within Chebyshev r is a >=2-tall wall.
    private static boolean fitsFootprint(WalkabilityGrid grid, Level level, int x, int y, int z, int r,
                                         int wantMinY, int wantMaxY) {
        if (r <= 0) return true;
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx == 0 && dz == 0) continue;
                int cx = x + dx, cz = z + dz;
                WalkabilityGridChunk c = grid.getOrBuild(level, cx >> 4, cz >> 4, wantMinY, wantMaxY);
                if (c.solidAt(cx, y, cz) && c.solidAt(cx, y + 1, cz)) return false;
            }
        }
        return true;
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
