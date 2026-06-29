package com.solegendary.reignofnether.unit.pathfinding;

// Shared movement model for grid A* and flow fields, so they can't diverge.
// 20 neighbours: 8 XZ at same Y, 4 cardinal at Y+1 and Y-1, 4 diagonal at Y-1
// (diagonal descents let wide units take corner staircases without zig-zagging down one cell at a time).
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

    // Wide-unit footprint test: a 2x2 body fits if the node cell plus at least one touching 2x2 quadrant is
    // standable. So it can hug a wall or take a 2-wide gap, but a 1-wide pinch (no clear quadrant) is refused.
    // Deliberately permissive about being near a wall - crowdingMalus does the steering away from walls when
    // there's room, rather than a hard 3x3 clearance that refused everything. Cells may straddle a single stair step.
    public static boolean wideFits(WalkabilityView view, int x, int y, int z) {
        int r = view.footprintRadius();
        if (r <= 0) return true;
        MobilityClass mob = view.mobility();
        int clearance = view.clearanceCells();
        float fireCost = view.fireCost();
        if (!cellStandable(view, mob, x, z, y, clearance, fireCost)) return false;
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2)
                if (cellStandable(view, mob, x + sx, z, y, clearance, fireCost)
                 && cellStandable(view, mob, x, z + sz, y, clearance, fireCost)
                 && cellStandable(view, mob, x + sx, z + sz, y, clearance, fireCost))
                    return true;
        return false;
    }

    // A footprint cell is standable if the unit can rest in it within +-1 of the node's Y (to span a stair step)
    // with enough headroom for its full height.
    public static boolean cellStandable(WalkabilityView view, MobilityClass mob, int cx, int cz, int y, int clearance, float fireCost) {
        for (int fy = y - 1; fy <= y + 1; fy++) {
            if (Float.isInfinite(mob.costFor(view.kindAt(cx, fy, cz), fireCost))) continue;
            if (headClear(view, cx, fy, cz, clearance)) return true;
        }
        return false;
    }

    // Cells [fy+2 .. fy+clearance-1] must be clear for units taller than 2 cells (a 2-tall unit's head at fy+1
    // is already covered by the feet cell being occupiable).
    private static boolean headClear(WalkabilityView view, int cx, int fy, int cz, int clearance) {
        for (int k = 2; k < clearance; k++)
            if (view.solidAt(cx, fy + k, cz)) return false;
        return true;
    }

    // Small A* cost nudging units toward open space (vanilla's "avoid box" malus). wideFits already guarantees
    // the footprint fits; this only makes the search prefer routes with room around the body. Penalises walls
    // (solid at body height) and low ceilings only - not drops/edges, since a cliff beside the path isn't
    // something the body bumps. Distance-1 walls count full, distance-2 count half.
    public static final float SIDE_MALUS = 0.75f; // touching a wall (distance-1); distance-2 is half this
    public static final float CORNER_MALUS = 0.375f; // inside 90-deg corner (L of walls)

    // Reads one cell's solid state. Lets crowdingMalus run over either a live WalkabilityView (A*/debug) or a
    // chunk's local solid[] array at build time, from one definition.
    @FunctionalInterface
    public interface SolidProbe { boolean solid(int x, int y, int z); }

    // Malus over a live view (A* / debug overlay), using the unit's clearance.
    public static float crowdingMalus(WalkabilityView view, int x, int y, int z) {
        return crowdingMalus(view::solidAt, view.clearanceCells(), x, y, z);
    }

    // The malus itself, over a solid-probe. Depends only on surrounding solid cells + clearance, so the same
    // code feeds both the live A* path and the precomputed per-cell crowd[] cache.
    public static float crowdingMalus(SolidProbe probe, int clearance, int x, int y, int z) {
        // Horizontal cost is driven by the NEAREST wall, not a sum over all walls. Distance-1 pays full,
        // distance-2 pays half, open all around pays nothing. Summing made a corridor middle (flanked by two
        // distance-2 walls) score worse than hugging one wall - backwards. Nearest-wall makes the middle of a
        // 3-wide corridor (0.125) beat the edges (0.25). Ceiling is handled separately below: folding it in here
        // saturated every cell in a roofed tunnel, flattening the steering so the middle could never win.
        float wall = 0f;
        // distance-1 ring (cells touching the body), cardinal and diagonal
        distance1:
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (wallBeside(probe, x + dx, z + dz, y, clearance)) { wall = SIDE_MALUS; break distance1; }
            }
        // distance-2 ring - half, only if nothing touched at distance-1
        if (wall == 0f) {
            distance2:
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) continue; // only the distance-2 ring
                    if (wallBeside(probe, x + dx, z + dz, y, clearance)) { wall = SIDE_MALUS * 0.5f; break distance2; }
                }
        }
        // Low ceiling above the head - separate additive term. Uniform across a roofed tunnel so it cancels out
        // in A* comparisons; where only some cells have a low ceiling it nudges the unit toward the headroom.
        float ceiling = probe.solid(x, y + clearance, z) ? SIDE_MALUS : 0f;
        // Inner-edge penalty: a +1 block (step-up terrain, or a wall base) on two adjacent sides makes this an
        // inside corner the unit would 45-degree step into. Penalise it so paths don't zig-zag up step corners.
        float corner = 0f;
        boolean rW = raisedBeside(probe, x - 1, z, y);
        boolean rE = raisedBeside(probe, x + 1, z, y);
        boolean rN = raisedBeside(probe, x, z - 1, y);
        boolean rS = raisedBeside(probe, x, z + 1, y);
        if (rN && rW) corner += CORNER_MALUS;
        if (rN && rE) corner += CORNER_MALUS;
        if (rS && rW) corner += CORNER_MALUS;
        if (rS && rE) corner += CORNER_MALUS;
        // No global cap: nearest-wall already caps the horizontal term at one wall, and ceiling/corner add on top
        // - a cell both against a wall and under a roof is genuinely more cramped and should cost more.
        return wall + ceiling + corner;
    }

    // A wall = a solid block above the feet, [y+1 .. y+clearance]. We skip the feet cell (y) on purpose: a lone
    // block at feet with air above is a climbable step, not a wall. Anything solid above feet counts as a wall,
    // even across an air gap - full 2-tall wall, overhang, lifted wall, or a wall with a hole.
    private static boolean wallBeside(SolidProbe probe, int cx, int cz, int y, int clearance) {
        for (int k = 1; k <= clearance; k++)
            if (probe.solid(cx, y + k, cz)) return true;
        return false;
    }

    // A +1 block beside: solid at the unit's feet level (step-up terrain, or a wall base).
    private static boolean raisedBeside(SolidProbe probe, int cx, int cz, int y) {
        return probe.solid(cx, y, cz);
    }

    // Cost of one vertical climb step. Higher than a normal step (1.0) or step-up (1.2) so a climber prefers
    // ground routes and only scales a wall when it's genuinely shorter.
    public static final float CLIMB_COST = 2.5f;

    // A climber may occupy this cell while clinging: its own column (feet..head) must be clear of solids.
    // Unlike a ground node it needs no floor support - it hangs off the adjacent wall.
    public static boolean climbColumnClear(WalkabilityView view, int x, int y, int z) {
        int clearance = view.clearanceCells();
        for (int k = 0; k < clearance; k++)
            if (view.solidAt(x, y + k, z)) return false;
        return true;
    }

    // A climb node needs a solid wall on at least one of the 4 sides over the body height, so the spider has
    // something to press into (vanilla climbing triggers on any horizontalCollision). Any solid block counts.
    public static boolean adjacentToClimbWall(WalkabilityView view, int x, int y, int z) {
        return climbWallColumn(view, x + 1, y, z)
            || climbWallColumn(view, x - 1, y, z)
            || climbWallColumn(view, x, y, z + 1)
            || climbWallColumn(view, x, y, z - 1);
    }

    private static boolean climbWallColumn(WalkabilityView view, int x, int y, int z) {
        int clearance = view.clearanceCells();
        // From y-1 up: the y-1 probe lets a cell just above a wall top count as a climb cell, so a unit steps
        // onto/off a cliff lip with a flat horizontal move instead of a diagonal one.
        for (int k = -1; k < clearance; k++)
            if (view.solidAt(x, y + k, z)) return true;
        return false;
    }

    // A diagonal XZ move is legal only if both flanking orthogonal cells are passable (no corner cutting).
    public static boolean diagonalBlocked(WalkabilityView view, MobilityClass mob, int fromX, int toY, int fromZ, int dx, int dz) {
        if (dx == 0 || dz == 0) return false;
        if (Float.isInfinite(mob.costFor(view.kindAt(fromX + dx, toY, fromZ)))) return true;
        if (Float.isInfinite(mob.costFor(view.kindAt(fromX, toY, fromZ + dz)))) return true;
        return false;
    }
}
