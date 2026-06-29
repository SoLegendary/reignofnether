package com.solegendary.reignofnether.unit.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class PathConverter {
    private PathConverter() {}

    // How far past the cell centre to aim a climbing unit, into the adjacent wall. A straight-up climb waypoint
    // shares its XZ with the node below, so without this bias there's no horizontal push - and a spider only
    // climbs while pressing a wall (vanilla gives +0.2 Y only on horizontalCollision && onClimbable). 0.55 lands
    // the target just inside the wall cell, forcing the collision, without shoving the body through a diagonal gap.
    private static final double WALL_PRESS = 0.55;

    // Going down, the opposite: nudge slightly off the wall. A spider pressing a wall is forced upward (+0.2/tick)
    // and can't descend; pushed clear, its body no longer collides so it free-falls straight down (no fall damage).
    private static final double WALL_PUSH_OFF = 0.5;

    // Extra push past the lip at a descent node, on top of the body's half-width. A wide body stays "on ground"
    // while any of its base overhangs the lip, so quadrant-centring alone leaves it perched in a deadlock (won't
    // fall until it moves forward, won't move forward until it falls). The target is pushed forward by bbWidth/2
    // (to carry the trailing edge past the lip) plus this margin so it fully clears support and commits to the
    // fall. Width-scaled per-entity in getEntityPosAtNode; separate from and added on top of the quadrant offset.
    private static final double DROP_CLEAR_MARGIN = 0.5;

    public static Path toMcPath(List<BlockPos> waypoints, BlockPos target, boolean reached, WalkabilityView view) {
        ArrayList<Node> nodes = new ArrayList<>(waypoints.size());
        for (BlockPos bp : waypoints) {
            Node n = new Node(bp.getX(), bp.getY(), bp.getZ());
            n.type = typeFor(view, bp);
            n.costMalus = 0f;
            nodes.add(n);
        }
        if (nodes.isEmpty()) {
            Node fallback = new Node(target.getX(), target.getY(), target.getZ());
            fallback.type = BlockPathTypes.WALKABLE;
            nodes.add(fallback);
        }

        // A climb node shares XZ with its predecessor and differs by 1 in Y (a vertical wall move). A node is
        // "climb-aimed" when it is a climb node or its successor is one, so the unit is already pressing the wall
        // on the transition cell before it rises or steps off the lip. For those we aim the target into the wall
        // (see getEntityPosAtNode) and force WALKABLE so navigation doesn't refuse the mid-air cell.
        boolean[] climbAim = new boolean[nodes.size()];
        boolean[] descending = new boolean[nodes.size()];
        Vec3[] wallDir = new Vec3[nodes.size()];
        for (int i = 0; view != null && i < nodes.size(); i++) {
            int dir = climbDir(nodes, i); // +1 up, -1 down, 0 not a vertical wall move
            if (dir == 0) continue;
            Node n = nodes.get(i);
            Vec3 wd = wallDirAt(view, n.x, n.y, n.z);
            if (wd == Vec3.ZERO) continue; // no wall (shouldn't happen for a real climb node)
            climbAim[i] = true;
            descending[i] = dir < 0;
            wallDir[i] = wd;
            n.type = BlockPathTypes.WALKABLE;
        }

        // Per-node body position for WIDE units. Our A* node is a single cell, but a 2-wide body occupies the node
        // cell plus one clear quadrant (see GridNeighbors.wideFits). Aiming the follower at the cell centre puts
        // the body half-on every neighbour, so a lone obstacle beside the path (tree, 1-block wall) clips it and it
        // jams. Instead we offset toward the clear quadrant the pathfinder found, so the body squeezes past on the
        // open side. Fully-open cells stay centred (no jitter mid-corridor).
        Vec3[] footOffset = new Vec3[nodes.size()];
        Vec3[] dropDir = new Vec3[nodes.size()];
        for (int i = 0; view != null && view.footprintRadius() > 0 && i < nodes.size(); i++) {
            if (climbAim[i]) continue; // climb nodes do their own wall press/push-off
            Node n = nodes.get(i);
            // Travel direction through this node: breaks ties between equally-clear quadrants and pushes off a ledge.
            Node prev = nodes.get(Math.max(0, i - 1)), next = nodes.get(Math.min(nodes.size() - 1, i + 1));
            footOffset[i] = footprintOffset(view, n, next.x - prev.x, next.z - prev.z);
            // Descent node: remember the normalised travel direction so the follower can shove the target past the
            // lip by a width-scaled amount (see getEntityPosAtNode) and a perched wide body commits to the drop.
            if (i > 0 && n.y < prev.y) {
                double dx = n.x - prev.x, dz = n.z - prev.z, len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0) dropDir[i] = new Vec3(dx / len, 0, dz / len);
            }
        }

        return new CenteredPath(nodes, target, reached, climbAim, descending, wallDir, footOffset, dropDir);
    }

    // Vertical direction of the wall move at node i: +1 up, -1 down, 0 if not a climb step. Counts if node i is
    // a climb step (vs predecessor) or its successor is.
    private static int climbDir(List<Node> nodes, int i) {
        if (isClimbNode(nodes, i)) return Integer.signum(nodes.get(i).y - nodes.get(i - 1).y);
        if (isClimbNode(nodes, i + 1)) return Integer.signum(nodes.get(i + 1).y - nodes.get(i).y);
        return 0;
    }

    // Climb node: same XZ as node i-1, one block apart in Y.
    private static boolean isClimbNode(List<Node> nodes, int index) {
        if (index <= 0 || index >= nodes.size()) return false;
        Node a = nodes.get(index - 1), b = nodes.get(index);
        return a.x == b.x && a.z == b.z && Math.abs(b.y - a.y) == 1;
    }

    // Where a wide body's centre should sit: offset toward a clear quadrant (node cell + that quadrant's 3 cells
    // all standable, matching wideFits), putting the centre on their shared corner (+-0.5 per axis). All 4
    // quadrants clear -> stay centred (walk down the middle); otherwise pick the quadrant best aligned with travel
    // so the choice is stable node-to-node and the body leads where it's going.
    private static Vec3 footprintOffset(WalkabilityView view, Node n, double travelX, double travelZ) {
        MobilityClass mob = view.mobility();
        int clearance = view.clearanceCells();
        float fireCost = view.fireCost();
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int clearCount = 0;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                if (!(GridNeighbors.cellStandable(view, mob, n.x + sx, n.z, n.y, clearance, fireCost)
                   && GridNeighbors.cellStandable(view, mob, n.x, n.z + sz, n.y, clearance, fireCost)
                   && GridNeighbors.cellStandable(view, mob, n.x + sx, n.z + sz, n.y, clearance, fireCost)))
                    continue;
                clearCount++;
                double score = sx * travelX + sz * travelZ; // prefer the quadrant we're heading toward
                if (score > bestScore) { bestScore = score; best = new Vec3(sx * 0.5, 0, sz * 0.5); }
            }
        }
        if (best == null || clearCount == 4) return Vec3.ZERO; // boxed-in, or fully open -> stay centred
        return best;
    }

    // First horizontal direction whose column is solid over the body height - the wall to cling to.
    private static Vec3 wallDirAt(WalkabilityView view, int x, int y, int z) {
        if (columnSolid(view, x + 1, y, z)) return new Vec3(1, 0, 0);
        if (columnSolid(view, x - 1, y, z)) return new Vec3(-1, 0, 0);
        if (columnSolid(view, x, y, z + 1)) return new Vec3(0, 0, 1);
        if (columnSolid(view, x, y, z - 1)) return new Vec3(0, 0, -1);
        return Vec3.ZERO;
    }

    private static boolean columnSolid(WalkabilityView view, int x, int y, int z) {
        int clearance = view.clearanceCells();
        // From y-1 up, matching GridNeighbors.climbWallColumn, so a lip cell just above a wall top still resolves
        // a wall direction (needed to push the unit off the wall when it steps off the edge to descend).
        for (int k = -1; k < clearance; k++)
            if (view.solidAt(x, y + k, z)) return true;
        return false;
    }

    // Vanilla's getEntityPosAtNode aims a wide unit at a fixed +X/+Z corner, which works because its node
    // evaluator places the node so that corner is the clear one - but on our centre-cell nodes it shoves the body
    // into whatever's on the +X/+Z side. So we override it: narrow units (and fully-open wide cells) aim at the
    // cell centre; wide units beside an obstacle aim at the clear quadrant the pathfinder found (footOffset).
    // Everything else (advance, corner-cutting, reach tolerance, stuck detection) is plain vanilla. Climb nodes
    // are the exception: nudged into the adjacent wall so climbing physics engage (WALL_PRESS).
    private static final class CenteredPath extends Path {
        private final boolean[] climbAim;
        private final boolean[] descending;
        private final Vec3[] wallDir;
        private final Vec3[] footOffset;
        private final Vec3[] dropDir;

        CenteredPath(List<Node> nodes, BlockPos target, boolean reached, boolean[] climbAim, boolean[] descending, Vec3[] wallDir, Vec3[] footOffset, Vec3[] dropDir) {
            super(nodes, target, reached);
            this.climbAim = climbAim;
            this.descending = descending;
            this.wallDir = wallDir;
            this.footOffset = footOffset;
            this.dropDir = dropDir;
        }

        @Override
        public Vec3 getEntityPosAtNode(Entity entity, int index) {
            Node n = this.nodes.get(index);
            Vec3 base = new Vec3(n.x + 0.5, n.y, n.z + 0.5);
            if (index < climbAim.length && climbAim[index] && wallDir[index] != null) {
                Vec3 w = wallDir[index];
                // Up: press into the wall to climb. Down: push off it so the body clears and free-falls.
                double press = descending[index] ? -WALL_PUSH_OFF : WALL_PRESS;
                return base.add(w.x * press, 0, w.z * press);
            }
            // Wide unit: sit the body in the clear quadrant so it squeezes past obstacles instead of clipping.
            if (entity.getBbWidth() > 1.0f) {
                if (index < footOffset.length && footOffset[index] != null)
                    base = base.add(footOffset[index]);
                // Descent: push the target past the lip by half-width (+ margin) so the perched body's trailing
                // edge clears its support and commits to the drop instead of stalling on the edge.
                if (index < dropDir.length && dropDir[index] != null) {
                    double nudge = entity.getBbWidth() / 2.0 + DROP_CLEAR_MARGIN;
                    base = base.add(dropDir[index].x * nudge, 0, dropDir[index].z * nudge);
                }
            }
            return base;
        }
    }

    private static BlockPathTypes typeFor(WalkabilityView view, BlockPos bp) {
        if (view == null) return BlockPathTypes.WALKABLE;
        byte kind = view.kindAt(bp.getX(), bp.getY(), bp.getZ());
        return switch (kind) {
            case WalkabilityBuilder.KIND_WATER -> BlockPathTypes.WATER;
            case WalkabilityBuilder.KIND_LAVA  -> BlockPathTypes.LAVA;
            case WalkabilityBuilder.KIND_FIRE  -> BlockPathTypes.DAMAGE_FIRE;
            case WalkabilityBuilder.KIND_SLIME  -> BlockPathTypes.STICKY_HONEY;
            default -> BlockPathTypes.WALKABLE;
        };
    }
}
