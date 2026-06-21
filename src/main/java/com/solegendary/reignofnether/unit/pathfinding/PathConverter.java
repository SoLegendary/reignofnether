package com.solegendary.reignofnether.unit.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

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
        return new Path(nodes, target, reached);
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
