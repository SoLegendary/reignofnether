package com.solegendary.reignofnether.unit.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class ChunkSnapshot implements WalkabilityView {
    private final Long2ObjectMap<WalkabilityGridChunk> chunks;
    private final MobilityClass mobility;
    private final int clearanceCells;
    private final int footprintRadius;
    private final float fireCost;
    private final boolean canClimb;

    private ChunkSnapshot(Long2ObjectMap<WalkabilityGridChunk> chunks, MobilityClass mobility, int clearanceCells, int footprintRadius, float fireCost, boolean canClimb) {
        this.chunks = chunks;
        this.mobility = mobility;
        this.clearanceCells = clearanceCells;
        this.footprintRadius = footprintRadius;
        this.fireCost = fireCost;
        this.canClimb = canClimb;
    }

    public static ChunkSnapshot capture(Level level, BlockPos start, BlockPos target, int dilation, MobilityClass mobility, int clearanceCells, int footprintRadius, float fireCost, boolean canClimb) {
        WalkabilityGrid grid = WalkabilityGrid.get(level);
        CaptureRegion region = regionFor(start, target, dilation);
        Long2ObjectOpenHashMap<WalkabilityGridChunk> map =
                new Long2ObjectOpenHashMap<>((region.cx1 - region.cx0 + 1) * (region.cz1 - region.cz0 + 1));
        for (int cx = region.cx0; cx <= region.cx1; cx++) {
            for (int cz = region.cz0; cz <= region.cz1; cz++) {
                map.put(ChunkPos.asLong(cx, cz), grid.getOrBuild(level, cx, cz, region.wantMinY, region.wantMaxY));
            }
        }
        return new ChunkSnapshot(map, mobility, clearanceCells, footprintRadius, fireCost, canClimb);
    }

    // The chunk rectangle + Y band a capture(start, target, dilation) needs. Shared with the deferred build
    // queue (PathfinderWorkerPool) so "which chunks/band to warm" can never drift from what capture reads.
    public static final class CaptureRegion {
        public final int cx0, cx1, cz0, cz1;
        public final int wantMinY, wantMaxY;

        private CaptureRegion(int cx0, int cx1, int cz0, int cz1, int wantMinY, int wantMaxY) {
            this.cx0 = cx0; this.cx1 = cx1; this.cz0 = cz0; this.cz1 = cz1;
            this.wantMinY = wantMinY; this.wantMaxY = wantMaxY;
        }

        public int chunkCount() { return (cx1 - cx0 + 1) * (cz1 - cz0 + 1); }
    }

    public static CaptureRegion regionFor(BlockPos start, BlockPos target, int dilation) {
        int minX = Math.min(start.getX(), target.getX()) - dilation;
        int maxX = Math.max(start.getX(), target.getX()) + dilation;
        int minZ = Math.min(start.getZ(), target.getZ()) - dilation;
        int maxZ = Math.max(start.getZ(), target.getZ()) + dilation;
        // A* hugs the surface between start and target, so only classify a Y band around the path.
        int band = PathfinderConfig.VERTICAL_RADIUS + PathfinderConfig.VERTICAL_WINDOW_SLACK;
        int refY = (start.getY() + target.getY()) / 2;
        return new CaptureRegion(minX >> 4, maxX >> 4, minZ >> 4, maxZ >> 4, refY - band, refY + band);
    }

    private WalkabilityGridChunk chunkAt(int wx, int wz) {
        return chunks.get(ChunkPos.asLong(wx >> 4, wz >> 4));
    }

    @Override
    public byte kindAt(int wx, int y, int wz) {
        WalkabilityGridChunk c = chunkAt(wx, wz);
        return c == null ? WalkabilityBuilder.KIND_BLOCKED : c.kindAt(wx, y, wz);
    }

    @Override
    public boolean solidAt(int wx, int y, int wz) {
        WalkabilityGridChunk c = chunkAt(wx, wz);
        return c != null && c.solidAt(wx, y, wz);
    }

    @Override
    public float crowdAt(int wx, int y, int wz) {
        WalkabilityGridChunk c = chunkAt(wx, wz);
        return c == null ? 0f : c.crowdAt(wx, y, wz);
    }

    @Override
    public MobilityClass mobility() { return mobility; }

    @Override
    public int clearanceCells() { return clearanceCells; }

    @Override
    public int footprintRadius() { return footprintRadius; }

    @Override
    public float fireCost() { return fireCost; }

    @Override
    public boolean canClimb() { return canClimb; }
}
