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

    // Wide-unit footprint test: a 2x2 body fits if the node cell plus at least ONE of the four 2x2 quadrants
    // touching it are standable. So a wide unit can hug a single wall or take a 2-wide gap, but a 1-wide pinch
    // (no quadrant clear on any side) is refused. This is deliberately permissive about being NEAR a wall - the
    // crowdingMalus is what keeps the unit off walls/edges/ceilings whenever there's open space to do so, instead
    // of a hard 3x3 clearance that refused everything. Each cell may sit one step above/below to straddle a stair.
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

    // Small A* cost that nudges a unit toward open space - vanilla's "avoid box" / border malus. wideFits already
    // guarantees the footprint fits; this only makes the search PREFER routes with room around the body, so it
    // bows off walls and low ceilings when there's space. It penalises ONLY actual WALLS (a solid block at body
    // height) and low ceilings - NOT drops/edges (a cliff beside the path isn't something the body bumps, so it
    // shouldn't read as "crowded"). Walls right beside the body (distance 1) count full; walls one cell further
    // (distance 2) count half - a gentler pull toward the middle of a wide-open area.
    public static final float SIDE_MALUS = 0.75f; // touching a wall (distance-1); distance-2 is half this
    public static final float CORNER_MALUS = 0.375f; // extra "slight" cost for an inside 90-deg corner (L of walls)

    // Reads one cell's body-blocking (solid) state. Lets crowdingMalus run either over a live WalkabilityView
    // (A* / debug) or directly over a chunk's local solid[] array at build time, from ONE definition.
    @FunctionalInterface
    public interface SolidProbe { boolean solid(int x, int y, int z); }

    // Adapter: the malus as A* and the debug overlay call it - over a live view, with the unit's clearance.
    public static float crowdingMalus(WalkabilityView view, int x, int y, int z) {
        return crowdingMalus(view::solidAt, view.clearanceCells(), x, y, z);
    }

    // The malus itself, over a solid-probe. crowdingMalus depends ONLY on surrounding solid cells + clearance,
    // so the same code feeds both the live A* path and the precomputed per-cell crowd[] cache (build time).
    public static float crowdingMalus(SolidProbe probe, int clearance, int x, int y, int z) {
        // HORIZONTAL cost is driven by the NEAREST wall, not a SUM over every wall. A cell touching a wall
        // (distance-1) pays full; a cell whose closest wall is one cell further (distance-2) pays half; a cell with
        // open space all around pays nothing. Summing every wall made the middle of a corridor - flanked by BOTH
        // walls at distance-2 - score higher than a cell hugging a single wall, the exact opposite of what we want.
        // Nearest-wall makes the search PREFER the most open lane: in a 3-wide corridor the middle (walls only at
        // distance-2 -> 0.125) beats the edges (a wall touching -> 0.25). The ceiling is handled SEPARATELY below:
        // folding it in here let a roofed tunnel saturate every cell to full malus, flattening the steering so the
        // middle could never win.
        float wall = 0f;
        // distance-1 ring (cells touching the body), cardinal AND diagonal - worst case, no need to look further
        distance1:
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (wallBeside(probe, x + dx, z + dz, y, clearance)) { wall = SIDE_MALUS; break distance1; }
            }
        // distance-2 ring - half, only counts if nothing was touching at distance-1
        if (wall == 0f) {
            distance2:
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) continue; // only the distance-2 ring
                    if (wallBeside(probe, x + dx, z + dz, y, clearance)) { wall = SIDE_MALUS * 0.5f; break distance2; }
                }
        }
        // Low ceiling right above the head - a separate, additive term. In a roofed tunnel this is uniform across
        // all cells so it cancels out in the A* comparison and the horizontal nearest-wall term still picks the
        // middle; where only some cells have a low ceiling it nudges the unit toward the headroom.
        float ceiling = probe.solid(x, y + clearance, z) ? SIDE_MALUS : 0f;
        // Inner-edge penalty: a +1 block (raised terrain / step-up, or the base of a wall) on two ADJACENT sides
        // makes this cell an inside corner the unit would 45-degree step into. Make it less valuable so paths
        // don't zig-zag diagonally up step corners.
        float corner = 0f;
        boolean rW = raisedBeside(probe, x - 1, z, y);
        boolean rE = raisedBeside(probe, x + 1, z, y);
        boolean rN = raisedBeside(probe, x, z - 1, y);
        boolean rS = raisedBeside(probe, x, z + 1, y);
        if (rN && rW) corner += CORNER_MALUS;
        if (rN && rE) corner += CORNER_MALUS;
        if (rS && rW) corner += CORNER_MALUS;
        if (rS && rE) corner += CORNER_MALUS;
        // No global cap: nearest-wall already prevents side walls from stacking, so the horizontal term is at most
        // one wall. Ceiling and corner are additive on top - a cell that's BOTH against a wall AND under a roof is
        // genuinely more cramped than one that's only against a wall, and should cost more.
        return wall + ceiling + corner;
    }

    // A wall here = a solid block anywhere ABOVE the feet, [y+1 .. y+clearance] (head level up to one block over
    // the head). We deliberately skip the FEET cell (y): a lone block at feet with air all the way above is a
    // climbable step ("block-air"), not a wall. But anything with a solid above feet counts as a wall even with a
    // 1-block air gap - a full 2-tall wall (y,y+1), an overhang ("air-block": air y, solid y+1), a wall lifted
    // off the ground (air y, solid y+1+), or a wall with a hole ("block-air-block": solid y, air y+1, solid y+2).
    private static boolean wallBeside(SolidProbe probe, int cx, int cz, int y, int clearance) {
        for (int k = 1; k <= clearance; k++)
            if (probe.solid(cx, y + k, cz)) return true;
        return false;
    }

    // A +1 block beside: a solid block at the unit's feet level (terrain raised one step up, or a wall's base).
    private static boolean raisedBeside(SolidProbe probe, int cx, int cz, int y) {
        return probe.solid(cx, y, cz);
    }

    // Cost of a single vertical climb step (up or down a wall). Higher than a normal step (1.0) and a step-up
    // (1.2) so a climber prefers walking/ground routes and only scales a wall when it's genuinely shorter.
    public static final float CLIMB_COST = 2.5f;

    // A climber may occupy this cell while clinging to a wall: the unit's own column (feet .. head) must be clear
    // of solid blocks. Unlike a ground node it needs NO floor support - it hangs off the adjacent wall instead.
    public static boolean climbColumnClear(WalkabilityView view, int x, int y, int z) {
        int clearance = view.clearanceCells();
        for (int k = 0; k < clearance; k++)
            if (view.solidAt(x, y + k, z)) return false;
        return true;
    }

    // A climb node needs a solid wall on at least one of the 4 horizontal sides over the unit's body height, so
    // the spider has something to press into (vanilla climbing triggers on any horizontalCollision). The grid
    // only exposes solidAt, which is exactly what the physics cares about, so any solid block counts as a wall.
    public static boolean adjacentToClimbWall(WalkabilityView view, int x, int y, int z) {
        return climbWallColumn(view, x + 1, y, z)
            || climbWallColumn(view, x - 1, y, z)
            || climbWallColumn(view, x, y, z + 1)
            || climbWallColumn(view, x, y, z - 1);
    }

    private static boolean climbWallColumn(WalkabilityView view, int x, int y, int z) {
        int clearance = view.clearanceCells();
        // From y-1 (one below the feet) up: the y-1 probe lets a cell just above a wall TOP count as a climb cell,
        // so a unit steps onto/off a cliff lip with a flat HORIZONTAL move instead of a diagonal one.
        for (int k = -1; k < clearance; k++)
            if (view.solidAt(x, y + k, z)) return true;
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
