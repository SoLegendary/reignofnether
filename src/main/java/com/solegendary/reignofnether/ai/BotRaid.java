package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// The raid army: hits the enemy where their army isn't, their workers first, at outposts before the main base. It
// runs as soon as a fight turns. It waits at home until it has enough pop to go, and goes with the main army's
// wave instead when that army is too weak on its own (BotArmy.comingAlong): walking to a fight from home it would
// only arrive a unit at a time.
public class BotRaid {

    enum State { HOME, OUT }

    static final int MIN_POP = 6;
    private static final int RANGE = 24;
    private static final long RAID_TIMEOUT = 3 * 60 * 20; // a raid that found nothing in this long comes home
    private static final long REST_TICKS = 45 * 20; // after a raid, this long before the next one
    private static final int ARMY_AVOID = 48; // targets this near where the enemy army was seen are left alone

    private final BotBrain bot;
    private final BotArmy army;
    State state = State.HOME;
    private BlockPos target = null;
    private long outSince = 0, nextRaid = 0;

    BotRaid(BotBrain bot, BotArmy army) {
        this.bot = bot;
        this.army = army;
    }

    boolean isOut() {
        return state == State.OUT;
    }

    // the base is under a real attack: back home, the defence takes over
    void recall() {
        if (state == State.OUT)
            back("the base is under attack");
    }

    void think() {
        List<LivingEntity> raid = army.raid();
        if (raid.isEmpty()) {
            state = State.HOME;
            return;
        }
        if (state == State.OUT)
            raid(raid);
        else if (!bot.defence.isDefending())
            atHome(raid);
    }

    private void raid(List<LivingEntity> raid) {
        List<LivingEntity> enemies = army.enemiesNearSquad(raid).stream().filter(e -> e instanceof AttackerUnit).toList();
        if (!enemies.isEmpty() && !army.wins(raid, enemies, BotArmy.RETREAT_MARGIN)) {
            back("losing vs " + BotMath.popOf(enemies) + " pop");
            return;
        }
        if (bot.tick - outSince > RAID_TIMEOUT) {
            back("took too long");
            return;
        }
        // workers in reach first: they're what the raid is for
        BlockPos at = BotMath.centroid(raid);
        LivingEntity worker = bot.memory.visibleEnemies.stream().filter(e -> e instanceof WorkerUnit && e.isAlive() &&
                e.blockPosition().distSqr(at) < RANGE * RANGE).min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(Vec3.atCenterOf(at)))).orElse(null);
        List<LivingEntity> free = raid.stream().filter(BotBrain::isFree).toList();
        if (worker != null && enemies.isEmpty()) {
            if (!free.isEmpty())
                bot.orderTarget(UnitAction.ATTACK, free, worker);
            return;
        }
        if (BotMath.isNear(raid, target, 12) && enemies.isEmpty()) {
            BlockPos next = pickTarget(raid);
            if (next == null || next.equals(target)) {
                back("nothing left there");
                return;
            }
            target = next;
            army.attackMove(raid, target);
            return;
        }
        if (!free.isEmpty())
            army.attackMove(free, target);
    }

    private void atHome(List<LivingEntity> raid) {
        if (BotMath.popOf(raid) >= MIN_POP && bot.tick >= nextRaid) {
            BlockPos t = pickTarget(raid);
            if (t != null && army.attackMove(raid, t)) {
                target = t;
                state = State.OUT;
                outSince = bot.tick;
                bot.log("raid: {} pop sets off for {} from home", BotMath.popOf(raid), bot.fromHome(t));
                return;
            }
        }
        BlockPos rally = army.rallyPoint();
        List<LivingEntity> idle = raid.stream().filter(e -> BotBrain.isFree(e) && e.blockPosition().distSqr(rally) > 10 * 10).toList();
        if (!idle.isEmpty())
            bot.order(UnitAction.MOVE, idle, rally);
    }

    private void back(String why) {
        bot.log("raid: back home, {}", why);
        state = State.HOME;
        target = null;
        nextRaid = bot.tick + REST_TICKS;
        List<LivingEntity> raid = army.raid().stream().filter(e -> !bot.unit(e).fleeing).toList();
        bot.order(UnitAction.MOVE, raid, army.rallyPoint());
    }

    // a known enemy drop-off away from their army, outposts before their capitol, nearest first; one their
    // defenders there would beat is skipped
    @Nullable
    BlockPos pickTarget(List<LivingEntity> raid) {
        List<BlockPos> armySeen = bot.memory.recentEnemyArmy().stream().map(bot.memory::lastSeenPos).toList();
        BlockPos from = BotMath.centroid(raid);
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BotMemory.SeenBuilding sb : bot.memory.enemyBuildings.values()) {
            BuildingPlacement b = BuildingUtils.findBuilding(false, sb.pos());
            if (b == null || b.isDestroyedServerside || !(b.isCapitol || b.getBuilding().canAcceptResources))
                continue;
            if (armySeen.stream().anyMatch(p -> p.distSqr(sb.pos()) < ARMY_AVOID * ARMY_AVOID))
                continue;
            List<LivingEntity> guards = bot.memory.enemiesNear(Vec3.atCenterOf(sb.pos()), RANGE).stream().filter(e -> e instanceof AttackerUnit).toList();
            if (!guards.isEmpty() && !army.wins(raid, guards, BotArmy.ATTACK_MARGIN))
                continue;
            double score = Math.sqrt(sb.pos().distSqr(from)) + (b.isCapitol ? 200 : 0);
            if (score < bestScore) {
                bestScore = score;
                best = sb.pos();
            }
        }
        return best;
    }
}
