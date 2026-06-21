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

            for (int d = 0; d < 16; d++) {
                int nx = cur.x + DX[d];
                int ny = cur.y + DY[d];
                int nz = cur.z + DZ[d];

                int ddx = nx - sx, ddz = nz - sz;
                if (ddx * ddx + ddz * ddz > radiusSq) continue;

                byte kind = view.kindAt(nx, ny, nz);
                float costMult = mob.costFor(kind);
                if (Float.isInfinite(costMult)) continue;
                if (GridNeighbors.footprintBlocked(view, nx, ny, nz, mob.footprintRadius())) continue;

                // Stepping up needs air above the unit's head at the origin (a 2-tall unit climbing onto a
                // 1-block ledge would smash its head into a ceiling otherwise — it can step, not jump).
                if (DY[d] == 1 && view.solidAt(cur.x, cur.y + 2, cur.z)) continue;

                if (GridNeighbors.diagonalBlocked(view, mob, cur.x, ny, cur.z, DX[d], DZ[d])) continue;

                float ng = cur.g + GridNeighbors.stepCost(d) * costMult;

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

        return new Result(reconstruct(best), false, expanded);
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
