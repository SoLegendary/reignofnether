package com.solegendary.reignofnether.unit.pathfinding;

// Shared movement model for grid A* and flow fields so the two can never diverge.
// 16 neighbours: 8 XZ moves at the same Y, plus 4 cardinal moves at Y+1 and Y-1.
public final class GridNeighbors {
    private GridNeighbors() {}

    public static final float SQRT2 = 1.41421356f;

    public static final int[] DX = { 1, -1, 0, 0,   1, -1, 1, -1,   1, -1, 0, 0,   1, -1, 0, 0 };
    public static final int[] DZ = { 0, 0, 1, -1,   1, -1, -1, 1,   0, 0, 1, -1,   0, 0, 1, -1 };
    public static final int[] DY = { 0, 0, 0, 0,    0, 0, 0, 0,     1, 1, 1, 1,   -1, -1, -1, -1 };
    public static final int COUNT = 16;

    // Cost of stepping along neighbour d (before terrain cost multiplier).
    public static float stepCost(int d) {
        float c;
        if (DX[d] != 0 && DZ[d] != 0) c = SQRT2;
        else if (DX[d] != 0 || DZ[d] != 0) c = 1f;
        else c = 0f;
        if (DY[d] != 0) c = Math.max(c, 1f) + 0.2f;
        return c;
    }

    // A unit with body radius r (in cells) cannot occupy (x,y,z) if any cell within Chebyshev distance r
    // is a >=2-tall wall (solid at both y and y+1). A single climbable step is solid at y but air at y+1,
    // so it never counts as a wall — this keeps stairs/ramps passable for wide units while still pushing
    // them one cell off real walls. r<=0 (1-wide units) imposes no constraint. Uses solidAt, which reads
    // correctly across chunk boundaries (unlike a per-chunk precomputed clearance, which must edge-clamp).
    public static boolean footprintBlocked(WalkabilityView view, int x, int y, int z, int r) {
        if (r <= 0) return false;
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx == 0 && dz == 0) continue;
                int cx = x + dx, cz = z + dz;
                if (view.solidAt(cx, y, cz) && view.solidAt(cx, y + 1, cz)) return true;
            }
        }
        return false;
    }

    // A diagonal XZ move is only legal if both flanking orthogonal cells are passable (no corner cutting).
    public static boolean diagonalBlocked(WalkabilityView view, MobilityClass mob, int fromX, int toY, int fromZ, int dx, int dz) {
        if (dx == 0 || dz == 0) return false;
        if (Float.isInfinite(mob.costFor(view.kindAt(fromX + dx, toY, fromZ)))) return true;
        if (Float.isInfinite(mob.costFor(view.kindAt(fromX, toY, fromZ + dz)))) return true;
        return false;
    }
}
