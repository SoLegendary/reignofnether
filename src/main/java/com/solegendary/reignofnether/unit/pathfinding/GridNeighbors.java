package com.solegendary.reignofnether.unit.pathfinding;

// Shared movement model for grid A* and flow fields so the two can never diverge.
// 20 neighbours: 8 XZ moves at the same Y, 4 cardinal moves at Y+1 and Y-1, and 4 diagonal moves at Y-1
// so wide units can descend the corner/pyramid staircases they otherwise had to zig-zag down one cell at a time.
public final class GridNeighbors {
    private GridNeighbors() {}

    public static final float SQRT2 = 1.41421356f;

    public static final int[] DX = { 1, -1, 0, 0,   1, -1, 1, -1,   1, -1, 0, 0,   1, -1, 0, 0,   1, -1, 1, -1 };
    public static final int[] DZ = { 0, 0, 1, -1,   1, -1, -1, 1,   0, 0, 1, -1,   0, 0, 1, -1,   1, -1, -1, 1 };
    public static final int[] DY = { 0, 0, 0, 0,    0, 0, 0, 0,     1, 1, 1, 1,   -1, -1, -1, -1,  -1, -1, -1, -1 };
    public static final int COUNT = 20;

    // Cost of stepping along neighbour d (before terrain cost multiplier).
    public static float stepCost(int d) {
        float c;
        if (DX[d] != 0 && DZ[d] != 0) c = SQRT2;
        else if (DX[d] != 0 || DZ[d] != 0) c = 1f;
        else c = 0f;
        if (DY[d] != 0) c = Math.max(c, 1f) + 0.2f;
        return c;
    }

    // Wide-unit footprint test, CENTRED on the node (the unit follows cell centres via CenteredPath). Every cell
    // the centred body can touch - the (2r+1)x(2r+1) block around the node, i.e. 3x3 for a bear (r=1) - must be
    // standable, so the body never overhangs a wall, corner or drop on ANY side. (An anchored +X/+Z box would
    // miss the -X/-Z cells the centred body pokes into and the unit would clip them.) Each cell may sit one step
    // above/below the node so the block can straddle a stair.
    public static boolean wideFits(WalkabilityView view, int x, int y, int z) {
        int r = view.footprintRadius();
        if (r <= 0) return true;
        MobilityClass mob = view.mobility();
        int clearance = view.clearanceCells();
        float fireCost = view.fireCost();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                if (!cellStandable(view, mob, x + dx, z + dz, y, clearance, fireCost)) return false;
        return true;
    }

    // A footprint cell is standable if the unit can rest in it within one step of the node's Y (so the box can
    // span a single stair step) with enough headroom for the unit's full height.
    public static boolean cellStandable(WalkabilityView view, MobilityClass mob, int cx, int cz, int y, int clearance, float fireCost) {
        for (int fy = y - 1; fy <= y + 1; fy++) {
            if (Float.isInfinite(mob.costFor(view.kindAt(cx, fy, cz), fireCost))) continue;
            if (headClear(view, cx, fy, cz, clearance)) return true;
        }
        return false;
    }

    // Cells [fy+2 .. fy+clearance-1] above the feet must be clear for a unit taller than 2 cells (a 2-tall
    // unit's head at fy+1 is already covered by the feet cell being occupiable).
    private static boolean headClear(WalkabilityView view, int cx, int fy, int cz, int clearance) {
        for (int k = 2; k < clearance; k++)
            if (view.solidAt(cx, fy + k, cz)) return false;
        return true;
    }

    // A diagonal XZ move is only legal if both flanking orthogonal cells are passable (no corner cutting).
    public static boolean diagonalBlocked(WalkabilityView view, MobilityClass mob, int fromX, int toY, int fromZ, int dx, int dz) {
        if (dx == 0 || dz == 0) return false;
        if (Float.isInfinite(mob.costFor(view.kindAt(fromX + dx, toY, fromZ)))) return true;
        if (Float.isInfinite(mob.costFor(view.kindAt(fromX, toY, fromZ + dz)))) return true;
        return false;
    }
}
