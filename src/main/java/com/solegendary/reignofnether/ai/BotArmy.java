package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.ping.PingType;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.soldier.SoldierServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

// The fighters are three armies, split by pop: a big MAIN army with the heroes that does the fighting and waits
// between waves on the hot zone (between our team's bases and the enemy, so it meets their attacks), a HOME army
// guarding the workers and base, and a RAID army that hits the enemy's workers and outposts (BotRaid). Every
// decision to fight is the combat sim's call: attack when we clearly win, retreat as soon as the fight turns. The
// home army helps the main army's fights when no enemy could be coming for the base, and leaves early once the
// fight turns (it's the one left to defend). The main army comes home when the home army can't hold.
public class BotArmy {

    enum State { RALLY, ATTACK, RETREAT, DEFEND, SUPPORT }

    static final double ATTACK_MARGIN = 0.15; // attack an army we expect to beat by this much
    static final double RETREAT_MARGIN = 0; // leave a fight as soon as we no longer expect to win it
    static final double COMMIT_MARGIN = -0.15; // already in melee: running hands them free hits, so only a clear loss
    private static final int ENGAGED_RANGE = 40; // our units this near the enemy are the ones fighting
    private static final int MELEE_RANGE = 6;
    private static final int HELP_RANGE = 80; // the home army only helps a fight this near home: further, it's there too late
    private static final int JOIN_RANGE = 64; // reinforcements further than this wait out the fight at the rally point
    static final double CAMP_MARGIN = 0.4; // neutral camps only when it's an easy win
    private static final int ASSIGN_EVERY = 2 * 20;
    private static final int FIGHT_RANGE = 24;
    private static final int THREAT_RANGE = 48; // enemies further than this from the main army could reach home first
    private static final int CAMP_CLEAR_RANGE = 96;
    private static final int GROUP_TIMEOUT = 20 * 20;
    private static final int RETREAT_TIMEOUT = 30 * 20;
    private static final int SUPPORT_TICKS = 45 * 20;
    private static final int REINFORCE_POP = 4;

    private final BotBrain bot;
    final BotRaid raid;
    State state = State.RALLY; // of the main army
    // each army's share of the army pop (max pop minus the workers): one big, two medium
    private static final double MAIN_SHARE = 0.5, HOME_SHARE = 0.25, RAID_SHARE = 0.25;
    private static final double HOME_LEAVE_MARGIN = 0.15; // helping the main army, the home army leaves below this
    private boolean homeHelping = false;
    // a small main army with nothing to do harasses: the enemy's workers, while nothing threatens us
    private boolean harassing = false;
    private long harassSince = 0, nextHarass = 0;
    private static final long HARASS_TICKS = 2 * 60 * 20, HARASS_REST = 60 * 20;
    private static final int SAFE_RANGE = 96; // no enemy army seen this near home: it's safe to leave
    private BlockPos target = null;
    private BlockPos pingTarget = null; // an ally asked us to attack here
    private BlockPos clearTarget = null; // a neutral camp we're clearing
    private long clearSince = 0;
    private final Map<BlockPos, Long> campGaveUp = new HashMap<>(); // camp -> tick we stop skipping it
    private static final long CLEAR_TIMEOUT = 90 * 20, CAMP_GIVE_UP_TICKS = 5 * 60 * 20;
    private BlockPos supportPos = null;
    private long supportUntil = 0;
    private int attackStartPop = 0;
    private int wavesSent = 0;
    private long retreatStartTick = 0;
    private long waveReadySince = -1; // the wave is ready: it groups up before setting off
    private BlockPos planned = null; // where the grouping wave will go (announced to the team)
    private boolean targetIsBase = false; // the target is enemy buildings, not a spot we're looking at
    private long lastAssign = -1_000_000;
    private BlockPos lastHold = null;
    // joining an ally bot's wave: meet its army where it groups up, and leave with it
    private BlockPos meetAt = null;
    private String meetWith = null;
    private long meetSince = 0;
    private static final int MEET_TIMEOUT = 60 * 20;
    private static final int WAVE_CHECK_EVERY = 2 * 20; // the sim's verdict on a wave holds this long
    private long waveCheckTick = -1_000_000;
    private boolean waveReadyCached = false;
    private static final long STALL_TICKS = 60 * 20;
    private int peakPop = 0; // the main army's biggest size since the last wave left, and when it got there
    private long peakSince = 0;
    private static final int REGROUP_TIMEOUT = 15 * 20, REGROUP_EVERY = 30 * 20, REGROUP_SPREAD = 16, REGROUP_FAR = 48;
    private long regroupUntil = -1, lastRegroup = -1_000_000;

    BotArmy(BotBrain bot) {
        this.bot = bot;
        this.raid = new BotRaid(bot, this);
    }

    void think() {
        if (bot.tick - lastAssign >= ASSIGN_EVERY) {
            lastAssign = bot.tick;
            assignSquads();
        } else {
            assignNewUnits();
        }
        if (state == State.DEFEND && !bot.defence.isDefending())
            state = State.RALLY;
        thinkMain();
        if (!bot.defence.isDefending())
            thinkHome();
        raid.think();
    }

    String describe() {
        return "M" + BotMath.popOf(main()) + " H" + BotMath.popOf(home()) + " R" + BotMath.popOf(raid()) + " " + state + " raid " + raid.state;
    }

    List<LivingEntity> main() {
        return squad(BotUnit.Squad.MAIN);
    }

    // the main army could take the neutrals guarding p (none there: true)
    boolean canClear(BlockPos p) {
        List<LivingEntity> camp = bot.memory.campUnitsNear(p, 24);
        return camp.isEmpty() || wins(main(), camp, CAMP_MARGIN);
    }

    List<LivingEntity> home() {
        return squad(BotUnit.Squad.HOME);
    }

    List<LivingEntity> raid() {
        return squad(BotUnit.Squad.RAID);
    }

    private List<LivingEntity> squad(BotUnit.Squad squad) {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : bot.fighters)
            if (bot.unit(e).squad == squad)
                list.add(e);
        return list;
    }

    private boolean mainAway() {
        return state == State.ATTACK || state == State.SUPPORT;
    }

    // whoever can answer a threat at home right now: the home army, plus the main and raid armies when they're home
    List<LivingEntity> atHome() {
        List<LivingEntity> list = new ArrayList<>(home());
        if (!mainAway())
            list.addAll(main());
        if (!raid.isOut())
            list.addAll(raid());
        return list;
    }

    // ---------------------------------------------------------------- squads

    // a new unit joins whichever army is furthest below its share; heroes always lead the main army
    void assignNewUnits() {
        for (LivingEntity e : bot.fighters) {
            BotUnit u = bot.unit(e);
            if (u.squad == BotUnit.Squad.NONE)
                u.squad = BotBrain.isHero(e) ? BotUnit.Squad.MAIN : neediestSquad();
        }
    }

    private boolean mainWeak = false; // the main army alone doesn't beat the enemy army we know of (assignSquads)

    private BotUnit.Squad neediestSquad() {
        if (mainWeak)
            return BotUnit.Squad.MAIN; // one army first: splitting a weaker one only loses it piece by piece
        double main = BotMath.popOf(main()) / MAIN_SHARE, home = BotMath.popOf(home()) / HOME_SHARE, raid = BotMath.popOf(raid()) / RAID_SHARE;
        return main <= home && main <= raid ? BotUnit.Squad.MAIN : home <= raid ? BotUnit.Squad.HOME : BotUnit.Squad.RAID;
    }

    // the home army helps a fight the main army is losing when no enemy could be coming for the base, and goes
    // back as soon as the fight turns, so it's alive to defend
    private void assignSquads() {
        List<CombatSim.Fighter> known = bot.memory.recentEnemyFighters();
        mainWeak = !known.isEmpty() && CombatSim.fight(withAllies(main()), known).margin() < ATTACK_MARGIN;
        assignNewUnits();
        List<LivingEntity> home = home(), main = main();
        List<LivingEntity> nearMain = mainAway() ? enemiesNear(main) : List.of();
        boolean safe = !bot.defence.isDefending() && raidThreat(main).isEmpty();
        if (homeHelping) {
            List<CombatSim.Fighter> together = BotFighters.of(main, true);
            together.addAll(BotFighters.of(home, true));
            if (nearMain.isEmpty() || !safe || CombatSim.fight(together, BotFighters.of(nearMain, true)).margin() < HOME_LEAVE_MARGIN) {
                homeHelping = false;
                bot.log("army: home army ({} pop) goes back to the base", BotMath.popOf(home));
            }
            return;
        }
        if (!nearMain.isEmpty() && !home.isEmpty() && safe && BotMath.centroid(nearMain).distSqr(bot.home) < HELP_RANGE * HELP_RANGE) {
            List<CombatSim.Fighter> theirs = BotFighters.of(nearMain, true);
            List<CombatSim.Fighter> ours = BotFighters.of(main, true);
            List<CombatSim.Fighter> together = new ArrayList<>(ours);
            together.addAll(BotFighters.of(home, true));
            if (CombatSim.fight(ours, theirs).margin() < ATTACK_MARGIN && CombatSim.fight(together, theirs).margin() >= ATTACK_MARGIN) {
                bot.log("army: home army ({} pop) joins the main army's fight", BotMath.popOf(home));
                homeHelping = true;
                attackMove(home, BotMath.centroid(nearMain));
            }
        }
    }

    // the main army's fight right now, or null
    @org.jetbrains.annotations.Nullable
    BlockPos mainFight() {
        List<LivingEntity> near = mainAway() ? enemiesNear(main()) : List.of();
        return near.isEmpty() ? null : BotMath.centroid(near);
    }

    List<LivingEntity> enemiesNearSquad(List<LivingEntity> squad) {
        return enemiesNear(squad);
    }

    // enemy fighters we know of that aren't facing our main army: they could hit home while it's away
    private List<LivingEntity> raidThreat(List<LivingEntity> main) {
        BlockPos at = main.isEmpty() ? bot.home : BotMath.centroid(main);
        List<LivingEntity> threat = new ArrayList<>();
        for (LivingEntity e : bot.memory.recentEnemyArmy())
            if (bot.memory.lastSeenPos(e).distSqr(at) > THREAT_RANGE * THREAT_RANGE)
                threat.add(e);
        return threat;
    }

    // the home army's post: towards the nearest enemy that could come for us, else the front
    private BlockPos homePost() {
        BlockPos nearest = raidThreat(main()).stream().map(bot.memory::lastSeenPos)
                .min(Comparator.comparingDouble(p -> p.distSqr(bot.home))).orElse(null);
        if (nearest == null)
            return frontPoint();
        return BotMath.towards(bot.home, Vec3.atCenterOf(nearest), 14, rallyPoint());
    }

    private void thinkHome() {
        List<LivingEntity> home = home();
        if (home.isEmpty())
            return;
        BlockPos fight = homeHelping ? mainFight() : null;
        if (fight != null) {
            attackMove(home.stream().filter(e -> BotBrain.isFree(e) && e.blockPosition().distSqr(fight) > 12 * 12).toList(), fight);
            return;
        }
        BlockPos post = homePost();
        attackMove(home.stream().filter(e -> BotBrain.isFree(e) && e.blockPosition().distSqr(post) > 10 * 10 &&
                !bot.defence.isHunting(e)).toList(), post);
    }

    // the main army only drops its attack for the base itself: an outpost's workers just leave (BotExpansion)
    private boolean nearCore(List<LivingEntity> threats) {
        return threats.stream().anyMatch(t -> t.blockPosition().distSqr(bot.home) < CORE_RANGE * CORE_RANGE);
    }

    static final int CORE_RANGE = 48; // our base itself; drop-offs further out are outposts the army won't die for

    // who meets an attack on our base: the home army, and the main army when it's home or the home army can't hold
    List<LivingEntity> defenders(List<LivingEntity> threats) {
        assignNewUnits();
        List<LivingEntity> home = home();
        if (!mainAway())
            home = new ArrayList<>(bot.fighters);
        else if (!BotDefence.canHold(atHome(), threats)) {
            if (!nearCore(threats))
                return List.of(); // an outpost we can't hold: its workers leave, nobody dies for it
            bot.log("army: home army can't hold {} pop, main army comes back", BotMath.popOf(threats));
            home = new ArrayList<>(bot.fighters);
            clearTarget = pingTarget = null;
        } else {
            home = atHome();
        }
        if (home.size() == bot.fighters.size()) {
            state = State.DEFEND;
            cancelPlannedWave(true);
            raid.recall();
        }
        return home;
    }

    // ---------------------------------------------------------------- main army

    private void thinkMain() {
        List<LivingEntity> main = main();
        if (state != State.ATTACK) {
            meetAt = null;
            harassing = false;
        }
        switch (state) {
            case DEFEND -> {}
            case RALLY -> rally(main);
            case ATTACK -> attack(main);
            case SUPPORT -> {
                if (bot.tick > supportUntil || supportPos == null) {
                    state = State.RALLY;
                    supportPos = null;
                    return;
                }
                attackMove(idleAwayFrom(main, supportPos, 8), supportPos);
            }
            case RETREAT -> {
                BlockPos rally = rallyPoint();
                bot.order(UnitAction.MOVE, idleAwayFrom(main, rally, 16).stream().filter(e -> !bot.unit(e).fleeing).toList(), rally);
                if (BotMath.isNear(main, rally, 16) || bot.tick - retreatStartTick > RETREAT_TIMEOUT)
                    state = State.RALLY;
            }
        }
    }

    // between waves the main army never just waits at home: it holds the next expansion (clearing it for the
    // workers) or the front, takes easy neutral camps (they level the heroes), and sets off once the wave is ready
    private void rally(List<LivingEntity> main) {
        BlockPos hold = holdPoint();
        if (!hold.equals(lastHold)) {
            lastHold = hold;
            orderSoldiers(PingType.DEFEND, hold);
            bot.log("army: main holds {} from home", bot.fromHome(hold));
        }
        attackMove(idleAwayFrom(main, hold, 10), hold);
        // a camp goes with whoever can come: with nothing coming for the base, the home and raid armies too
        BotMemory.Camp camp = campInTheWay();
        List<LivingEntity> force = new ArrayList<>(main);
        boolean safe = camp != null && raidThreat(main).isEmpty() && !enemyArmyNearHome();
        if (safe) {
            force.addAll(home().stream().filter(e -> e.getHealth() > e.getMaxHealth() * 0.7f).toList());
            if (!raid.isOut())
                force.addAll(raid());
        }
        if (camp != null && wins(force, bot.memory.campUnitsNear(camp.pos(), 16), CAMP_MARGIN) && attackMove(force, camp.pos())) {
            for (LivingEntity e : force)
                if (bot.unit(e).squad != BotUnit.Squad.MAIN) {
                    bot.unit(e).lentFrom = bot.unit(e).squad;
                    bot.unit(e).squad = BotUnit.Squad.MAIN;
                }
            main = main();
            bot.log("army: {} pop clears a neutral camp at {} from home", BotMath.popOf(main), bot.fromHome(camp.pos()));
            clearTarget = camp.pos();
            clearSince = bot.tick;
            setTarget(camp.pos());
            startAttack(main);
            return;
        }
        boolean ready = waveReady(main);
        if (!ready) {
            cancelPlannedWave(false);
            harass(main, hold);
            return;
        }
        if (waveReadySince < 0) {
            // ready: it groups up first, with whoever comes along, and tells the team where it's going
            waveReadySince = bot.tick;
            lendToWave();
            main = main();
            planned = pingTarget != null ? pingTarget : bot.memory.getAttackTarget(bot.home, bot.level.random, target);
            if (planned == null)
                planned = bot.scout.nextTarget(); // we know of nothing: go looking with the army
            bot.comms.planningAttack(planned, GROUP_TIMEOUT / 20);
        }
        if (!BotMath.isNear(main, hold, 16) && bot.tick - waveReadySince <= GROUP_TIMEOUT)
            return;
        if (alliesComing(hold) && bot.tick - waveReadySince <= MEET_TIMEOUT)
            return; // an ally bot's army is on its way to set off with us
        waveReadySince = -1;
        setTarget(pingTarget != null ? pingTarget : planned);
        planned = null;
        if (target != null && attackMove(main, target)) {
            wavesSent++;
            startAttack(main);
            bot.log("army: wave {} sets off, {} pop, {} from home", wavesSent, BotMath.popOf(main), bot.fromHome(target));
            bot.comms.attacking(target);
        }
    }

    // too small for a wave, no home army yet and nothing coming for us: go after the enemy's workers instead of
    // standing around. Not while it holds an expansion for our workers; an army coming home sends it back (attack)
    private void harass(List<LivingEntity> main, BlockPos hold) {
        if (bot.tick < nextHarass || BotMath.popOf(main) < BotRaid.MIN_POP || BotMath.popOf(home()) >= BotRaid.MIN_POP ||
                hold.equals(bot.expansion.nextExpansion()) || enemyArmyNearHome())
            return;
        BlockPos t = raid.pickTarget(main);
        if (t == null || !attackMove(main, t))
            return;
        bot.log("army: main ({} pop) harasses {} from home", BotMath.popOf(main), bot.fromHome(t));
        harassing = true;
        harassSince = bot.tick;
        setTarget(t);
        startAttack(main);
    }

    private boolean enemyArmyNearHome() {
        return bot.memory.recentEnemyArmy().stream().anyMatch(e -> bot.memory.lastSeenPos(e).distSqr(bot.home) < SAFE_RANGE * SAFE_RANGE);
    }

    // a wave that was announced but won't leave: tell the team
    private void cancelPlannedWave(boolean underAttack) {
        if (waveReadySince >= 0 && planned != null)
            bot.comms.attackOff(underAttack);
        if (waveReadySince >= 0)
            returnLent();
        waveReadySince = -1;
        planned = null;
    }

    private void setTarget(BlockPos pos) {
        target = pos;
        targetIsBase = pos != null && bot.memory.enemyBuildings.values().stream().anyMatch(b -> b.pos().distSqr(pos) < 20 * 20);
    }

    private void startAttack(List<LivingEntity> main) {
        state = State.ATTACK;
        peakPop = 0;
        peakSince = bot.tick;
        attackStartPop = BotMath.popOf(main);
        orderSoldiers(PingType.ATTACK, target);
    }

    // players on our team as FPV soldiers follow the army like a commander's pings would tell them to
    private BlockPos soldierOrderPos = null;

    void orderSoldiers(PingType type, BlockPos pos) {
        if (pos == null || pos.equals(soldierOrderPos) || PlayerRoles.soldiersOf(bot.name).isEmpty())
            return;
        soldierOrderPos = pos;
        SoldierServerEvents.setOrder(bot.name, type, BotComms.ground(bot, pos));
    }

    private void attack(List<LivingEntity> main) {
        if (main.isEmpty()) {
            retreat(main, "no army left");
            bot.comms.armyLost();
            return;
        }
        if (meeting(main))
            return;
        if (harassing) {
            if (enemyArmyNearHome()) {
                retreat(main, "an enemy army is coming for the base");
                return;
            }
            if (bot.tick - harassSince > HARASS_TICKS || (BotMath.isNear(main, target, 12) && enemiesNear(main).isEmpty())) {
                retreat(main, "harass done");
                return;
            }
        }
        // camp done (or out of reach): home, but never in the middle of a fight that started there
        boolean fighting = !enemiesNear(main).isEmpty();
        if (clearTarget != null && !fighting && bot.memory.isSeen(clearTarget) && bot.memory.campUnitsNear(clearTarget, 24).isEmpty()) {
            clearTarget = null;
            retreat(main, "camp cleared");
            bot.comms.campCleared();
            return;
        }
        // mobs up a cliff or in a cave: the army would stand under them for good. Leave that camp alone a while
        if (clearTarget != null && !fighting && bot.tick - clearSince > CLEAR_TIMEOUT) {
            campGaveUp.put(clearTarget, bot.tick + CAMP_GIVE_UP_TICKS);
            clearTarget = null;
            retreat(main, "camp out of reach");
            return;
        }
        if (pingTarget != null && BotMath.isNear(main, pingTarget, 10) && bot.memory.enemiesNear(Vec3.atCenterOf(pingTarget), 16).isEmpty())
            pingTarget = null; // an ally's target holds until we've seen it's cleared
        // the fight in front of us, judged on our units actually there: the ones still walking up don't fight yet.
        // Losing there but not as a whole army: they fall back to the rest, and everyone goes in together
        List<LivingEntity> enemies = enemiesNear(main);
        BlockPos fightAt = enemies.isEmpty() ? null : BotMath.centroid(enemies);
        List<LivingEntity> there = fightAt == null ? main : main.stream().filter(e -> e.blockPosition().distSqr(fightAt) < ENGAGED_RANGE * ENGAGED_RANGE).toList();
        List<LivingEntity> engaged = there.isEmpty() ? main : there; // nobody there yet: the army as a whole
        boolean inMelee = engaged.stream().anyMatch(e -> enemies.stream().anyMatch(t -> t.distanceToSqr(e) < MELEE_RANGE * MELEE_RANGE));
        double margin = inMelee ? COMMIT_MARGIN : RETREAT_MARGIN;
        if (!enemies.isEmpty() && engaged.size() < main.size() && !wins(engaged, enemies, margin) && wins(main, enemies, RETREAT_MARGIN)) {
            List<LivingEntity> rest = main.stream().filter(e -> !engaged.contains(e)).toList();
            BlockPos meet = BotMath.centroid(rest);
            bot.order(UnitAction.MOVE, engaged.stream().filter(e -> !bot.unit(e).fleeing).toList(), meet);
            attackMove(rest.stream().filter(e -> e.blockPosition().distSqr(meet) > 8 * 8).toList(), meet);
            return;
        }
        if (!enemies.isEmpty() && !wins(engaged, enemies, margin)) {
            retreat(main, "losing the fight vs " + BotMath.popOf(enemies) + " pop");
            // the team only hears about it when it's a player's army: a neutral camp won't follow us home
            if (enemies.stream().anyMatch(e -> e instanceof Unit u && !u.getOwnerName().isBlank()))
                bot.comms.pullingBack();
            return;
        }
        if (BotMath.popOf(main) < attackStartPop / 3) {
            retreat(main, "lost most of the army");
            bot.comms.armyLost();
            return;
        }
        if (!enemies.isEmpty())
            regroupUntil = -1; // a fight: no waiting for stragglers
        else if (regroup(main))
            return;
        if (enemies.isEmpty())
            focusBuildings(main); // their army first (attack-move takes it on); with it gone, the right buildings
        BlockPos next = nextTarget();
        // got there and there's nothing left: whatever we remembered is gone
        if (target != null && BotMath.isNear(main, target, 12) && enemies.isEmpty() &&
                bot.memory.enemyBuildings.values().stream().noneMatch(sb -> sb.pos().distSqr(target) < 20 * 20 && bot.tick - sb.lastSeenTick() < 40)) {
            bot.memory.forget(target);
            if (targetIsBase)
                bot.comms.targetDown();
            if (target.equals(pingTarget))
                pingTarget = null;
            next = nextTarget();
        }
        if (next == null && target != null && !BotMath.isNear(main, target, 12))
            next = target; // still searching: keep going until we get there
        if (next == null) {
            retreat(main, "no target left");
            return;
        }
        if (!next.equals(target)) {
            setTarget(next);
            attackMove(main, target);
            orderSoldiers(PingType.ATTACK, target);
        }
        reinforce(main);
    }

    // on a long way to the target, an army that strings out stops round its middle until it's together again (or
    // 15 s), then goes on. At most once every 30 s, so it doesn't stop and start
    private boolean regroup(List<LivingEntity> main) {
        BlockPos at = BotMath.centroid(main);
        if (regroupUntil >= 0) {
            if (bot.tick < regroupUntil && spreadOut(main, at))
                return true;
            regroupUntil = -1;
            if (target != null)
                attackMove(main, target);
            return false;
        }
        if (target == null || bot.tick - lastRegroup < REGROUP_EVERY || at.distSqr(target) < REGROUP_FAR * REGROUP_FAR || !spreadOut(main, at))
            return false;
        lastRegroup = bot.tick;
        regroupUntil = bot.tick + REGROUP_TIMEOUT;
        bot.log("army: regrouping {} pop on the way, {} from home", BotMath.popOf(main), bot.fromHome(at));
        attackMove(main, at);
        return true;
    }

    // over a quarter of the army is far from its middle
    private static boolean spreadOut(List<LivingEntity> main, BlockPos at) {
        int far = BotMath.popOf(main.stream().filter(e -> e.blockPosition().distSqr(at) > REGROUP_SPREAD * REGROUP_SPREAD).toList());
        return far * 4 > BotMath.popOf(main);
    }

    private static final int FOCUS_RANGE = 40;

    // no enemy army in front of us: the capitol first (no more workers), then what trains troops, then the rest,
    // nearest first. Units that can't hit buildings keep attack-moving and pick off whatever shows up
    private void focusBuildings(List<LivingEntity> main) {
        BlockPos at = BotMath.centroid(main);
        BuildingPlacement best = null;
        int bestRank = Integer.MAX_VALUE;
        for (BotMemory.SeenBuilding sb : bot.memory.enemyBuildings.values()) {
            if (sb.pos().distSqr(at) > FOCUS_RANGE * FOCUS_RANGE)
                continue;
            BuildingPlacement b = BuildingUtils.findBuilding(false, sb.pos());
            if (b == null || b.isDestroyedServerside)
                continue;
            int rank = (b.isCapitol ? 0 : b instanceof ProductionPlacement ? 1 : 2) * 10_000 + BotMath.dist(sb.pos(), at);
            if (rank < bestRank) {
                bestRank = rank;
                best = b;
            }
        }
        if (best == null)
            return;
        long focus = best.centrePos.asLong();
        List<LivingEntity> hitters = main.stream().filter(e -> e instanceof AttackerUnit a && a.canAttackBuildings() &&
                !bot.unit(e).fleeing && (BotBrain.isFree(e) || bot.unit(e).lastTarget != focus)).toList();
        bot.order(UnitAction.ATTACK_BUILDING, hitters, best.centrePos);
    }

    private void retreat(List<LivingEntity> main, String why) {
        bot.log("army: retreat, {} of {} pop, {}", BotMath.popOf(main), attackStartPop, why);
        if (harassing)
            nextHarass = bot.tick + HARASS_REST;
        harassing = false;
        state = State.RETREAT;
        retreatStartTick = bot.tick;
        orderSoldiers(PingType.DEFEND, rallyPoint());
        clearTarget = pingTarget = null;
        bot.order(UnitAction.MOVE, main.stream().filter(e -> !bot.unit(e).fleeing).toList(), rallyPoint());
        returnLent();
    }

    private BlockPos nextTarget() {
        return pingTarget != null ? pingTarget : clearTarget != null ? clearTarget :
                bot.memory.getAttackTarget(bot.home, bot.level.random, target);
    }

    // a wave sets off when the sim says it beats the enemy army we've seen (or at the population cap). Not seen
    // anything yet: the plan's timing and size, growing each wave
    private boolean waveReady(List<LivingEntity> main) {
        if (bot.tick - waveCheckTick < WAVE_CHECK_EVERY)
            return waveReadyCached;
        waveCheckTick = bot.tick;
        waveReadyCached = checkWave(main);
        return waveReadyCached;
    }

    private boolean checkWave(List<LivingEntity> main) {
        main = new ArrayList<>(main);
        main.addAll(comingAlong());
        int pop = BotMath.popOf(main);
        if (pop > peakPop) {
            peakPop = pop;
            peakSince = bot.tick;
        }
        if (bot.getPop() >= bot.getSupply() - 2 && bot.getSupply() >= UnitServerEvents.maxPopulation && pop >= 6)
            return true;
        // a big army that stopped growing (out of supply, money or room) goes anyway: sitting at home wins nothing
        if (pop >= bot.plan.firstWavePop() && bot.tick - peakSince >= STALL_TICKS)
            return true;
        List<CombatSim.Fighter> known = bot.memory.recentEnemyFighters();
        if (!known.isEmpty() && pop >= 4)
            return CombatSim.fight(withAllies(main), known).margin() >= ATTACK_MARGIN;
        BotPlan plan = bot.plan;
        if (wavesSent == 0 && bot.minutesPlayed() >= plan.firstWaveMinutes() && pop >= 4)
            return true;
        return pop >= Math.min(UnitServerEvents.maxPopulation * 3 / 5, plan.firstWavePop() + plan.waveGrowth() * wavesSent);
    }

    // who goes with the wave besides the main army: with nothing that could hit the base, the healthy home army;
    // the raid army unless the main army beats their army without it (then it raids on its own)
    private List<LivingEntity> comingAlong() {
        List<LivingEntity> extra = new ArrayList<>();
        if (raidThreat(main()).isEmpty() && !enemyArmyNearHome())
            home().stream().filter(e -> e.getHealth() > e.getMaxHealth() * 0.7f).forEach(extra::add);
        if (!raid.isOut() && mainWeak)
            extra.addAll(raid());
        return extra;
    }

    // the wave sets off: whoever comes along joins the main army until it's back (retreat)
    private void lendToWave() {
        List<LivingEntity> extra = comingAlong();
        for (LivingEntity e : extra) {
            BotUnit u = bot.unit(e);
            u.lentFrom = u.squad;
            u.squad = BotUnit.Squad.MAIN;
        }
        if (!extra.isEmpty())
            bot.log("army: {} pop of the home and raid armies go with the wave", BotMath.popOf(extra));
    }

    // the wave is over (or never left): the lent units go back to their own armies
    private void returnLent() {
        for (LivingEntity e : bot.fighters) {
            BotUnit u = bot.unit(e);
            if (u.lentFrom != null) {
                if (u.squad == BotUnit.Squad.MAIN)
                    u.squad = u.lentFrom;
                u.lentFrom = null;
            }
        }
    }

    // newly trained or stragglers far from the fight wait at the rally point and join in groups, so they don't
    // walk into the enemy one at a time, heroes included
    private void reinforce(List<LivingEntity> main) {
        List<LivingEntity> front = main.stream().filter(e -> e.blockPosition().distSqr(target) < 40 * 40).toList();
        BlockPos joinAt = front.isEmpty() ? target : BotMath.centroid(front);
        List<LivingEntity> idleFar = idleAwayFrom(main, joinAt, 40);
        BlockPos rally = rallyPoint();
        // a fight is on: only the ones close by join it, the rest wait and come together after
        if (!enemiesNear(front.isEmpty() ? main : front).isEmpty()) {
            attackMove(idleFar.stream().filter(e -> e.blockPosition().distSqr(joinAt) < JOIN_RANGE * JOIN_RANGE).toList(), joinAt);
            bot.order(UnitAction.MOVE, idleAwayFrom(idleFar, rally, 10).stream()
                    .filter(e -> !bot.unit(e).fleeing && e.blockPosition().distSqr(joinAt) >= JOIN_RANGE * JOIN_RANGE).toList(), rally);
            return;
        }
        if (BotMath.popOf(idleFar) >= REINFORCE_POP || front.isEmpty()) {
            attackMove(idleFar, joinAt);
            return;
        }
        bot.order(UnitAction.MOVE, idleAwayFrom(idleFar, rally, 10).stream().filter(e -> !bot.unit(e).fleeing).toList(), rally);
    }

    // allies' troops near the fight are on our side
    boolean wins(List<LivingEntity> ours, List<LivingEntity> theirs, double margin) {
        List<LivingEntity> side = new ArrayList<>(ours);
        if (!theirs.isEmpty())
            for (LivingEntity a : bot.memory.alliesNear(Vec3.atCenterOf(BotMath.centroid(theirs)), FIGHT_RANGE))
                if (!side.contains(a))
                    side.add(a);
        return CombatSim.fight(BotFighters.of(side, true), BotFighters.of(theirs, true)).margin() >= margin;
    }

    // enemies the main army is up against now, neutral camps included
    private List<LivingEntity> enemiesNear(List<LivingEntity> squad) {
        if (squad.isEmpty())
            return List.of();
        BlockPos at = BotMath.centroid(squad);
        List<LivingEntity> list = new ArrayList<>(bot.memory.enemiesNear(Vec3.atCenterOf(at), FIGHT_RANGE));
        // and the ones seen there a moment ago that fog hides now: they're most likely still around
        for (LivingEntity e : bot.memory.rememberedNear(at, FIGHT_RANGE * 2))
            if (!list.contains(e))
                list.add(e);
        for (LivingEntity e : bot.memory.campUnitsNear(at, FIGHT_RANGE))
            if (!list.contains(e))
                list.add(e);
        return list;
    }

    // ---------------------------------------------------------------- places

    BlockPos rallyPoint() {
        Vec3 centre = new Vec3(bot.level.getWorldBorder().getCenterX(), 0, bot.level.getWorldBorder().getCenterZ());
        return BotMath.towards(bot.home, centre, 12, bot.home);
    }

    // the next patch the bot wants, else the hot zone
    private BlockPos holdPoint() {
        BlockPos exp = bot.expansion.wantsExpansion() ? bot.expansion.nextExpansion() : null;
        return exp != null ? exp : hotZone();
    }

    private static final double HOT_ZONE = 0.35; // how far from our team's bases towards the enemy the main army waits

    // where the enemy's army would pass to hit us or our allies: on the line from the middle of our team's bases to
    // the nearest enemy base, a third of the way. Waiting there, the main army meets an attack instead of walking
    // back for two minutes from wherever it stood
    private BlockPos hotZone() {
        double x = bot.home.getX(), z = bot.home.getZ();
        int n = 1;
        for (BuildingPlacement b : BuildingServerEvents.getBuildings())
            if (b.isCapitol && !b.isDestroyedServerside && !b.ownerName.equals(bot.name) && AlliancesServerEvents.isAllied(bot.name, b.ownerName)) {
                x += b.centrePos.getX();
                z += b.centrePos.getZ();
                n++;
            }
        Vec3 team = new Vec3(x / n, bot.home.getY(), z / n);
        BlockPos teamPos = BlockPos.containing(team);
        BlockPos enemy = bot.memory.enemyBuildings.values().stream().map(BotMemory.SeenBuilding::pos)
                .min(Comparator.comparingDouble(p -> p.distSqr(teamPos))).orElse(nearestEnemy(true));
        double dist = Math.sqrt(teamPos.distSqr(enemy));
        return dist < 32 ? frontPoint() : BotMath.towards(teamPos, Vec3.atCenterOf(enemy), dist * HOT_ZONE, frontPoint());
    }

    // their known building nearest home, else (when asked) the nearest enemy start, else the map centre
    private BlockPos nearestEnemy(boolean orStart) {
        var border = bot.level.getWorldBorder();
        BlockPos fallback = orStart && !bot.memory.unscoutedStarts.isEmpty() ? bot.memory.unscoutedStarts.get(0)
                : BlockPos.containing(border.getCenterX(), bot.home.getY(), border.getCenterZ());
        return bot.memory.enemyBuildings.values().stream().map(BotMemory.SeenBuilding::pos)
                .min(Comparator.comparingDouble(p -> p.distSqr(bot.home))).orElse(fallback);
    }

    // from home towards danger: towards every enemy start and known enemy capitol, away from our allies' capitols,
    // so the bot builds on the side its allies cover instead of the corridor the fights go through
    Vec3 enemyDirection() {
        Vec3 dir = Vec3.ZERO;
        List<BlockPos> enemies = new ArrayList<>(bot.memory.enemyStarts());
        bot.memory.enemyBuildings.values().stream().filter(BotMemory.SeenBuilding::isCapitol).forEach(b -> enemies.add(b.pos()));
        if (enemies.isEmpty())
            enemies.add(nearestEnemy(true));
        for (BlockPos e : enemies)
            dir = dir.add(flatDir(e));
        for (BuildingPlacement b : BuildingServerEvents.getBuildings())
            if (b.isCapitol && !b.ownerName.equals(bot.name) && AlliancesServerEvents.isAllied(bot.name, b.ownerName))
                dir = dir.subtract(flatDir(b.centrePos));
        return dir.lengthSqr() < 0.01 ? null : dir.normalize();
    }

    private Vec3 flatDir(BlockPos to) {
        Vec3 d = Vec3.atCenterOf(to).subtract(Vec3.atCenterOf(bot.home)).multiply(1, 0, 1);
        return d.lengthSqr() < 1 ? Vec3.ZERO : d.normalize();
    }

    // 8 blocks past our building nearest the enemy (their nearest known building, else the map centre)
    private BlockPos frontPoint() {
        BlockPos enemy = nearestEnemy(false);
        BlockPos ours = bot.buildings.stream().filter(b -> b.isBuilt).map(b -> b.centrePos)
                .min(Comparator.comparingDouble(p -> p.distSqr(enemy))).orElse(bot.home);
        return BotMath.towards(ours, Vec3.atCenterOf(enemy), 8, rallyPoint());
    }

    boolean isHolding(BlockPos pos) {
        List<LivingEntity> main = main();
        return state == State.RALLY && !main.isEmpty() && BotMath.isNear(main, pos, 16);
    }

    // the nearest neutral camp near our base, buildings or workers, or guarding resources we want
    private BotMemory.Camp campInTheWay() {
        BlockPos wanted = bot.expansion.campToClear;
        BotMemory.Camp best = null;
        campGaveUp.values().removeIf(until -> until < bot.tick);
        for (BotMemory.Camp c : bot.memory.camps.values()) {
            if (campGaveUp.keySet().stream().anyMatch(p -> p.distSqr(c.pos()) < 24 * 24))
                continue; // its centre drifts as the mobs move
            boolean inTheWay = c.pos().distSqr(bot.home) < CAMP_CLEAR_RANGE * CAMP_CLEAR_RANGE ||
                    (wanted != null && c.pos().distSqr(wanted) < 40 * 40) ||
                    bot.buildings.stream().anyMatch(b -> b.centrePos.distSqr(c.pos()) < 48 * 48) ||
                    bot.workers.stream().anyMatch(w -> w.blockPosition().distSqr(c.pos()) < 40 * 40);
            if (inTheWay && (best == null || c.pos().distSqr(bot.home) < best.pos().distSqr(bot.home)))
                best = c;
        }
        return best;
    }

    private static List<LivingEntity> idleAwayFrom(List<LivingEntity> units, BlockPos pos, int range) {
        return units.stream().filter(e -> BotBrain.isFree(e) && e.blockPosition().distSqr(pos) > (long) range * range).toList();
    }

    // every squad order goes through here; units that can't attack (witches) follow behind with a plain move
    boolean attackMove(List<LivingEntity> who, BlockPos pos) {
        List<LivingEntity> able = new ArrayList<>(), followers = new ArrayList<>();
        for (LivingEntity e : who) {
            if (bot.unit(e).fleeing || bot.defence.isHunting(e))
                continue;
            (e instanceof AttackerUnit ? able : followers).add(e);
        }
        // they'd walk past the fighters into the enemy: they keep behind them (BotMicro.keepWitchesBack)
        bot.order(UnitAction.MOVE, followers, able.isEmpty() ? pos : bot.micro.behind(able));
        return bot.order(UnitAction.ATTACK_MOVE, able, pos);
    }

    // ---------------------------------------------------------------- allies' pings

    // how the army answers an ally's ping (BotComms says it)
    enum Answer { COMING, JOINING, NO_ENEMY, NOTHING_THERE, TOO_STRONG, BUSY, DEFENDING }

    record Reply(Answer answer, int seconds) {}

    // an ally asks for help at pos: only if there's an enemy there (we see what our allies see), and only as many
    // units as the combat sim says beat it, nearest first. All of them makes it the main army's job for a while
    Reply answerHelp(BlockPos pos) {
        if (bot.defence.isDefending())
            return new Reply(Answer.DEFENDING, 0);
        List<LivingEntity> enemies = bot.memory.enemiesNear(Vec3.atCenterOf(pos), FIGHT_RANGE);
        if (enemies.isEmpty())
            return new Reply(Answer.NO_ENEMY, 0);
        if (state == State.RETREAT || state == State.DEFEND)
            return new Reply(Answer.BUSY, 0);
        List<LivingEntity> pool = new ArrayList<>(mainAway() ? home() : main());
        pool.sort(Comparator.comparingDouble(e -> e.blockPosition().distSqr(pos)));
        int n = BotMath.fewest(pool.size(), k -> wins(pool.subList(0, k), enemies, BotDefence.HOLD_MARGIN));
        if (n < 0 || pool.isEmpty())
            return new Reply(Answer.TOO_STRONG, 0);
        List<LivingEntity> group = pool.subList(0, Math.max(1, n));
        if (!mainAway() && group.size() == pool.size()) {
            state = State.SUPPORT;
            supportPos = pos;
            supportUntil = bot.tick + SUPPORT_TICKS;
        }
        attackMove(group, pos);
        return new Reply(Answer.COMING, 0);
    }

    // an ally asks us to attack pos: only an enemy (or a spot under fog that may hide one), and only when the
    // sim says we beat what's there and their army we know of; else how long until our wave is ready
    // an ally bot's army is walking to our hold point to leave with us, and isn't there yet
    private boolean alliesComing(BlockPos hold) {
        for (String ally : AlliancesServerEvents.getAllAllies(bot.name)) {
            BotBrain b = BotServerEvents.brain(ally);
            if (b != null && b.army.meetAt != null && bot.name.equals(b.army.meetWith) && !BotMath.isNear(b.army.main(), b.army.meetAt, 16))
                return true;
        }
        return false;
    }

    // walking to meet an ally bot's army: true while we still wait, until it sets off (or too long)
    private boolean meeting(List<LivingEntity> main) {
        if (meetAt == null)
            return false;
        BotBrain host = BotServerEvents.brain(meetWith);
        boolean hostLeft = host == null || host.army.state != State.RALLY || host.army.waveReadySince < 0;
        if (hostLeft || bot.tick - meetSince > MEET_TIMEOUT) {
            meetAt = null;
            attackMove(main, target);
            return false;
        }
        attackMove(idleAwayFrom(main, meetAt, 10), meetAt);
        return true;
    }

    // our army plus every ally fighter, as the combat sim sees them
    private List<CombatSim.Fighter> withAllies(List<LivingEntity> main) {
        List<LivingEntity> side = new ArrayList<>(main);
        for (LivingEntity a : bot.memory.allyArmy())
            if (!side.contains(a))
                side.add(a);
        return BotFighters.of(side, true);
    }

    // an ally attacks pos: with two enemies, hit the other one's base at the same time when we could hold their
    // whole army alone, so they can't both defend. Else the same target
    private BlockPos secondFront(List<LivingEntity> main, BlockPos pos) {
        BotMemory.SeenBuilding atPos = bot.memory.enemyBuildings.values().stream()
                .min(Comparator.comparingDouble(b -> b.pos().distSqr(pos))).orElse(null);
        if (atPos == null || CombatSim.fight(BotFighters.of(main, true), bot.memory.recentEnemyFighters()).margin() < 0)
            return pos;
        return bot.memory.enemyBuildings.values().stream()
                .filter(b -> !b.owner().equals(atPos.owner()) && b.pos().distSqr(pos) > 48 * 48)
                .min(Comparator.comparing((BotMemory.SeenBuilding b) -> !b.isCapitol()).thenComparingDouble(b -> b.pos().distSqr(bot.home)))
                .map(BotMemory.SeenBuilding::pos).orElse(pos);
    }

    Reply answerAttack(BlockPos pos, String sender) {
        if (bot.defence.isDefending())
            return new Reply(Answer.DEFENDING, 0);
        Vec3 at = Vec3.atCenterOf(pos);
        boolean enemyThere = !bot.memory.enemiesNear(at, FIGHT_RANGE).isEmpty() ||
                bot.memory.enemyBuildings.values().stream().anyMatch(b -> b.pos().distSqr(pos) < FIGHT_RANGE * FIGHT_RANGE);
        if (!enemyThere && bot.memory.isSeen(pos))
            return new Reply(Answer.NOTHING_THERE, 0);
        List<LivingEntity> main = main();
        if (state == State.ATTACK) {
            pingTarget = pos;
            return new Reply(Answer.JOINING, 0);
        }
        if (state != State.RALLY)
            return new Reply(Answer.BUSY, 0);
        List<CombatSim.Fighter> theirs = bot.memory.fightersNear(at, FIGHT_RANGE * 2);
        if (theirs.isEmpty())
            theirs = bot.memory.recentEnemyFighters(); // their army will come to defend it
        // the ally attacking counts on our side: together we take what neither would alone
        if (!main.isEmpty() && CombatSim.fight(withAllies(main), theirs).margin() >= BotDefence.HOLD_MARGIN) {
            BlockPos go = secondFront(main, pos);
            // same target and the ally bot is still grouping: meet its army first so both arrive together
            BotBrain host = go == pos ? BotServerEvents.brain(sender) : null;
            BlockPos meet = host != null && host.army.waveReadySince >= 0 ? host.army.lastHold : null;
            if (attackMove(main, meet != null ? meet : go)) {
                if (go == pos)
                    pingTarget = pos;
                setTarget(go);
                startAttack(main); // not one of our waves: the next wave's size stays
                meetAt = meet;
                meetWith = sender;
                meetSince = bot.tick;
                bot.log("army: joins an ally's attack {}", go == pos ? "on the same target" : "on a second front " + bot.fromHome(go) + " from home");
                return new Reply(Answer.JOINING, 0);
            }
        }
        int pop = BotMath.popOf(main);
        return new Reply(Answer.TOO_STRONG, Math.max(15, (bot.plan.firstWavePop() - pop) * 12)); // ponytail: ~12 s per pop
    }
}
