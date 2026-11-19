package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

// Facade for flying units, the counterpart of RtsPathfinder for MobilityClass.FLYING. It reuses the shared worker
// pool / chunk warming / ChunkSnapshot, and only skips what is ground-specific: snapping the start and goal onto
// standable cells, and the footprint / fire / climb handling. The search itself is FlyingAStar.
public final class FlyingPathfinder {
    private FlyingPathfinder() {}

    // Body box in whole cells, in the units the shared WalkabilityView exposes: clearanceCells is the height,
    // footprintRadius is the cell radius around the node cell. Rounded up so the search never fits a body through
    // a gap it can't physically pass.
    //
    // Small flyers (bat, bee, windcaller: under a block wide, under 2 tall) are fractions of a cell, so rounding up
    // already leaves them slack. A big body like the ghast (3x3x3) would fill its cells exactly, and any drift while
    // following the path (momentum, waypoint tolerance) then clips a block - so bodies wider than a block or taller
    // than 2 get half a block of margin on every side.
    private static final float MARGIN_ABOVE_WIDTH = 1f;
    private static final float MARGIN_ABOVE_HEIGHT = 2f;

    public static int clearanceCellsFor(float height) {
        if (height > MARGIN_ABOVE_HEIGHT)
            return Mth.ceil(height + 1f);
        return Math.max(1, Mth.ceil(height));
    }

    public static int footprintRadiusFor(float width) {
        if (width > MARGIN_ABOVE_WIDTH)
            return Mth.ceil(width / 2f);
        return 0;
    }

    public static void requestPath(Mob mob, BlockPos target, int reach, BooleanSupplier valid, PathCallback onReady) {
        Level level = mob.level();
        BlockPos start = mob.blockPosition();
        if (isUnloaded(level, start) || isUnloaded(level, target)) {
            onReady.onPath(null, false);
            return;
        }
        // read here on the main thread: the worker has no Level to look the gamerule up on
        int maxY = level.getGameRules().getRule(GameRuleRegistrar.FLYING_MAX_Y_LEVEL).get();
        // single async path, same queue/pool as ground units. fire / climbing don't apply to a flyer.
        PathfinderWorkerPool.submit(level, start, target, reach, MobilityClass.FLYING, clearanceCellsFor(mob.getBbHeight()),
                footprintRadiusFor(mob.getBbWidth()), 1.0f, false, PathfinderConfig.MAX_NODES_FLYING, maxY, valid, onReady);
    }

    // Plain vanilla Path over the A* cells, aimed at the body's centre in each (see FlyingPath).
    public static Path toMcPath(List<BlockPos> waypoints, BlockPos target, boolean reached) {
        ArrayList<Node> nodes = new ArrayList<>(waypoints.size());
        for (BlockPos bp : waypoints) {
            Node n = new Node(bp.getX(), bp.getY(), bp.getZ());
            n.type = BlockPathTypes.WALKABLE;
            n.costMalus = 0f;
            nodes.add(n);
        }
        if (nodes.isEmpty()) {
            Node fallback = new Node(target.getX(), target.getY(), target.getZ());
            fallback.type = BlockPathTypes.WALKABLE;
            nodes.add(fallback);
        }
        return new FlyingPath(nodes, target, reached);
    }

    private static boolean isUnloaded(Level level, BlockPos bp) {
        if (level instanceof ServerLevel sl) {
            ChunkPos cp = new ChunkPos(bp);
            return sl.getChunkSource().getChunkNow(cp.x, cp.z) == null;
        }
        return false;
    }

    // Vanilla's getEntityPosAtNode offsets by the entity's width as if the node were a corner of its footprint.
    // Our nodes are the centre of the body box FlyingAStar cleared, so aim at the cell centre horizontally and
    // sit the body in the middle of the cleared height (the box is whole cells tall, the body usually isn't).
    private static final class FlyingPath extends Path {
        FlyingPath(List<Node> nodes, BlockPos target, boolean reached) {
            super(nodes, target, reached);
        }

        @Override
        public Vec3 getEntityPosAtNode(Entity entity, int index) {
            Node n = this.nodes.get(index);
            float height = entity.getBbHeight();
            return new Vec3(n.x + 0.5, n.y + (clearanceCellsFor(height) - height) / 2.0, n.z + 0.5);
        }
    }
}
