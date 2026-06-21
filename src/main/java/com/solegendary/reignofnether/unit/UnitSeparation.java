package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.unit.goals.MoveToTargetBlockGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.pathfinding.PathfinderConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

// Lightweight per-tick separation steering: crowding, same-owner, currently-moving ground units get a
// soft XZ nudge away from each other so a marching group fans out instead of stacking on one block.
// Additive to delta movement only (vanilla collision still resolves hard overlaps); disabled near the
// destination so settled/formed-up units don't jitter. Capped per tick with a round-robin cursor.
public final class UnitSeparation {
    private UnitSeparation() {}

    private static int startIndex = 0;
    private static volatile int lastProcessed = 0;

    public static int lastProcessed() { return lastProcessed; }

    public static void applySeparation(List<LivingEntity> allUnits) {
        int cellSize = PathfinderConfig.SEPARATION_CELL_SIZE;
        double radius = PathfinderConfig.SEPARATION_RADIUS;
        double radiusSq = radius * radius;
        double strength = PathfinderConfig.SEPARATION_STRENGTH;
        final double settleSq = PathfinderConfig.ARRIVAL_SETTLE_SQ; // near the target: let it settle, no push

        List<LivingEntity> moving = new ArrayList<>();
        Long2ObjectOpenHashMap<List<LivingEntity>> buckets = new Long2ObjectOpenHashMap<>();
        for (LivingEntity e : allUnits) {
            if (!(e instanceof Unit u) || u.isFlyingUnit()) continue;
            MoveToTargetBlockGoal g = u.getMoveGoal();
            if (g == null || g.getMoveTarget() == null) continue;
            if (e instanceof Mob m && m.getNavigation().isDone()) continue;
            if (e.distanceToSqr(g.getMoveTarget().getCenter()) < settleSq) continue;
            moving.add(e);
            long key = cellKey((int) Math.floor(e.getX()), (int) Math.floor(e.getZ()), cellSize);
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }
        if (moving.isEmpty()) { startIndex = 0; lastProcessed = 0; return; }

        int count = Math.min(moving.size(), PathfinderConfig.SEPARATION_MAX_PER_TICK);
        for (int i = 0; i < count; i++) {
            LivingEntity e = moving.get((startIndex + i) % moving.size());
            applyOne(e, buckets, cellSize, radius, radiusSq, strength);
        }
        startIndex = (startIndex + count) % moving.size();
        lastProcessed = count;
    }

    private static void applyOne(LivingEntity e, Long2ObjectOpenHashMap<List<LivingEntity>> buckets,
                                 int cellSize, double radius, double radiusSq, double strength) {
        double px = e.getX(), pz = e.getZ();
        String owner = ((Unit) e).getOwnerName();
        int cx = (int) Math.floor(px), cz = (int) Math.floor(pz);
        double fx = 0, fz = 0;
        for (int dcx = -1; dcx <= 1; dcx++) {
            for (int dcz = -1; dcz <= 1; dcz++) {
                List<LivingEntity> bucket = buckets.get(cellKey(cx + dcx * cellSize, cz + dcz * cellSize, cellSize));
                if (bucket == null) continue;
                for (LivingEntity other : bucket) {
                    if (other == e || !owner.equals(((Unit) other).getOwnerName())) continue;
                    double dx = px - other.getX(), dz = pz - other.getZ();
                    double dsq = dx * dx + dz * dz;
                    if (dsq >= radiusSq) continue;
                    if (dsq < 1.0e-4) { // exactly overlapping: deterministic tiny nudge by id
                        double s = ((e.getId() - other.getId()) & 1) == 0 ? strength : -strength;
                        fx += s; fz += s;
                        continue;
                    }
                    double d = Math.sqrt(dsq);
                    double w = strength * (1.0 - d / radius);
                    fx += (dx / d) * w;
                    fz += (dz / d) * w;
                }
            }
        }
        if (fx != 0 || fz != 0) {
            Vec3 dm = e.getDeltaMovement();
            e.setDeltaMovement(dm.x + fx, dm.y, dm.z + fz);
        }
    }

    private static long cellKey(int x, int z, int cellSize) {
        long gx = Math.floorDiv(x, cellSize);
        long gz = Math.floorDiv(z, cellSize);
        return (gx << 32) ^ (gz & 0xffffffffL);
    }
}
