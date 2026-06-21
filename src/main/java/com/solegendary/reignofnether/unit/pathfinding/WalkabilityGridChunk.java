package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

// 3D walkability cache for one chunk, classified only over a Y band [minY, minY+height).
// cellKind indexed by (yIdx * SIZE + localZ) * SIZE + localX. Cells outside the band read as BLOCKED.
// Wide-unit footprint clearance is checked on the fly via solidAt (GridNeighbors.footprintBlocked) so it
// reads correctly across chunk seams, rather than a per-chunk precomputed field that has to edge-clamp.
public final class WalkabilityGridChunk {
    public static final int SIZE = 16;

    private final int minY;
    private final int height;
    private final byte[] cellKind;
    private final byte[] solid; // 1 if the block in this cell is solid-blocking (blocks a unit's head/body)

    private WalkabilityGridChunk(int minY, int height, byte[] cellKind, byte[] solid) {
        this.minY = minY;
        this.height = height;
        this.cellKind = cellKind;
        this.solid = solid;
    }

    public static WalkabilityGridChunk build(Level level, ChunkPos cp, int windowMinY, int windowMaxY) {
        int worldMin = level.getMinBuildHeight();
        int worldMax = level.getMaxBuildHeight();
        int minY = Math.max(worldMin, windowMinY);
        int maxY = Math.min(worldMax, windowMaxY);
        if (maxY <= minY) maxY = Math.min(worldMax, minY + 1);
        int height = maxY - minY;
        byte[] cellKind = new byte[SIZE * SIZE * height];
        byte[] solid = new byte[SIZE * SIZE * height];
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();
        for (int dz = 0; dz < SIZE; dz++) {
            for (int dx = 0; dx < SIZE; dx++) {
                int wx = baseX + dx;
                int wz = baseZ + dz;
                for (int y = minY; y < maxY; y++) {
                    int i = idx(dx, y - minY, dz);
                    cellKind[i] = WalkabilityBuilder.classify(level, wx, y, wz);
                    solid[i] = MiscUtil.isSolidBlocking(level, mp.set(wx, y, wz)) ? (byte) 1 : 0;
                }
            }
        }
        return new WalkabilityGridChunk(minY, height, cellKind, solid);
    }

    private static int idx(int localX, int yIdx, int localZ) {
        return (yIdx * SIZE + localZ) * SIZE + localX;
    }

    public byte kindAt(int wx, int y, int wz) {
        int yIdx = y - minY;
        if (yIdx < 0 || yIdx >= height) return WalkabilityBuilder.KIND_BLOCKED;
        return cellKind[idx(wx & 15, yIdx, wz & 15)];
    }

    // Out-of-band cells read as non-solid (open air) so a step-up near the band edge isn't falsely blocked.
    public boolean solidAt(int wx, int y, int wz) {
        int yIdx = y - minY;
        if (yIdx < 0 || yIdx >= height) return false;
        return solid[idx(wx & 15, yIdx, wz & 15)] != 0;
    }

    public int minY() { return minY; }

    public int maxY() { return minY + height; }

    // True when this chunk's band fully contains [bandMinY, bandMaxY).
    public boolean covers(int bandMinY, int bandMaxY) {
        return minY <= bandMinY && maxY() >= bandMaxY;
    }
}
