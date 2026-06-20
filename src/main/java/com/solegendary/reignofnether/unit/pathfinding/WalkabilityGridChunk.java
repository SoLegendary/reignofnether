package com.solegendary.reignofnether.unit.pathfinding;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

// 3D walkability cache for one chunk. cellKind indexed by (yIdx * SIZE + localZ) * SIZE + localX.
public final class WalkabilityGridChunk {
    public static final int SIZE = 16;

    private final int minY;
    private final int height;
    private final byte[] cellKind;

    private WalkabilityGridChunk(int minY, int height, byte[] cellKind) {
        this.minY = minY;
        this.height = height;
        this.cellKind = cellKind;
    }

    public static WalkabilityGridChunk build(Level level, ChunkPos cp) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int height = maxY - minY;
        byte[] cellKind = new byte[SIZE * SIZE * height];
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();
        for (int dz = 0; dz < SIZE; dz++) {
            for (int dx = 0; dx < SIZE; dx++) {
                int wx = baseX + dx;
                int wz = baseZ + dz;
                for (int y = minY; y < maxY; y++) {
                    cellKind[idx(dx, y - minY, dz)] = WalkabilityBuilder.classify(level, wx, y, wz);
                }
            }
        }
        return new WalkabilityGridChunk(minY, height, cellKind);
    }

    private static int idx(int localX, int yIdx, int localZ) {
        return (yIdx * SIZE + localZ) * SIZE + localX;
    }

    public byte kindAt(int wx, int y, int wz) {
        int yIdx = y - minY;
        if (yIdx < 0 || yIdx >= height) return WalkabilityBuilder.KIND_BLOCKED;
        return cellKind[idx(wx & 15, yIdx, wz & 15)];
    }
}
