package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.pathfinding.MobilityClass;
import com.solegendary.reignofnether.unit.pathfinding.RtsPathfinder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Walking distances measured with the RTS pathfinder, the same one units follow when ordered. It answers a few
// ticks later, so the first ask for a spot returns null and the caller tries again on a later think.
public class BotWalk {

    public record Walk(boolean reached, double length, double straight) {}

    private static final long FORGET_TICKS = 2 * 60 * 20; // buildings and gathering change the ground

    private final Map<Object, Walk> known = new ConcurrentHashMap<>();
    private final Map<Object, Long> asked = new ConcurrentHashMap<>(); // -> tick asked
    private static final long ASK_TIMEOUT = 10 * 20; // a request dropped without an answer (the worker died) is asked again
    private long lastForget = 0;

    // ponytail: cached by target only, the start is whichever worker asked first; fine for "is it a long walk"
    @Nullable
    public Walk get(Mob from, BlockPos to, long tick) {
        return get(from, null, to, tick, to.asLong());
    }

    // the walk from one spot to another, with the given worker's size and mobility
    @Nullable
    public Walk get(Mob mob, BlockPos from, BlockPos to, long tick) {
        return get(mob, from, to, tick, from);
    }

    // same, cached under keyFrom instead of from: a walking worker's spot changes before the answer comes back
    @Nullable
    public Walk get(Mob mob, BlockPos from, BlockPos to, long tick, BlockPos keyFrom) {
        return get(mob, from, to, tick, List.of(keyFrom.asLong(), to.asLong()));
    }

    @Nullable
    private Walk get(Mob mob, @Nullable BlockPos from, BlockPos to, long tick, Object key) {
        if (tick - lastForget > FORGET_TICKS) {
            lastForget = tick;
            known.clear();
            asked.values().removeIf(at -> tick - at >= ASK_TIMEOUT); // never answered (the worker died)
        }
        Walk w = known.get(key);
        if (w != null)
            return w;
        Long askedAt = asked.get(key);
        if (askedAt != null && tick - askedAt < ASK_TIMEOUT)
            return null;
        asked.put(key, tick);
        BlockPos start = from != null ? from : mob.blockPosition();
        double straight = Math.sqrt(start.distSqr(to));
        RtsPathfinder.requestPath(mob, start, to, 2, MobilityClass.of((Unit) mob), mob::isAlive, (path, busy) -> {
            asked.remove(key);
            if (!busy) // a busy pool never computed it: ask again later
                known.put(key, new Walk(path != null && path.canReach(), path == null ? 0 : length(path), straight));
        });
        return null;
    }

    static double length(Path path) {
        double total = 0;
        for (int i = 1; i < path.getNodeCount(); i++)
            total += Math.sqrt(path.getNode(i).distanceToSqr(path.getNode(i - 1)));
        return total;
    }
}
