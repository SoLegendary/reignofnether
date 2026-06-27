package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.resources.BlockUtils;
import com.solegendary.reignofnether.util.MiscUtil;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// 3D walkability cache for one chunk, classified only over a Y band [minY, minY+height).
// cellKind indexed by (yIdx * SIZE + localZ) * SIZE + localX. Cells outside the band read as BLOCKED.
// Footprint clearance is checked on the fly via solidAt (GridNeighbors.wideFits), which reads correctly
// across chunk seams unlike a per-chunk precomputed field.
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
        BlockPos.MutableBlockPos mpBelow = new BlockPos.MutableBlockPos();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();
        for (int dz = 0; dz < SIZE; dz++) {
            for (int dx = 0; dx < SIZE; dx++) {
                int wx = baseX + dx;
                int wz = baseZ + dz;
                for (int y = minY; y < maxY; y++) {
                    int i = idx(dx, y - minY, dz);
                    classifyCell(level, wx, y, wz, cellKind, solid, i, mp, mpBelow);
                }
            }
        }
        return new WalkabilityGridChunk(minY, height, cellKind, solid);
    }

    // Re-derive ONE local column (packed (lz<<4)|lx) into clones of this chunk's arrays, returning a NEW
    // immutable chunk - never mutating the receiver. Worker-pool threads holding the old chunk through a
    // captured snapshot keep reading consistent (if briefly stale) data; the swap into the cache is atomic
    // (see WalkabilityGrid.drainDirtyColumns). Same minY/height as the original, so covers() and every
    // snapshot's band assumption stay valid. ~height cell classifications per column vs SIZE*SIZE for build().
    public WalkabilityGridChunk reclassifyColumns(Level level, ChunkPos cp, IntCollection localColumns) {
        byte[] newKind = cellKind.clone();
        byte[] newSolid = solid.clone();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();
        int maxY = minY + height;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos mpBelow = new BlockPos.MutableBlockPos();
        IntIterator it = localColumns.iterator();
        while (it.hasNext()) {
            int packed = it.nextInt();
            int lx = packed & 15;
            int lz = (packed >> 4) & 15;
            int wx = baseX + lx;
            int wz = baseZ + lz;
            for (int y = minY; y < maxY; y++) {
                int i = idx(lx, y - minY, lz);
                classifyCell(level, wx, y, wz, newKind, newSolid, i, mp, mpBelow);
            }
        }
        return new WalkabilityGridChunk(minY, height, newKind, newSolid);
    }

    // Classify one cell (x,y,z) into cellKind[i]/solid[i]. The single definition of how a cell becomes
    // walkable + body-blocking, shared by build() and reclassifyColumns() so the two can never drift.
    private static void classifyCell(Level level, int wx, int y, int wz, byte[] cellKind, byte[] solid, int i,
                                     BlockPos.MutableBlockPos mp, BlockPos.MutableBlockPos mpBelow) {
        cellKind[i] = WalkabilityBuilder.classify(level, wx, y, wz);
        // Leaves report non-solid but have full collision, so count them as body-blocking here
        // (classify still treats them as no floor support, so units don't walk on them). Fences,
        // walls and closed fence gates are 1.5-tall barriers too - count them as body-blocking so
        // they register as walls for the crowding malus and block footprints, like solid blocks.
        mp.set(wx, y, wz);
        BlockState bs = level.getBlockState(mp);
        boolean cellSolid = MiscUtil.isSolidBlocking(level, mp) || BlockUtils.isLeafBlock(bs)
                || WalkabilityBuilder.isFenceLike(bs);
        // A fence/wall directly BELOW is 1.5 tall and reaches up into this cell. Mark this cell solid
        // too so wallBeside (which probes ABOVE the feet, skipping the feet cell as a climbable step)
        // actually detects a ground-level fence/wall as a wall - otherwise a 1-tall fence sitting at a
        // neighbour's feet is invisible to the crowding malus.
        if (!cellSolid) {
            mpBelow.set(wx, y - 1, wz);
            if (WalkabilityBuilder.isFenceLike(level.getBlockState(mpBelow))) cellSolid = true;
        }
        solid[i] = cellSolid ? (byte) 1 : 0;
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
