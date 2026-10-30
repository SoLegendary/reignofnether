package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.ping.PingType;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.units.villagers.MilitiaUnit;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitRole;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.*;

// Enemies at our base or on our workers. Whoever hits a worker becomes a target at once (the worker knows who hit
// it), and the nearest troops go for it, as many as the combat sim says it takes. A real attack on the base gets
// the home army, the main army too when the home army can't hold, and the Call to Arms when only the militia
// turns it. Workers flee when they can get away, and fight together when they can't but would win.
public class BotDefence {

    private static final int DEFEND_RANGE = 24;
    private static final int SMALL_THREAT_POP = 3;
    private static final int HURT_TICKS = 3 * 20; // a worker hit this recently knows who hit it
    private static final int MAX_RESPONDERS = 6;
    private static final int HUNT_LEASH = 48; // responders give up beyond this distance from our nearest building
    private static final int WORKER_GROUP_RANGE = 10;
    private static final int WORKER_FIGHT_LEASH = 24; // workers fighting back don't chase further than this
    private static final double ESCAPE_SPEED = 1.05; // an attacker this much faster than a worker catches it
    static final double HOLD_MARGIN = 0.1; // the defenders must win by this much to hold without help

    private final BotBrain bot;
    private boolean defending = false;
    private BlockPos defendPos = null;
    List<LivingEntity> threats = List.of();
    private List<LivingEntity> seenThreats = List.of(); // the ones in sight: where the fight is

    BotDefence(BotBrain bot) {
        this.bot = bot;
    }

    boolean isDefending() {
        return defending;
    }

    void think() {
        releaseResponders();
        Map<LivingEntity, LivingEntity> attackers = workerAttackers();
        threats = findThreats(attackers.values());
        protectWorkers(attackers);
        sendMilitia();
        if (threats.isEmpty()) {
            if (defending)
                bot.comms.held(); // a real attack, beaten
            defending = false;
            backToWork();
            return;
        }
        // a few stragglers or a raider on our workers: just enough troops to kill them
        if (BotMath.popOf(threats) <= SMALL_THREAT_POP && respond(threats)) {
            defending = false;
            return;
        }
        List<LivingEntity> defenders = bot.army.defenders(threats);
        if (defenders.isEmpty() && !bot.fighters.isEmpty()) { // an outpost we can't hold: its workers leave, the rest carries on
            defending = false;
            return;
        }
        BlockPos pos = BotMath.centroid(seenThreats);
        boolean moved = defendPos == null || defendPos.distSqr(pos) > 10 * 10;
        if (moved) {
            bot.comms.underAttack(pos, BotMath.popOf(threats));
            bot.log("defence: {} pop at {} from home", BotMath.popOf(threats), bot.fromHome(pos));
        }
        defending = true;
        defendPos = pos;
        bot.army.orderSoldiers(PingType.DEFEND, pos);
        callToArms(defenders);
        defenders = defenders.stream().filter(e -> canHitAny(e, threats)).toList(); // the rest would only stand under them
        // re-order everyone only when the fight moves, otherwise just send idle ones in
        bot.army.attackMove(moved ? defenders : defenders.stream().filter(e -> BotBrain.isFree(e)).toList(), pos);
    }

    // can these units hold against the threat without help?
    static boolean canHold(List<LivingEntity> defenders, List<LivingEntity> threats) {
        return CombatSim.fight(BotFighters.of(defenders, true), BotFighters.of(threats, true)).margin() >= HOLD_MARGIN;
    }

    // worker -> the enemy that just hit it
    private Map<LivingEntity, LivingEntity> workerAttackers() {
        Map<LivingEntity, LivingEntity> map = new HashMap<>();
        for (LivingEntity w : bot.workers) {
            LivingEntity by = w.getLastHurtByMob();
            if (by != null && by.isAlive() && w.tickCount - w.getLastHurtByMobTimestamp() < HURT_TICKS && isHostile(by))
                map.put(w, by);
        }
        return map;
    }

    private boolean isHostile(LivingEntity e) {
        return e instanceof Unit u && (u.getOwnerName().isBlank() || bot.memory.isEnemy(u.getOwnerName()));
    }

    // enemies anywhere our buildings can see, plus whoever is hitting our workers, plus the rest of their army we
    // saw with them a moment ago: an army walking in through fog is judged by its full size, not its first 3 units
    private List<LivingEntity> findThreats(Collection<LivingEntity> onWorkers) {
        Set<LivingEntity> found = new LinkedHashSet<>(onWorkers);
        for (BuildingPlacement b : bot.buildings)
            found.addAll(bot.memory.enemiesNear(Vec3.atCenterOf(b.centrePos), Math.max(DEFEND_RANGE, b.getSightRange())));
        seenThreats = new ArrayList<>(found);
        if (!found.isEmpty())
            found.addAll(bot.memory.rememberedNear(BotMath.centroid(found), ARMY_RANGE));
        return new ArrayList<>(found);
    }

    private static final int ARMY_RANGE = 48;

    // e can hurt at least one of them (a sword can't reach a flyer)
    static boolean canHitAny(LivingEntity e, List<LivingEntity> targets) {
        CombatSim.Fighter f = BotFighters.of(e, true);
        return targets.stream().anyMatch(t -> CombatSim.dpsAgainst(f, BotFighters.of(t, true), 1) > 0);
    }

    private boolean nearBase(BlockPos p, int range) {
        for (BuildingPlacement b : bot.buildings)
            if (b.centrePos.distSqr(p) < (long) range * range)
                return true;
        return false;
    }

    // ---------------------------------------------------------------- responders

    // the nearest troops at home, as few as the sim says beat each target and whoever is with it; false when
    // even all of them can't, so it's a real attack
    private boolean respond(List<LivingEntity> targets) {
        for (LivingEntity t : targets) {
            List<LivingEntity> local = new ArrayList<>(bot.memory.enemiesNear(t.position(), 12));
            if (!local.contains(t))
                local.add(t);
            List<LivingEntity> group = new ArrayList<>();
            for (LivingEntity f : bot.fighters)
                if (local.stream().anyMatch(l -> l.getId() == bot.unit(f).chasingId))
                    group.add(f);
            if (canHold(group, local))
                continue;
            boolean flying = BotBrain.has(t, UnitRole.FLYER);
            List<LivingEntity> pool = bot.army.atHome().stream()
                    .filter(e -> e instanceof AttackerUnit && !BotBrain.isHero(e) && bot.unit(e).chasingId < 0 && !bot.unit(e).fleeing)
                    .filter(e -> !flying || BotBrain.has(e, UnitRole.ANTI_AIR))
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(t))).toList();
            int n = BotMath.fewest(Math.max(0, Math.min(pool.size(), MAX_RESPONDERS - group.size())),
                    k -> canHold(concat(group, pool.subList(0, k)), local));
            if (n < 0)
                return false;
            List<LivingEntity> picked = pool.subList(0, n);
            if (bot.orderTarget(UnitAction.ATTACK, picked, t))
                for (LivingEntity h : picked)
                    bot.unit(h).chasingId = t.getId();
        }
        return true;
    }

    private static List<LivingEntity> concat(List<LivingEntity> a, List<LivingEntity> b) {
        List<LivingEntity> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    // responders come back once the target is dead, out of sight, or has led them too far from home
    private void releaseResponders() {
        Set<Integer> visible = new HashSet<>();
        for (LivingEntity e : bot.memory.visibleEnemies)
            visible.add(e.getId());
        List<LivingEntity> back = new ArrayList<>();
        for (LivingEntity f : bot.fighters) {
            BotUnit s = bot.unit(f);
            if (s.chasingId >= 0 && (!visible.contains(s.chasingId) || !nearBase(f.blockPosition(), HUNT_LEASH))) {
                s.chasingId = -1;
                back.add(f);
            }
        }
        bot.order(UnitAction.MOVE, back, bot.army.rallyPoint()); // one order for all of them
    }

    boolean isHunting(LivingEntity e) {
        return bot.unit(e).chasingId >= 0;
    }

    // ---------------------------------------------------------------- workers

    private void protectWorkers(Map<LivingEntity, LivingEntity> attackers) {
        // a fight back ends when the attacker is gone or has drawn them too far
        for (LivingEntity w : bot.workers) {
            BotUnit s = bot.unit(w);
            if (s.fightingId < 0)
                continue;
            LivingEntity foe = threats.stream().filter(t -> t.getId() == s.fightingId).findFirst().orElse(null);
            if (foe == null || s.fightFrom == null || w.blockPosition().distSqr(s.fightFrom) > WORKER_FIGHT_LEASH * WORKER_FIGHT_LEASH) {
                s.fightingId = -1;
                if (foe != null)
                    bot.order(UnitAction.MOVE, List.of(w), safety(w));
            }
        }
        for (var hit : attackers.entrySet()) {
            LivingEntity w = hit.getKey(), foe = hit.getValue();
            if (bot.unit(w).fightingId >= 0)
                continue;
            List<LivingEntity> local = new ArrayList<>(bot.memory.enemiesNear(w.position(), WORKER_GROUP_RANGE));
            if (!local.contains(foe))
                local.add(foe);
            List<LivingEntity> group = bot.workers.stream()
                    .filter(o -> bot.unit(o).fightingId < 0 && o.distanceToSqr(w) < WORKER_GROUP_RANGE * WORKER_GROUP_RANGE).toList();
            if (!canEscape(w, local) && canHold(group, local)) {
                if (bot.orderTarget(UnitAction.ATTACK, group, foe))
                    for (LivingEntity f : group) {
                        bot.unit(f).fightingId = foe.getId();
                        bot.unit(f).fightFrom = f.blockPosition();
                    }
                continue;
            }
            BlockPos safe = safety(w);
            if (!safe.equals(((Unit) w).getMoveGoal().getMoveTarget()))
                bot.order(UnitAction.MOVE, List.of(w), safe);
        }
    }

    // it gets away unless someone faster, or someone who shoots, is after it
    private static boolean canEscape(LivingEntity worker, List<LivingEntity> enemies) {
        double speed = worker.getAttributeValue(Attributes.MOVEMENT_SPEED);
        for (LivingEntity e : enemies)
            if (BotBrain.has(e, UnitRole.RANGED) || e.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed * ESCAPE_SPEED)
                return false;
        return true;
    }

    // our nearest troops, else home
    private BlockPos safety(LivingEntity worker) {
        LivingEntity nearest = bot.fighters.stream().filter(f -> f.distanceToSqr(worker) < 48 * 48)
                .min(Comparator.comparingDouble(f -> f.distanceToSqr(worker))).orElse(null);
        return nearest != null ? nearest.blockPosition() : bot.home;
    }

    // ---------------------------------------------------------------- call to arms

    // villagers called up as the sim says turn the fight, when the defenders and the militia we already have can't
    // hold it. Not even the whole town would: everyone goes anyway, a base that falls loses its workers too
    private void callToArms(List<LivingEntity> defenders) {
        BuildingPlacement tc = bot.getCapitol();
        int range = TownCentre.MILITIA_RANGE;
        // any enemy in the bell's reach rings it, not just a fight centred there
        if (tc == null || !tc.isBuilt || !(tc.getBuilding() instanceof TownCentre) ||
                threats.stream().noneMatch(t -> t.blockPosition().distSqr(tc.centrePos) < (long) range * range))
            return;
        // flyers only and no bows: militia would stand under them doing nothing, the workers are better off running
        if (!ResearchServerEvents.playerHasResearch(bot.name, ProductionItems.RESEARCH_MILITIA_BOWS) &&
                threats.stream().allMatch(t -> BotBrain.has(t, UnitRole.FLYER)))
            return;
        List<CombatSim.Fighter> theirs = BotFighters.of(threats, true);
        List<CombatSim.Fighter> ours = new ArrayList<>(BotFighters.of(defenders, true));
        ours.addAll(BotFighters.of(bot.militia, true));
        CombatSim.Fighter militia = BotFighters.of(bot.level, EntityRegistrar.MILITIA_UNIT.get());
        if (militia == null)
            return;
        ours.addAll(Collections.nCopies((int) bot.workers.stream().filter(w -> bot.unit(w).called).count(), militia)); // on their way
        if (CombatSim.fight(ours, theirs).margin() >= HOLD_MARGIN)
            return;
        List<LivingEntity> villagers = bot.workers.stream()
                .filter(w -> !bot.unit(w).called && w.blockPosition().distSqr(tc.centrePos) < (long) range * range)
                .sorted(Comparator.comparingDouble(w -> w.blockPosition().distSqr(tc.centrePos))).toList();
        int n = BotMath.fewest(villagers.size(), k -> {
            List<CombatSim.Fighter> with = new ArrayList<>(ours);
            with.addAll(Collections.nCopies(k, militia));
            return CombatSim.fight(with, theirs).margin() >= HOLD_MARGIN;
        });
        if (villagers.isEmpty())
            return;
        List<LivingEntity> call = villagers.subList(0, n > 0 ? n : villagers.size());
        if (bot.order(UnitAction.CALL_TO_ARMS_UNIT, call, tc.centrePos)) {
            for (LivingEntity v : call)
                bot.unit(v).called = true;
            bot.log("defence: call to arms, {} villagers", call.size());
        }
    }

    // the nearest militia, as many as it takes, go for the enemies within the bell's range (outside it they turn
    // back into villagers, so they never chase further)
    private void sendMilitia() {
        BuildingPlacement tc = bot.getCapitol();
        if (bot.militia.isEmpty() || tc == null)
            return;
        int range = TownCentre.MILITIA_RANGE - 8;
        List<LivingEntity> inRange = threats.stream().filter(t -> t.blockPosition().distSqr(tc.centrePos) < (long) range * range).toList();
        if (inRange.isEmpty())
            return;
        BlockPos at = BotMath.centroid(inRange);
        // bows against flyers, swords against the rest (a sword hits harder up close)
        if (ResearchServerEvents.playerHasResearch(bot.name, ProductionItems.RESEARCH_MILITIA_BOWS)) {
            boolean flyers = inRange.stream().filter(t -> BotBrain.has(t, UnitRole.FLYER)).count() * 2 > inRange.size();
            bot.order(flyers ? UnitAction.MILITIA_USE_BOW : UnitAction.MILITIA_USE_SWORD,
                    bot.militia.stream().filter(m -> m instanceof MilitiaUnit mu && mu.isUsingBow() != flyers).toList(), tc.centrePos);
        }
        List<LivingEntity> sorted = bot.militia.stream().filter(m -> canHitAny(m, inRange)).sorted(Comparator.comparingDouble(m -> m.blockPosition().distSqr(at))).toList();
        int n = BotMath.fewest(sorted.size(), k -> canHold(sorted.subList(0, k), inRange));
        List<LivingEntity> send = sorted.subList(0, n < 0 ? sorted.size() : n);
        bot.army.attackMove(send.stream().filter(m -> ((Unit) m).getTargetGoal().getTarget() == null).toList(), at);
    }

    // the fight is over: every militia and every villager still running to the bell goes back to work
    private void backToWork() {
        List<LivingEntity> back = new ArrayList<>(bot.militia);
        back.addAll(bot.workers.stream().filter(w -> bot.unit(w).called).toList());
        if (back.isEmpty() || !bot.order(UnitAction.BACK_TO_WORK_UNIT, back, bot.home))
            return;
        for (LivingEntity w : bot.workers)
            bot.unit(w).called = false;
        bot.log("defence: back to work, {} units", back.size());
    }
}
