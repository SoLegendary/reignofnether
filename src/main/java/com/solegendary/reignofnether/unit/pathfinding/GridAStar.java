package com.solegendary.reignofnether.unit.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

// 3D A* over the shared GridNeighbors movement model.
public final class GridAStar {
    private static final float SQRT2 = GridNeighbors.SQRT2;

    private static final int[] DX = GridNeighbors.DX;
    private static final int[] DZ = GridNeighbors.DZ;
    private static final int[] DY = GridNeighbors.DY;

    // Climb moves for climber units (wall-cling cells only). Strictly straight: vertical up/down the same
    // column (the climb itself) plus the 4 cardinals to step onto/off a wall and traverse a face. No diagonals
    // (they'd zig-zag the climb). Stepping onto a cliff lip is a flat horizontal move since the lip counts as a
    // climb cell (wall one block below, see adjacentToClimbWall). All bypass the footprint/floor gates.
    private static final int[] CLIMB_DX = { 0,  0,   1, -1,  0,  0 };
    private static final int[] CLIMB_DY = { 1, -1,   0,  0,  0,  0 };
    private static final int[] CLIMB_DZ = { 0,  0,   0,  0,  1, -1 };

    public static final class Result {
        public final List<BlockPos> waypoints;
        public final boolean reached;
        public final int nodesExpanded;
        public Result(List<BlockPos> waypoints, boolean reached, int nodesExpanded) {
            this.waypoints = waypoints;
            this.reached = reached;
            this.nodesExpanded = nodesExpanded;
        }
    }

    private GridAStar() {}

    private static long key(int x, int y, int z) {
        return (((long) (x & 0x1FFFFFF)) << 39)
             | (((long) (z & 0x1FFFFFF)) << 14)
             | ((y + 2048) & 0x3FFF);
    }

    private static float heuristic(int dx, int dy, int dz) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        int min = Math.min(ax, az), max = Math.max(ax, az);
        return (max - min) + SQRT2 * min + Math.abs(dy);
    }

    public static Result search(WalkabilityView view, BlockPos start, BlockPos goal, int reach, int maxRadius, int maxNodes) {
        int sx = start.getX(), sy = start.getY(), sz = start.getZ();
        int gx = goal.getX(),  gy = goal.getY(),  gz = goal.getZ();

        Long2ObjectOpenHashMap<NodeRec> all = new Long2ObjectOpenHashMap<>(1024);
        PriorityQueue<NodeRec> open = new PriorityQueue<>(256, (a, b) -> Float.compare(a.f, b.f));

        NodeRec startRec = new NodeRec(sx, sy, sz, 0f, heuristic(gx - sx, gy - sy, gz - sz), null);
        all.put(key(sx, sy, sz), startRec);
        open.add(startRec);

        int reachSq = reach * reach;
        int radiusSq = maxRadius * maxRadius;
        int expanded = 0;
        NodeRec best = startRec;
        float bestH = startRec.h;

        MobilityClass mob = view.mobility();
        int clearance = view.clearanceCells();
        int footprintRadius = view.footprintRadius();
        float fireCost = view.fireCost();

        while (!open.isEmpty() && expanded < maxNodes) {
            NodeRec cur = open.poll();
            if (cur.closed) continue;
            cur.closed = true;
            expanded++;

            int dxg = cur.x - gx, dyg = cur.y - gy, dzg = cur.z - gz;
            if (dxg * dxg + dzg * dzg <= reachSq && Math.abs(dyg) <= 1) {
                return new Result(reconstruct(cur), true, expanded);
            }
            if (cur.h < bestH) { bestH = cur.h; best = cur; }

            for (int d = 0; d < GridNeighbors.COUNT; d++) {
                int nx = cur.x + DX[d];
                int ny = cur.y + DY[d];
                int nz = cur.z + DZ[d];

                int ddx = nx - sx, ddz = nz - sz;
                if (ddx * ddx + ddz * ddz > radiusSq) continue;

                byte kind = view.kindAt(nx, ny, nz);
                float costMult = mob.costFor(kind, fireCost);
                if (Float.isInfinite(costMult)) continue;

                if (footprintRadius > 0) {
                    // wide unit: its whole footprint box must be standable (no wall/corner/drop it overhangs) -
                    // vanilla's "keep clearance from block borders" rule. See wideFits.
                    if (!GridNeighbors.wideFits(view, nx, ny, nz)) continue;
                } else if (headBlocked(view, nx, ny, nz, clearance)) {
                    // 1-wide unit: only its own column needs headroom for its (possibly tall) height.
                    continue;
                }

                // stepping up also needs height clear above the origin so the body can rise into it
                // (else a tall mob can't climb a ledge under an overhang).
                if (DY[d] == 1 && riseBlocked(view, cur.x, cur.y, cur.z, clearance)) continue;

                if (GridNeighbors.diagonalBlocked(view, mob, cur.x, ny, cur.z, DX[d], DZ[d])) continue;

                float ng = cur.g + GridNeighbors.stepCost(d) * costMult;
                ng += view.crowdAt(nx, ny, nz); // steer all units off walls/edges/corners (precomputed per chunk)

                relax(all, open, nx, ny, nz, ng, gx, gy, gz, cur);
            }

            // Falling: drop more than one block where there's no standable cell one step down - a sheer 2+ block
            // descent like a tall staircase. The +-1 DY model can't express this, so a unit stalls at the lip (a
            // wide body spins on the ledge). Vanilla allows multi-block falls; mirror that. For each horizontal
            // dir, step off the edge into an open column and land on the first standable cell within
            // MAX_FALL_DROP. The 1-block drop is the DY=-1 neighbours above, so this only fires when the one-down
            // cell isn't a standable step.
            for (int d = 0; d < 8; d++) { // the 8 horizontal (DY==0) directions
                int nx = cur.x + DX[d];
                int nz = cur.z + DZ[d];
                int fddx = nx - sx, fddz = nz - sz;
                if (fddx * fddx + fddz * fddz > radiusSq) continue;

                // body must be able to step off the edge: target column clear over its full height, diagonal
                // doesn't cut a blocked corner.
                if (!GridNeighbors.climbColumnClear(view, nx, cur.y, nz)) continue;
                if (DX[d] != 0 && DZ[d] != 0
                        && GridNeighbors.diagonalBlocked(view, mob, cur.x, cur.y, cur.z, DX[d], DZ[d])) continue;

                // a real drop needs open air under the edge. solid (wall) or standable (a normal step-down the
                // DY=-1 neighbour already covers) means no fall here.
                if (view.solidAt(nx, cur.y - 1, nz)) continue;
                if (standable(view, mob, nx, cur.y - 1, nz, clearance, footprintRadius, fireCost)) continue;

                for (int fy = cur.y - 2; fy >= cur.y - PathfinderConfig.MAX_FALL_DROP; fy--) {
                    if (view.solidAt(nx, fy, nz)) break; // hit ground/obstacle before an open landing
                    float lmult = mob.costFor(view.kindAt(nx, fy, nz), fireCost);
                    if (Float.isInfinite(lmult)) continue; // still mid-air - keep falling
                    if (footprintRadius > 0 ? !GridNeighbors.wideFits(view, nx, fy, nz)
                                            : headBlocked(view, nx, fy, nz, clearance)) continue;
                    int drop = cur.y - fy;
                    float ng = cur.g + GridNeighbors.stepCost(d) * lmult
                             + PathfinderConfig.FALL_COST_PER_BLOCK * drop
                             + view.crowdAt(nx, fy, nz);
                    relax(all, open, nx, fy, nz, ng, gx, gy, gz, cur);
                    break;
                }
            }

            // Climber units (spiders with wall-climbing on) may also cling to a wall face. A climb cell hangs off
            // an adjacent wall with no floor and is the unit's own column, so it bypasses the costMult / wideFits
            // / headBlocked / riseBlocked / diagonal gates above (all of which assume floor + footprint box). The
            // CLIMB_* moves hug the wall both ways: enter from the ground, climb the face, step on/off a lip. The
            // main neighbour loop still runs every node, so the unit rejoins ground movement at top/bottom.
            if (view.canClimb()) {
                for (int c = 0; c < CLIMB_DX.length; c++) {
                    int nx = cur.x + CLIMB_DX[c];
                    int ny = cur.y + CLIMB_DY[c];
                    int nz = cur.z + CLIMB_DZ[c];

                    int cddx = nx - sx, cddz = nz - sz;
                    if (cddx * cddx + cddz * cddz > radiusSq) continue;

                    if (!GridNeighbors.climbColumnClear(view, nx, ny, nz)) continue;
                    if (!GridNeighbors.adjacentToClimbWall(view, nx, ny, nz)) continue;

                    relax(all, open, nx, ny, nz, cur.g + GridNeighbors.CLIMB_COST, gx, gy, gz, cur);
                }
            }
        }

        return new Result(reconstruct(best), false, expanded);
    }

    // Insert a freshly-reached node, or relax an existing open one if this path to it is cheaper.
    private static void relax(Long2ObjectOpenHashMap<NodeRec> all, PriorityQueue<NodeRec> open,
                              int nx, int ny, int nz, float ng, int gx, int gy, int gz, NodeRec cur) {
        long nkey = key(nx, ny, nz);
        NodeRec nb = all.get(nkey);
        if (nb == null) {
            nb = new NodeRec(nx, ny, nz, ng, heuristic(gx - nx, gy - ny, gz - nz), cur);
            all.put(nkey, nb);
            open.add(nb);
        } else if (!nb.closed && ng < nb.g) {
            nb.g = ng;
            nb.f = ng + nb.h;
            nb.parent = cur;
            open.add(nb);
        }
    }

    // Can a unit rest with its feet in this cell? Walkable terrain plus the footprint/headroom gate the main
    // neighbour loop uses, so the fall pass agrees with it on what counts as a landing.
    private static boolean standable(WalkabilityView view, MobilityClass mob, int x, int y, int z,
                                     int clearance, int footprintRadius, float fireCost) {
        if (Float.isInfinite(mob.costFor(view.kindAt(x, y, z), fireCost))) return false;
        return footprintRadius > 0 ? GridNeighbors.wideFits(view, x, y, z)
                                   : !headBlocked(view, x, y, z, clearance);
    }

    // Cells [y+2 .. y+clearance-1] above the destination feet must be clear for a mob taller than 2 cells.
    private static boolean headBlocked(WalkabilityView view, int x, int y, int z, int clearance) {
        for (int k = 2; k < clearance; k++)
            if (view.solidAt(x, y + k, z)) return true;
        return false;
    }

    // Cells [y+2 .. y+clearance] above the origin feet must be clear for the body to rise on a step-up.
    private static boolean riseBlocked(WalkabilityView view, int x, int y, int z, int clearance) {
        for (int k = 2; k <= clearance; k++)
            if (view.solidAt(x, y + k, z)) return true;
        return false;
    }

    private static List<BlockPos> reconstruct(NodeRec end) {
        ArrayList<BlockPos> out = new ArrayList<>();
        NodeRec n = end;
        while (n != null) {
            out.add(new BlockPos(n.x, n.y, n.z));
            n = n.parent;
        }
        Collections.reverse(out);
        return out;
    }

    private static final class NodeRec {
        final int x, y, z;
        float g, h, f;
        NodeRec parent;
        boolean closed;
        NodeRec(int x, int y, int z, float g, float h, NodeRec parent) {
            this.x = x; this.y = y; this.z = z;
            this.g = g; this.h = h; this.f = g + h;
            this.parent = parent;
        }
    }
}
