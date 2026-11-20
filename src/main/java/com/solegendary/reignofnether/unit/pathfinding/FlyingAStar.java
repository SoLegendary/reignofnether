package com.solegendary.reignofnether.unit.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

// 3D A* for flying units (MobilityClass.FLYING), dispatched from GridAStar.search. Kept apart from the ground
// search on purpose: a flyer needs no floor, so terrain kinds / wideFits / fall passes / climbing don't apply -
// a cell is simply passable if the unit's whole body box fits there without touching a solid cell.
public final class FlyingAStar {
    private FlyingAStar() {}

    private static final float SQRT2 = GridNeighbors.SQRT2;
    private static final float SQRT3 = 1.7320508f;
    // extra cost on any step that changes altitude, so level flight wins ties over needless climbing/diving
    private static final float VERTICAL_STEP_COST = 0.25f;
    // how far up/down the goal's column we look for a free cell if the goal itself is inside terrain (eg. a canopy)
    private static final int GOAL_SNAP_RANGE = 12;

    // maxY is the flyingMaxYLevel gamerule: the highest cell the unit's feet may be put in. It only bounds the
    // cells searched (the body box can still reach above it, and solids up there still block it). A unit already
    // above it keeps its own altitude as the ceiling, so it can still move sideways and descend, never climb.
    public static GridAStar.Result search(WalkabilityView view, BlockPos start, BlockPos goal, int reach, int maxRadius, int maxNodes, int maxY) {
        Body body = new Body(view);
        int sx = start.getX(), sy = start.getY(), sz = start.getZ();
        int yMax = Math.max(sy, Math.min(maxY, Math.max(sy, goal.getY()) + PathfinderConfig.FLYING_Y_ABOVE));
        goal = snapGoal(body, goal, yMax);
        int gx = goal.getX(),  gy = goal.getY(),  gz = goal.getZ();
        int yMin = Math.min(sy, gy) - PathfinderConfig.FLYING_Y_BELOW;
        int goalReach = Math.max(1, reach);
        int radiusSq = maxRadius * maxRadius;

        Long2ObjectOpenHashMap<NodeRec> all = new Long2ObjectOpenHashMap<>(1024);
        // ties on f go to the node closer to the goal, else open sky floods a whole plateau of equal-f cells
        PriorityQueue<NodeRec> open = new PriorityQueue<>(256, (a, b) -> {
            int c = Float.compare(a.f, b.f);
            return c != 0 ? c : Float.compare(a.h, b.h);
        });

        NodeRec startRec = new NodeRec(sx, sy, sz, 0f, heuristic(gx - sx, gy - sy, gz - sz), null);
        all.put(key(sx, sy, sz), startRec);
        open.add(startRec);

        int expanded = 0;
        NodeRec best = startRec;
        while (!open.isEmpty() && expanded < maxNodes) {
            NodeRec cur = open.poll();
            if (cur.closed) continue;
            cur.closed = true;
            expanded++;

            if (Math.max(Math.abs(cur.x - gx), Math.max(Math.abs(cur.y - gy), Math.abs(cur.z - gz))) <= goalReach)
                return new GridAStar.Result(reconstruct(cur), true, expanded);
            // best-effort fallback for an unreachable goal: the closed node nearest the goal
            if (cur.h < best.h) best = cur;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        int nx = cur.x + dx, ny = cur.y + dy, nz = cur.z + dz;
                        if (ny < yMin || ny > yMax) continue;
                        int ddx = nx - sx, ddz = nz - sz;
                        if (ddx * ddx + ddz * ddz > radiusSq) continue;
                        if (!body.fits(nx, ny, nz)) continue;
                        if (!cornerClear(body, cur.x, cur.y, cur.z, dx, dy, dz)) continue;
                        relax(all, open, nx, ny, nz, cur.g + stepCost(dx, dy, dz), gx, gy, gz, cur);
                    }
                }
            }
        }
        return new GridAStar.Result(reconstruct(best), false, expanded);
    }

    // If the goal cell is inside terrain, aim for the nearest free cell in its column (up first, flyers prefer
    // altitude, but never above yMax) so the search has something reachable to finish on instead of flooding to the
    // node cap.
    private static BlockPos snapGoal(Body body, BlockPos goal, int yMax) {
        if (body.fits(goal.getX(), goal.getY(), goal.getZ())) return goal;
        for (int d = 1; d <= GOAL_SNAP_RANGE; d++) {
            if (goal.getY() + d <= yMax && body.fits(goal.getX(), goal.getY() + d, goal.getZ())) return goal.above(d);
            if (body.fits(goal.getX(), goal.getY() - d, goal.getZ())) return goal.below(d);
        }
        return goal;
    }

    // A diagonal move sweeps through the cells it passes beside; each axis step (and for a 3-axis move, each
    // axis pair) must also be free or the body would clip the corner. The destination itself is checked by the caller.
    private static boolean cornerClear(Body body, int x, int y, int z, int dx, int dy, int dz) {
        int axes = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
        if (axes < 2) return true;
        if (dx != 0 && !body.fits(x + dx, y, z)) return false;
        if (dy != 0 && !body.fits(x, y + dy, z)) return false;
        if (dz != 0 && !body.fits(x, y, z + dz)) return false;
        if (axes == 3) {
            if (!body.fits(x + dx, y + dy, z)) return false;
            if (!body.fits(x + dx, y, z + dz)) return false;
            if (!body.fits(x, y + dy, z + dz)) return false;
        }
        return true;
    }

    private static float stepCost(int dx, int dy, int dz) {
        int axes = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
        float c = axes == 1 ? 1f : axes == 2 ? SQRT2 : SQRT3;
        return dy != 0 ? c + VERTICAL_STEP_COST : c;
    }

    // exact cost of free-space flight in a 3D grid (octile generalised to 3 axes): never overestimates stepCost
    private static float heuristic(int dx, int dy, int dz) {
        int a = Math.abs(dx), b = Math.abs(dy), c = Math.abs(dz);
        int max = Math.max(a, Math.max(b, c));
        int min = Math.min(a, Math.min(b, c));
        int mid = a + b + c - max - min;
        return (max - mid) + SQRT2 * (mid - min) + SQRT3 * min;
    }

    private static long key(int x, int y, int z) {
        return (((long) (x & 0x1FFFFFF)) << 39)
             | (((long) (z & 0x1FFFFFF)) << 14)
             | ((y + 2048) & 0x3FFF);
    }

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

    private static List<BlockPos> reconstruct(NodeRec end) {
        ArrayList<BlockPos> out = new ArrayList<>();
        for (NodeRec n = end; n != null; n = n.parent)
            out.add(new BlockPos(n.x, n.y, n.z));
        Collections.reverse(out);
        return out;
    }

    // Whether the unit's body box, centred on a cell and rising from its floor, is clear of solid cells. The box
    // is (2*radius+1) wide and clearanceCells tall (see FlyingPathfinder). Each cell is tested once per search -
    // a big body (ghast) would otherwise rescan the same cube from up to 26 neighbours.
    private static final class Body {
        private final WalkabilityView view;
        private final int radius;
        private final int height;
        private final Long2ByteOpenHashMap memo = new Long2ByteOpenHashMap(1024); // 0 unknown, 1 fits, -1 blocked

        Body(WalkabilityView view) {
            this.view = view;
            this.radius = view.footprintRadius();
            this.height = Math.max(1, view.clearanceCells());
        }

        boolean fits(int x, int y, int z) {
            long k = key(x, y, z);
            byte known = memo.get(k);
            if (known != 0) return known > 0;
            boolean clear = isClear(x, y, z);
            memo.put(k, clear ? (byte) 1 : (byte) -1);
            return clear;
        }

        private boolean isClear(int x, int y, int z) {
            for (int cy = 0; cy < height; cy++)
                for (int cx = -radius; cx <= radius; cx++)
                    for (int cz = -radius; cz <= radius; cz++)
                        if (view.solidAt(x + cx, y + cy, z + cz)) return false;
            return true;
        }
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
