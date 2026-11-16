package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.IntPredicate;

// small helpers about groups of units, shared by the managers
final class BotMath {

    private BotMath() {}

    static int popOf(LivingEntity e) {
        return e instanceof Unit u && u.getCost() != null ? Math.max(1, u.getCost().population) : 1;
    }

    static int popOf(Collection<LivingEntity> units) {
        int n = 0;
        for (LivingEntity e : units)
            n += popOf(e);
        return n;
    }

    // the middle of a group, snapped to the member nearest it: units are sent there, and a group split by a cliff
    // has its true middle in the air or the rock, where the pathfinder gives up early and everyone stops at the edge
    static BlockPos centroid(Collection<LivingEntity> units) {
        double x = 0, y = 0, z = 0;
        for (LivingEntity e : units) {
            x += e.getX();
            y += e.getY();
            z += e.getZ();
        }
        int n = Math.max(1, units.size());
        BlockPos mid = BlockPos.containing(x / n, y / n, z / n);
        return units.stream().map(LivingEntity::blockPosition).min(Comparator.comparingDouble(p -> p.distSqr(mid))).orElse(mid);
    }

    // at least half the group is within range of pos
    static boolean isNear(Collection<LivingEntity> units, BlockPos pos, int range) {
        int near = 0;
        for (LivingEntity e : units)
            if (e.blockPosition().distSqr(pos) < (long) range * range)
                near++;
        return near * 2 >= units.size();
    }

    // the fewest of n units (sorted best first) that pass ok, -1 when even all n don't. Adding a unit never makes a
    // group weaker, so a binary search: the combat sim runs ~log n times instead of once per unit added
    static int fewest(int n, IntPredicate ok) {
        if (!ok.test(n))
            return -1;
        int lo = 0, hi = n;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (ok.test(mid))
                hi = mid;
            else
                lo = mid + 1;
        }
        return lo;
    }

    // dist blocks from `from` towards `to` on the flat, or orElse when they're the same spot
    static BlockPos towards(BlockPos from, Vec3 to, double dist, BlockPos orElse) {
        Vec3 dir = to.subtract(Vec3.atCenterOf(from)).multiply(1, 0, 1);
        return dir.lengthSqr() < 1 ? orElse : BlockPos.containing(Vec3.atCenterOf(from).add(dir.normalize().scale(dist)));
    }

    static LivingEntity closest(LivingEntity from, Collection<LivingEntity> list) {
        return list.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(from))).orElse(null);
    }

    static int dist(BlockPos a, BlockPos b) {
        return (int) Math.sqrt(a.distSqr(b));
    }
}
