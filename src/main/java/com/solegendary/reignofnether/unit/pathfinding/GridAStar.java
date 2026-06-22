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

    // Climb moves for climber units (wall-cling cells only). STRICTLY straight: vertical up/down the SAME column
    // (the actual climb), plus the 4 cardinal horizontals to step onto/off a wall and traverse a face. NO
    // diagonals - those let the climb zig-zag. Stepping onto the cliff lip is a flat horizontal move because the
    // lip cell counts as a climb cell (a wall sits one block below it - see GridNeighbors.adjacentToClimbWall).
    // All bypass the footprint/floor gates (a cling cell has no floor).
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
                    // Wide unit: its full footprint box must be standable (no wall, corner or drop the body
                    // would overhang), the vanilla "keep clearance from block borders" rule. See wideFits.
                    if (!GridNeighbors.wideFits(view, nx, ny, nz)) continue;
                } else if (headBlocked(view, nx, ny, nz, clearance)) {
                    // 1-wide unit: only its own column needs headroom for its (possibly tall) height.
                    continue;
                }

                // Stepping up also needs the full height clear above the ORIGIN so the body can rise into it
                // (a tall mob can't climb a ledge under an overhang otherwise). For a 2-tall unit, one cell.
                if (DY[d] == 1 && riseBlocked(view, cur.x, cur.y, cur.z, clearance)) continue;

                if (GridNeighbors.diagonalBlocked(view, mob, cur.x, ny, cur.z, DX[d], DZ[d])) continue;

                float ng = cur.g + GridNeighbors.stepCost(d) * costMult;
                ng += GridNeighbors.crowdingMalus(view, nx, ny, nz); // avoid walls/edges/corners for ALL units

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

            // Climber units (spiders with wall-climbing on) may also cling to a wall face. A climb cell hangs off
            // an adjacent wall with no floor and occupies only the unit's own column, so it BYPASSES the costMult
            // / wideFits / headBlocked / riseBlocked / diagonal gates above (those all assume floor support and a
            // footprint box). The moves (see CLIMB_*) let the path hug the wall both ways: enter a column from the
            // ground, climb up/down the face, and step on/off a cliff lip. The main neighbour loop still runs on
            // every node, so a unit re-enters normal ground movement at the top or bottom automatically.
            if (view.canClimb()) {
                for (int c = 0; c < CLIMB_DX.length; c++) {
                    int nx = cur.x + CLIMB_DX[c];
                    int ny = cur.y + CLIMB_DY[c];
                    int nz = cur.z + CLIMB_DZ[c];

                    int cddx = nx - sx, cddz = nz - sz;
                    if (cddx * cddx + cddz * cddz > radiusSq) continue;

                    if (!GridNeighbors.climbColumnClear(view, nx, ny, nz)) continue;
                    if (!GridNeighbors.adjacentToClimbWall(view, nx, ny, nz)) continue;

                    float ng = cur.g + GridNeighbors.CLIMB_COST;
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
            }
        }

        return new Result(reconstruct(best), false, expanded);
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
