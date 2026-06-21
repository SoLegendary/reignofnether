package com.solegendary.reignofnether.unit.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class ChunkSnapshot implements WalkabilityView {
    private final Long2ObjectMap<WalkabilityGridChunk> chunks;
    private final MobilityClass mobility;

    private ChunkSnapshot(Long2ObjectMap<WalkabilityGridChunk> chunks, MobilityClass mobility) {
        this.chunks = chunks;
        this.mobility = mobility;
    }

    public static ChunkSnapshot capture(Level level, BlockPos start, BlockPos target, int dilation, MobilityClass mobility) {
        WalkabilityGrid grid = WalkabilityGrid.get(level);
        int minX = Math.min(start.getX(), target.getX()) - dilation;
        int maxX = Math.max(start.getX(), target.getX()) + dilation;
        int minZ = Math.min(start.getZ(), target.getZ()) - dilation;
        int maxZ = Math.max(start.getZ(), target.getZ()) + dilation;
        int cx0 = minX >> 4, cx1 = maxX >> 4;
        int cz0 = minZ >> 4, cz1 = maxZ >> 4;
        // A* hugs the surface between start and target, so only classify a Y band around the path.
        int band = PathfinderConfig.VERTICAL_RADIUS + PathfinderConfig.VERTICAL_WINDOW_SLACK;
        int refY = (start.getY() + target.getY()) / 2;
        int wantMinY = refY - band;
        int wantMaxY = refY + band;
        Long2ObjectOpenHashMap<WalkabilityGridChunk> map = new Long2ObjectOpenHashMap<>((cx1 - cx0 + 1) * (cz1 - cz0 + 1));
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                map.put(ChunkPos.asLong(cx, cz), grid.getOrBuild(level, cx, cz, wantMinY, wantMaxY));
            }
        }
        return new ChunkSnapshot(map, mobility);
    }

    @Override
    public byte kindAt(int wx, int y, int wz) {
        WalkabilityGridChunk c = chunks.get(ChunkPos.asLong(wx >> 4, wz >> 4));
        if (c == null) return WalkabilityBuilder.KIND_BLOCKED;
        return c.kindAt(wx, y, wz);
    }

    @Override
    public boolean solidAt(int wx, int y, int wz) {
        WalkabilityGridChunk c = chunks.get(ChunkPos.asLong(wx >> 4, wz >> 4));
        if (c == null) return false;
        return c.solidAt(wx, y, wz);
    }

    @Override
    public MobilityClass mobility() { return mobility; }
}
