package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.resources.BlockUtils;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// 3D walkability cache for one chunk, classified only over a Y band [minY, minY+height).
// cellKind indexed by (yIdx * SIZE + localZ) * SIZE + localX. cells outside the band read as BLOCKED.
// footprint clearance is checked on the fly via solidAt (GridNeighbors.wideFits), which reads correctly
// across chunk seams unlike a per-chunk precomputed field.
public final class WalkabilityGridChunk {
    public static final int SIZE = 16;

    // crowd malus quantised to a byte: stored = round(malus * CROWD_SCALE), read = byte / CROWD_SCALE.
    // malus is 0..3.0, so *16 (step 0.0625, max 48) fits an unsigned byte.
    private static final int CROWD_SCALE = 16;

    private final int minY;
    private final int height;
    private final byte[] cellKind;
    private final byte[] solid; // 1 if the block in this cell is solid-blocking (blocks a unit's head/body)
    // precomputed crowdingMalus per cell (quantised), baked once instead of rescanned per A* node. it's a pure
    // function of solid[] + a fixed clearance. computed from LOCAL solid[] only (out-of-chunk = open) - a small
    // seam approximation, fine for a soft steering term, and keeps build/reclassify pure array work.
    private final byte[] crowd;

    private WalkabilityGridChunk(int minY, int height, byte[] cellKind, byte[] solid, byte[] crowd) {
        this.minY = minY;
        this.height = height;
        this.cellKind = cellKind;
        this.solid = solid;
        this.crowd = crowd;
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
        byte[] crowd = new byte[SIZE * SIZE * height];
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
        // second pass: crowd[] reads neighbouring solid[] cells, so it needs the whole chunk's solid[] filled.
        GridNeighbors.SolidProbe probe = solidProbe(solid, minY, height, baseX, baseZ);
        for (int lz = 0; lz < SIZE; lz++)
            for (int lx = 0; lx < SIZE; lx++)
                computeCrowdColumn(probe, crowd, minY, height, baseX, baseZ, lx, lz, 0, height - 1);
        return new WalkabilityGridChunk(minY, height, cellKind, solid, crowd);
    }

    // copy-on-write patch of just the cells a block change touched, instead of rebuilding the whole band (build()).
    // reclassifies only the changed footprint columns over the changed Y levels, plus a 2-cell halo for crowd[]
    // (crowdingMalus reads a distance-2 ring + y..y+2). the band-wide crowd pass over a whole chunk is what made
    // construction stutter; this bounds it to the change. [minLX..maxLX]/[minLZ..maxLZ] are local 0..15,
    // [regionMinY..regionMaxY] are WORLD-Y (inclusive). clones the arrays and returns a new chunk, never mutating
    // the live one (A* threads read it).
    public WalkabilityGridChunk reclassifyRegion(Level level, ChunkPos cp,
                                                 int minLX, int maxLX, int minLZ, int maxLZ,
                                                 int regionMinY, int regionMaxY) {
        int maxY = minY + height;
        int kMinY = Math.max(minY, regionMinY);       // clamp the changed-cell Y span to this chunk's band
        int kMaxY = Math.min(maxY - 1, regionMaxY);
        if (kMinY > kMaxY) return this;               // whole change outside the band -> no-op

        byte[] newKind = cellKind.clone();            // COW: only ever write the clones
        byte[] newSolid = solid.clone();
        byte[] newCrowd = crowd.clone();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos mpBelow = new BlockPos.MutableBlockPos();

        // pass 1: re-classify cellKind/solid for the dirty columns over the changed Y span. a block at (x,z) only
        // changes cells in its own column, so no halo here - and all solid[] edits land before pass 2 reads them.
        for (int lz = minLZ; lz <= maxLZ; lz++)
            for (int lx = minLX; lx <= maxLX; lx++) {
                int wx = baseX + lx, wz = baseZ + lz;
                for (int y = kMinY; y <= kMaxY; y++)
                    classifyCell(level, wx, y, wz, newKind, newSolid, idx(lx, y - minY, lz), mp, mpBelow);
            }

        // pass 2: recompute crowd[] over the dirty region grown by a 2-cell horizontal halo and down 2 in Y,
        // since a changed solid cell at world-Y cy affects crowd of cells y in [cy-2, cy] within +-2 in X/Z.
        // halo cells keep their unchanged (still-correct) old solid in newSolid, so this matches build() exactly.
        GridNeighbors.SolidProbe probe = solidProbe(newSolid, minY, height, baseX, baseZ);
        int hMinLX = Math.max(0, minLX - 2), hMaxLX = Math.min(SIZE - 1, maxLX + 2);
        int hMinLZ = Math.max(0, minLZ - 2), hMaxLZ = Math.min(SIZE - 1, maxLZ + 2);
        int cMinYIdx = Math.max(0, (kMinY - 2) - minY);
        int cMaxYIdx = Math.min(height - 1, kMaxY - minY);
        for (int lz = hMinLZ; lz <= hMaxLZ; lz++)
            for (int lx = hMinLX; lx <= hMaxLX; lx++)
                computeCrowdColumn(probe, newCrowd, minY, height, baseX, baseZ, lx, lz, cMinYIdx, cMaxYIdx);

        return new WalkabilityGridChunk(minY, height, newKind, newSolid, newCrowd);
    }

    // classify one cell into cellKind[i]/solid[i]. the single source of truth for how a cell becomes
    // walkable + body-blocking, used by build().
    private static void classifyCell(Level level, int wx, int y, int wz, byte[] cellKind, byte[] solid, int i,
                                     BlockPos.MutableBlockPos mp, BlockPos.MutableBlockPos mpBelow) {
        cellKind[i] = WalkabilityBuilder.classify(level, wx, y, wz);
        // leaves report non-solid but block the body, and fences/walls/closed gates are 1.5-tall barriers - count
        // both as body-blocking so they register as walls for the crowding malus and block footprints. (classify
        // still treats them as no floor support, so units don't walk on them.)
        mp.set(wx, y, wz);
        BlockState bs = level.getBlockState(mp);
        boolean cellSolid = MiscUtil.isSolidBlocking(level, mp) || BlockUtils.isLeafBlock(bs)
                || WalkabilityBuilder.isFenceLike(bs);
        // a fence/wall directly below is 1.5 tall and reaches into this cell. mark this cell solid too so
        // wallBeside (which probes above the feet, skipping the feet cell as a climbable step) detects a
        // ground-level fence/wall - otherwise a 1-tall fence at a neighbour's feet is invisible to the malus.
        if (!cellSolid) {
            mpBelow.set(wx, y - 1, wz);
            if (WalkabilityBuilder.isFenceLike(level.getBlockState(mpBelow))) cellSolid = true;
        }
        solid[i] = cellSolid ? (byte) 1 : 0;
    }

    private static int idx(int localX, int yIdx, int localZ) {
        return (yIdx * SIZE + localZ) * SIZE + localX;
    }

    // solid-probe over one chunk's local solid[]: world (sx,sy,sz) outside this chunk's columns or band reads as
    // open (false). the seam approximation that lets crowd[] be precomputed without cross-chunk reads.
    private static GridNeighbors.SolidProbe solidProbe(byte[] solid, int minY, int height, int baseX, int baseZ) {
        return (sx, sy, sz) -> {
            int lx = sx - baseX, lz = sz - baseZ, yIdx = sy - minY;
            if (lx < 0 || lx >= SIZE || lz < 0 || lz >= SIZE || yIdx < 0 || yIdx >= height) return false;
            return solid[idx(lx, yIdx, lz)] != 0;
        };
    }

    // fill crowd[] for one local column over yIdx [yIdxLo, yIdxHi] (inclusive) from GridNeighbors.crowdingMalus
    // (fixed MALUS_CLEARANCE since one cached value serves all units), quantised to a byte. build() passes the
    // full band; reclassifyRegion passes only the changed Y slice.
    private static void computeCrowdColumn(GridNeighbors.SolidProbe probe, byte[] crowd, int minY, int height,
                                           int baseX, int baseZ, int lx, int lz, int yIdxLo, int yIdxHi) {
        int wx = baseX + lx, wz = baseZ + lz;
        for (int yIdx = yIdxLo; yIdx <= yIdxHi; yIdx++) {
            float malus = GridNeighbors.crowdingMalus(probe, PathfinderConfig.MALUS_CLEARANCE, wx, minY + yIdx, wz);
            crowd[idx(lx, yIdx, lz)] = (byte) Math.min(255, Math.round(malus * CROWD_SCALE));
        }
    }

    public byte kindAt(int wx, int y, int wz) {
        int yIdx = y - minY;
        if (yIdx < 0 || yIdx >= height) return WalkabilityBuilder.KIND_BLOCKED;
        return cellKind[idx(wx & 15, yIdx, wz & 15)];
    }

    // out-of-band cells read as non-solid (open air) so a step-up near the band edge isn't falsely blocked
    public boolean solidAt(int wx, int y, int wz) {
        int yIdx = y - minY;
        if (yIdx < 0 || yIdx >= height) return false;
        return solid[idx(wx & 15, yIdx, wz & 15)] != 0;
    }

    // precomputed crowding malus for this cell (dequantised). out-of-band reads as 0 (open, no steering cost).
    public float crowdAt(int wx, int y, int wz) {
        int yIdx = y - minY;
        if (yIdx < 0 || yIdx >= height) return 0f;
        return (crowd[idx(wx & 15, yIdx, wz & 15)] & 0xFF) / (float) CROWD_SCALE;
    }

    public int minY() { return minY; }

    public int maxY() { return minY + height; }

    // true when this chunk's band fully contains [bandMinY, bandMaxY)
    public boolean covers(int bandMinY, int bandMaxY) {
        return minY <= bandMinY && maxY() >= bandMaxY;
    }
}
