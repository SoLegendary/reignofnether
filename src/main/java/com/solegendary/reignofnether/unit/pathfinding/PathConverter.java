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
        return new CenteredPath(nodes, target, reached);
    }

    // Vanilla's getEntityPosAtNode aims a wide unit at the +X/+Z CORNER of each cell (offset (int)(width+1)*0.5),
    // which physically drags its body into the wall/corner - even under a low overhang - no matter how clear the
    // path is. We aim every unit at the cell CENTRE so it walks down the middle of the 3x3-cleared path. Following
    // is otherwise left loose (vanilla corner-cutting + reach tolerance) so units flow instead of freezing.
    private static final class CenteredPath extends Path {
        CenteredPath(List<Node> nodes, BlockPos target, boolean reached) { super(nodes, target, reached); }

        @Override
        public Vec3 getEntityPosAtNode(Entity entity, int index) {
            Node n = this.nodes.get(index);
            return new Vec3(n.x + 0.5, n.y, n.z + 0.5);
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
