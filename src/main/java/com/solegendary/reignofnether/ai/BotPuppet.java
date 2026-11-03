package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// The armies when a net does the thinking (BotSupervisor): no decisions here, only its orders carried out. The main
// army, the second army split off it, each hero and the raid army go where they're told and hold, move, attack or
// pull back. Paths, targets in a fight, kiting and spells stay with the rest of the bot (BotMicro, BotHeroes), and
// whoever's at home answers an attack on the base (BotDefence)
class BotPuppet {

    private static final double RAID_SHARE = 0.1;
    private static final int NEAR = 10, REORDER_TICKS = 5 * 20; // another manager's order gets this long before ours wins
    private static final int BODY = 24, REINFORCE_POP = 8; // reinforcements go out once they're 8 pop or a quarter of the group
    private static final double REINFORCE_SHARE = 0.25;

    private final BotBrain bot;
    private final BotArmy army;

    BotPuppet(BotBrain bot, BotArmy army) {
        this.bot = bot;
        this.army = army;
    }

    void think() {
        BotSupervisor s = bot.supervisor;
        assign(s);
        List<LivingEntity> main = new ArrayList<>(army.main());
        for (int k = 0; k < 2; k++) {
            LivingEntity h = s.hero(k);
            if (h == null || s.heroes[k].order() == BotSupervisor.FOLLOW)
                continue;
            main.remove(h);
            BotSupervisor.Group g = s.heroes[k];
            drive(List.of(h), g.order() == BotSupervisor.HERO_HOME ? BotSupervisor.RETREAT : g.order(), g.at());
        }
        drive(main, s.main.order(), s.main.at());
        if (s.split > 0)
            drive(army.home(), s.second.order(), s.second.at());
        drive(army.raid(), s.raid.order() == BotSupervisor.OUT ? BotSupervisor.ATTACK : BotSupervisor.RETREAT, s.raid.at());
    }

    // new units join the main army, a tenth of the pop the raid army; the second army is the net's share of the main
    private void assign(BotSupervisor s) {
        int armyPop = BotMath.popOf(bot.fighters);
        for (LivingEntity e : bot.fighters) {
            BotUnit u = bot.unit(e);
            if (u.squad == BotUnit.Squad.NONE)
                u.squad = !BotBrain.isHero(e) && BotMath.popOf(army.raid()) < armyPop * RAID_SHARE ? BotUnit.Squad.RAID : BotUnit.Squad.MAIN;
        }
        List<LivingEntity> pool = bot.fighters.stream().filter(e -> !BotBrain.isHero(e) && bot.unit(e).squad != BotUnit.Squad.RAID).toList();
        int wanted = (int) Math.round(BotMath.popOf(pool) * s.split);
        BlockPos to = s.second.at() != null ? s.second.at() : bot.home;
        List<LivingEntity> second = new ArrayList<>(army.home());
        // too many: the ones furthest from where the second army goes rejoin the main one; too few: the nearest leave it
        second.sort(Comparator.comparingDouble(e -> -e.blockPosition().distSqr(to)));
        while (!second.isEmpty() && BotMath.popOf(second) - BotMath.popOf(List.of(second.get(0))) >= wanted)
            bot.unit(second.remove(0)).squad = BotUnit.Squad.MAIN;
        List<LivingEntity> main = pool.stream().filter(e -> bot.unit(e).squad == BotUnit.Squad.MAIN)
                .sorted(Comparator.comparingDouble(e -> e.blockPosition().distSqr(to))).toList();
        int pop = BotMath.popOf(second);
        for (LivingEntity e : main) {
            if (pop >= wanted)
                break;
            bot.unit(e).squad = BotUnit.Squad.HOME;
            pop += BotMath.popOf(List.of(e));
        }
    }

    private void drive(List<LivingEntity> group, int order, @Nullable BlockPos at) {
        group = group.stream().filter(e -> !bot.defence.isFighting(e)).toList();
        if (group.isEmpty())
            return;
        if (order == BotSupervisor.RETREAT || at == null) {
            BlockPos rally = army.rallyPoint();
            bot.order(UnitAction.MOVE, BotArmy.idleAwayFrom(group, rally, 16).stream().filter(e -> !bot.unit(e).fleeing).toList(), rally);
            return;
        }
        BlockPos rally = army.rallyPoint();
        group = reinforce(group, at, rally);
        List<LivingEntity> away = group.stream().filter(e -> e.blockPosition().distSqr(at) > NEAR * NEAR && !bot.unit(e).fleeing &&
                (BotBrain.isFree(e) || (bot.unit(e).lastTarget != at.asLong() && bot.unit(e).sinceOrdered(bot.tick) > REORDER_TICKS))).toList();
        switch (order) {
            case BotSupervisor.MOVE -> bot.order(UnitAction.MOVE, away, at);
            case BotSupervisor.HOLD -> army.attackMove(away, at);
            case BotSupervisor.ATTACK -> {
                army.attackMove(away, at);
                // there and nothing to fight: their buildings, the right ones first
                if (BotMath.isNear(group, at, NEAR * 2) && army.enemiesNearSquad(group).isEmpty())
                    army.focusBuildings(group);
            }
            default -> {}
        }
    }

    // units far behind the group (fresh from production, or left behind) wait at the rally point and only go out
    // together once there are enough of them: one by one they'd walk into the enemy or a camp alone and die
    private List<LivingEntity> reinforce(List<LivingEntity> group, BlockPos at, BlockPos rally) {
        LivingEntity lead = group.stream().min(Comparator.comparingDouble(e -> e.blockPosition().distSqr(at))).orElseThrow();
        List<LivingEntity> body = group.stream().filter(e -> e.blockPosition().closerThan(lead.blockPosition(), BODY)).toList();
        List<LivingEntity> behind = group.stream().filter(e -> !body.contains(e)).toList();
        if (behind.isEmpty() || BotMath.popOf(behind) >= Math.max(REINFORCE_POP, BotMath.popOf(body) * REINFORCE_SHARE))
            return group;
        bot.order(UnitAction.MOVE, BotArmy.idleAwayFrom(behind, rally, 16).stream().filter(e -> !bot.unit(e).fleeing).toList(), rally);
        return body;
    }
}
