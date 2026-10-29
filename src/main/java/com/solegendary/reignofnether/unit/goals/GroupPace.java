package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// A group ordered to move together walks as a pack at its slowest member's speed. A member that falls behind the
// pack walks at its own full speed to catch up, and one that gets ahead slows down. Ranged units keep a few
// blocks behind the melee. "Where the pack is" is the average distance the melee members have left to the
// destination (all members when there's no melee).
// ponytail: the slowest speed is fixed at the order, so a group that loses its slowest unit stays slow until the next one
public class GroupPace {

    private static final double SLACK = 3; // blocks either side of its place in the pack before a unit changes pace
    private static final double RANGED_BEHIND = 4;
    private static final double AHEAD_SPEED = 0.6; // share of the slowest speed for a unit ahead of the pack

    private final List<Unit> members;
    private final Vec3 destination;
    private final double slowest; // blocks per tick
    private long refTick = -1;
    private double ref;
    private boolean hasMelee;

    public GroupPace(List<Unit> members, BlockPos destination) {
        this.members = members;
        this.destination = Vec3.atCenterOf(destination);
        double s = Double.MAX_VALUE;
        for (Unit u : members)
            s = Math.min(s, u.getMoveGoal().getWalkSpeed(u));
        this.slowest = s;
    }

    // the fastest this unit may walk now, in blocks per tick
    public double speedCap(Unit u) {
        LivingEntity e = (LivingEntity) u;
        long now = e.level().getGameTime();
        if (now != refTick) {
            refTick = now;
            updateRef();
        }
        double want = ref + (hasMelee && u instanceof RangedAttackerUnit ? RANGED_BEHIND : 0);
        double left = distLeft(e);
        if (left - want > SLACK)
            return Double.MAX_VALUE;
        if (want - left > SLACK)
            return slowest * AHEAD_SPEED;
        return slowest;
    }

    private void updateRef() {
        double melee = 0, all = 0;
        int meleeCount = 0, allCount = 0;
        for (Unit u : members) {
            LivingEntity e = (LivingEntity) u;
            if (!e.isAlive() || u.getMoveGoal().pace != this)
                continue; // dead or given another order
            double left = distLeft(e);
            all += left;
            allCount++;
            if (!(u instanceof RangedAttackerUnit)) {
                melee += left;
                meleeCount++;
            }
        }
        hasMelee = meleeCount > 0;
        ref = hasMelee ? melee / meleeCount : allCount > 0 ? all / allCount : 0;
    }

    private double distLeft(LivingEntity e) {
        return Math.sqrt(e.position().distanceToSqr(destination.x, e.getY(), destination.z));
    }
}
