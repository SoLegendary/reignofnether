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

    private static final int SNAP_RADIUS = 4;

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

    private static BlockPos snapToWalkable(Level level, BlockPos bp, MobilityClass mobility, int radius) {
        WalkabilityGrid grid = WalkabilityGrid.get(level);
        WalkabilityGridChunk c = grid.getOrBuild(level, bp.getX() >> 4, bp.getZ() >> 4);
        if (mobility.costFor(c.kindAt(bp.getX(), bp.getY(), bp.getZ())) != Float.POSITIVE_INFINITY) {
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
                    WalkabilityGridChunk nc = grid.getOrBuild(level, nx >> 4, nz >> 4);
                    if (mobility.costFor(nc.kindAt(nx, ny, nz)) == Float.POSITIVE_INFINITY) continue;
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq < bestDistSq) { bestDistSq = distSq; best = new BlockPos(nx, ny, nz); }
                }
            }
        }
        return best;
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
