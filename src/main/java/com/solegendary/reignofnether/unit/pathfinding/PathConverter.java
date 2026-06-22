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
    // shares its XZ with the node below it, so without this bias the unit would be told to go straight up with no
    // horizontal push - and a spider only physically climbs while pressing into a wall (vanilla gives +0.2 Y only
    // when horizontalCollision && onClimbable()). 0.55 lands the target just inside the wall cell, guaranteeing a
    // horizontal collision so the climb engages, without being so far it shoves the body through a diagonal gap.
    private static final double WALL_PRESS = 0.55;

    // Going DOWN, do the opposite: nudge the unit slightly OFF the wall. A spider pressing a wall is forced
    // upward (+0.2/tick), so it can't descend while touching one. Pushed clear, its wide body no longer collides
    // with the wall, so it simply free-falls straight down the column (no fall damage) and lands at the bottom.
    private static final double WALL_PUSH_OFF = 0.5;

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

        // A climb node shares XZ with the previous node and differs by one in Y (a vertical wall move). A node is
        // "climb-aimed" when it is itself a climb node OR its SUCCESSOR is a vertical climb node (up or down), so
        // the unit is already pressing the wall on the transition cell before it rises or steps off the lip to
        // descend. For those we aim the follow target into the wall (see getEntityPosAtNode) and force WALKABLE so
        // navigation doesn't refuse the mid-air cell.
        boolean[] climbAim = new boolean[nodes.size()];
        boolean[] descending = new boolean[nodes.size()];
        Vec3[] wallDir = new Vec3[nodes.size()];
        for (int i = 0; view != null && i < nodes.size(); i++) {
            int dir = climbDir(nodes, i); // +1 climbing up, -1 descending, 0 not a vertical wall move here
            if (dir == 0) continue;
            Node n = nodes.get(i);
            Vec3 wd = wallDirAt(view, n.x, n.y, n.z);
            if (wd == Vec3.ZERO) continue; // no wall found (shouldn't happen for a real climb node)
            climbAim[i] = true;
            descending[i] = dir < 0;
            wallDir[i] = wd;
            n.type = BlockPathTypes.WALKABLE;
        }

        return new CenteredPath(nodes, target, reached, climbAim, descending, wallDir);
    }

    // Vertical direction of the wall move at node i: +1 going up, -1 going down, 0 if not a vertical climb step.
    // A node counts if it is itself a vertical climb step (vs its predecessor) or its successor is one.
    private static int climbDir(List<Node> nodes, int i) {
        if (isClimbNode(nodes, i)) return Integer.signum(nodes.get(i).y - nodes.get(i - 1).y);
        if (isClimbNode(nodes, i + 1)) return Integer.signum(nodes.get(i + 1).y - nodes.get(i).y);
        return 0;
    }

    // Node i is a climb node: same XZ as node i-1, one block apart in Y.
    private static boolean isClimbNode(List<Node> nodes, int index) {
        if (index <= 0 || index >= nodes.size()) return false;
        Node a = nodes.get(index - 1), b = nodes.get(index);
        return a.x == b.x && a.z == b.z && Math.abs(b.y - a.y) == 1;
    }

    // First horizontal direction whose column is solid over the unit's body height - the wall to cling to.
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

    // Vanilla's getEntityPosAtNode aims a wide unit at the +X/+Z CORNER of each cell (offset (int)(width+1)*0.5),
    // which physically drags its body into the wall/corner - even under a low overhang - no matter how clear the
    // path is. We aim every unit at the cell CENTRE so it walks down the middle of the 3x3-cleared path. Following
    // is otherwise left loose (vanilla corner-cutting + reach tolerance) so units flow instead of freezing. Climb
    // nodes are the exception: they're nudged into the adjacent wall so the climbing physics engage (WALL_PRESS).
    private static final class CenteredPath extends Path {
        private final boolean[] climbAim;
        private final boolean[] descending;
        private final Vec3[] wallDir;

        CenteredPath(List<Node> nodes, BlockPos target, boolean reached, boolean[] climbAim, boolean[] descending, Vec3[] wallDir) {
            super(nodes, target, reached);
            this.climbAim = climbAim;
            this.descending = descending;
            this.wallDir = wallDir;
        }

        @Override
        public Vec3 getEntityPosAtNode(Entity entity, int index) {
            Node n = this.nodes.get(index);
            Vec3 base = new Vec3(n.x + 0.5, n.y, n.z + 0.5);
            if (index < climbAim.length && climbAim[index] && wallDir[index] != null) {
                Vec3 w = wallDir[index];
                // Up: press INTO the wall to climb. Down: push OFF it so the body clears the wall and free-falls.
                double press = descending[index] ? -WALL_PUSH_OFF : WALL_PRESS;
                return base.add(w.x * press, 0, w.z * press);
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
            default -> BlockPathTypes.WALKABLE;
        };
    }
}
