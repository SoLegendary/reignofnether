package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.startpos.StartPos;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.unit.UnitRole;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// What the bot knows about its enemies. Everything comes from its own units' and buildings' sight ranges, the
// same way a player sees through fog of war - the bot never reads enemy positions it couldn't see.
public class BotMemory {

    public record SeenBuilding(BlockPos pos, boolean isCapitol, long lastSeenTick, String owner) {}
    // neutral units guarding a spot (owner ""), grouped by 16-block cell
    public record Camp(BlockPos pos, List<LivingEntity> units) {}

    private final String ownerName;
    public final Map<BlockPos, SeenBuilding> enemyBuildings = new HashMap<>();
    public final List<LivingEntity> visibleEnemies = new ArrayList<>();
    private final Set<Integer> visibleIds = new HashSet<>();
    private final List<LivingEntity> allyFighters = new ArrayList<>(); // our allies' troops: they fight beside us
    public final List<BlockPos> unscoutedStarts = new ArrayList<>();
    private final List<BlockPos> enemyStarts = new ArrayList<>();
    private final Map<BlockPos, Integer> startColour = new HashMap<>(); // the team that starts there
    private static final long UNIT_MEMORY_TICKS = 3 * 60 * 20;
    private final Map<Integer, LivingEntity> seenUnits = new HashMap<>(); // what the enemy army looked like lately
    private final Map<Integer, Long> seenUnitTick = new HashMap<>();
    private final Map<Integer, BlockPos> seenUnitPos = new HashMap<>(); // where we last saw it, not where it is now
    private final List<LivingEntity> recentArmy = new ArrayList<>(); // the fighters among seenUnits, rebuilt each update
    public final Map<Long, Camp> camps = new HashMap<>();

    private record Eye(double x, double z, double rangeSqr) {}
    private List<Eye> eyes = List.of();
    private boolean seesAll = false; // fog of war is off: players see the whole map, so the bot does too

    public BotMemory(String ownerName, BlockPos home) {
        this.ownerName = ownerName;
        // start positions are public map knowledge; teams share a start colour, so skip our own team's starts
        Set<Integer> friendlyColours = new HashSet<>();
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers)
                if (p.name.equals(ownerName) || AlliancesServerEvents.isAllied(ownerName, p.name))
                    friendlyColours.add(p.startPosColorId);
        }
        // closed seats stay in the list but disabled; nobody starts there. Who sat where is cleared once the match
        // starts (StartPosServerEvents.reset), so an open seat nobody took is checked once too
        for (StartPos sp : StartPosServerEvents.startPoses)
            if (sp.enabled && !friendlyColours.contains(sp.colorId) && sp.pos.distSqr(home) > 32 * 32) {
                unscoutedStarts.add(sp.pos);
                startColour.put(sp.pos, sp.colorId);
            }
        unscoutedStarts.sort(Comparator.comparingDouble(p -> p.distSqr(home)));
        enemyStarts.addAll(unscoutedStarts);
    }

    // an enemy team still starts there (alliances can change mid-match): who sat on a seat is cleared at match start, its colour stays
    private boolean stillPlaying(BlockPos start) {
        Integer colour = startColour.get(start);
        if (colour == null)
            return true;
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers)
                if (p.startPosColorId == colour && isEnemy(p.name))
                    return true;
        }
        return false;
    }

    // the army reached a target and found nothing: drop whatever we remembered there so it picks a new one
    public void forget(BlockPos pos) {
        enemyBuildings.values().removeIf(sb -> sb.pos.distSqr(pos) < 12 * 12);
        unscoutedStarts.remove(pos);
    }

    public List<BlockPos> enemyStarts() {
        return enemyStarts;
    }

    public boolean isEnemyStart(BlockPos pos) {
        return enemyStarts.contains(pos);
    }

    private boolean isFriendly(String otherOwner) {
        return ownerName.equals(otherOwner) || AlliancesServerEvents.isAllied(ownerName, otherOwner);
    }

    public boolean isEnemy(String otherOwner) {
        return otherOwner != null && !otherOwner.isBlank() && !otherOwner.equals(ownerName)
                && !AlliancesServerEvents.isAllied(ownerName, otherOwner);
    }

    // allies share vision in RoN, so what our allies see counts as seen (same as for a human player)
    public void update(long tick) {
        seesAll = !FogOfWarServerEvents.isEnabled();
        List<Eye> eyes = new ArrayList<>();
        allyFighters.clear();
        for (LivingEntity e : UnitServerEvents.getAllUnits()) {
            if (e.isAlive() && e instanceof Unit u && isFriendly(u.getOwnerName())) {
                int r = u.getSightRange();
                eyes.add(new Eye(e.getX(), e.getZ(), (double) r * r));
                if (!u.getOwnerName().equals(ownerName) && !u.getUnitRoles().contains(UnitRole.WORKER) && !u.getUnitRoles().contains(UnitRole.SCOUT))
                    allyFighters.add(e);
            }
        }
        for (BuildingPlacement b : BuildingServerEvents.getBuildings()) {
            if (isFriendly(b.ownerName)) {
                int r = b.getSightRange();
                eyes.add(new Eye(b.centrePos.getX(), b.centrePos.getZ(), (double) r * r));
            }
        }
        this.eyes = eyes;

        // one pass over every unit we can see: enemies, plus neutrals (camps, wild llamas) that count as enemies
        // while they're attacking us or our allies, and armed neutrals grouped into camps
        visibleEnemies.clear();
        visibleIds.clear();
        Map<Long, List<LivingEntity>> neutralsByCell = new HashMap<>();
        for (LivingEntity e : UnitServerEvents.getAllUnits()) {
            if (!e.isAlive() || !(e instanceof Unit u))
                continue;
            boolean neutral = u.getOwnerName().isBlank();
            if ((!neutral && !isEnemy(u.getOwnerName())) || !seen(e.getX(), e.getZ()))
                continue;
            if (!neutral || attacksUs(e))
                visibleEnemies.add(e);
            if (neutral && e instanceof AttackerUnit)
                neutralsByCell.computeIfAbsent(BlockPos.asLong(e.getBlockX() >> 4, 0, e.getBlockZ() >> 4), k -> new ArrayList<>()).add(e);
        }
        for (LivingEntity e : visibleEnemies) {
            visibleIds.add(e.getId());
            seenUnits.put(e.getId(), e);
            seenUnitTick.put(e.getId(), tick);
            seenUnitPos.put(e.getId(), e.blockPosition());
        }
        seenUnits.entrySet().removeIf(en -> !en.getValue().isAlive() || tick - seenUnitTick.get(en.getKey()) > UNIT_MEMORY_TICKS);
        seenUnitTick.keySet().retainAll(seenUnits.keySet());
        seenUnitPos.keySet().retainAll(seenUnits.keySet());

        // neutral camps: remember where armed neutrals stand; forget a camp once we see its spot empty
        camps.entrySet().removeIf(en -> !neutralsByCell.containsKey(en.getKey()) &&
                seen(en.getValue().pos.getX(), en.getValue().pos.getZ()));
        neutralsByCell.forEach((cell, list) -> camps.put(cell, new Camp(BotMath.centroid(list), list)));

        Set<BlockPos> stillStanding = new HashSet<>();
        for (BuildingPlacement b : BuildingServerEvents.getBuildings()) {
            if (!isEnemy(b.ownerName) || b.isDestroyedServerside) // a destroyed building can linger in the list
                continue;
            stillStanding.add(b.originPos);
            if (seen(b.centrePos.getX(), b.centrePos.getZ()))
                enemyBuildings.put(b.originPos, new SeenBuilding(b.centrePos, b.isCapitol, tick, b.ownerName));
        }
        // forget remembered buildings once we look at their spot and they're gone, or their owner became an ally
        enemyBuildings.entrySet().removeIf(e -> !isEnemy(e.getValue().owner()) || (!stillStanding.contains(e.getKey()) &&
                seen(e.getValue().pos.getX(), e.getValue().pos.getZ())));
        seenUnits.values().removeIf(e -> !(e instanceof Unit u) || !isEnemy(u.getOwnerName()) && !u.getOwnerName().isBlank());
        recentArmy.clear();
        for (LivingEntity e : seenUnits.values())
            if (e instanceof Unit u && !u.getUnitRoles().contains(UnitRole.WORKER) && !u.getUnitRoles().contains(UnitRole.SCOUT) &&
                    !u.getUnitRoles().contains(UnitRole.SUMMON))
                recentArmy.add(e);
        // a start position counts as scouted once anything of ours has seen it; defeated players' starts are dropped
        unscoutedStarts.removeIf(p -> seen(p.getX(), p.getZ()) || !stillPlaying(p));
        enemyStarts.removeIf(p -> !stillPlaying(p));
    }

    // a neutral counts as an enemy while it attacks one of our (or our allies') units or buildings
    private boolean attacksUs(LivingEntity e) {
        if (e instanceof Mob m && m.getTarget() instanceof Unit t && isFriendly(t.getOwnerName()))
            return true;
        BuildingPlacement b = BotBrain.buildingTarget(e);
        return b != null && isFriendly(b.ownerName);
    }

    public boolean isSeen(BlockPos p) {
        return seen(p.getX(), p.getZ());
    }

    private boolean seen(double x, double z) {
        if (seesAll)
            return true;
        for (Eye eye : eyes) {
            double dx = eye.x - x, dz = eye.z - z;
            if (dx * dx + dz * dz <= eye.rangeSqr)
                return true;
        }
        return false;
    }

    public List<LivingEntity> enemiesNear(Vec3 pos, double range) {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : visibleEnemies)
            if (e.isAlive() && e.position().distanceToSqr(pos) <= range * range)
                list.add(e);
        return list;
    }

    public boolean nearCamp(BlockPos p, int range) {
        for (Camp c : camps.values())
            if (c.pos.distSqr(p) < (long) range * range)
                return true;
        return false;
    }

    // a camp on a cell border is stored as two halves, so a fight with it takes every camp nearby
    public List<LivingEntity> campUnitsNear(BlockPos p, int range) {
        List<LivingEntity> list = new ArrayList<>();
        for (Camp c : camps.values())
            if (c.pos.distSqr(p) < (long) range * range)
                for (LivingEntity e : c.units)
                    if (e.isAlive())
                        list.add(e);
        return list;
    }

    // the enemy fighters seen in the last few minutes: what we think their army is (read-only)
    public List<LivingEntity> recentEnemyArmy() {
        return Collections.unmodifiableList(recentArmy);
    }

    // the enemy army as the combat sim sees it: units in sight with their health, the rest remembered at full health
    // every ally fighter we know of, wherever it is
    public List<LivingEntity> allyArmy() {
        return Collections.unmodifiableList(allyFighters);
    }

    public List<CombatSim.Fighter> recentEnemyFighters() {
        List<CombatSim.Fighter> list = new ArrayList<>();
        for (LivingEntity e : recentEnemyArmy())
            list.add(BotFighters.of(e, visibleIds.contains(e.getId())));
        return list;
    }

    // where a remembered enemy was last in sight (its real position now may be under fog)
    public BlockPos lastSeenPos(LivingEntity e) {
        return seenUnitPos.getOrDefault(e.getId(), e.blockPosition());
    }

    public List<LivingEntity> alliesNear(Vec3 pos, double range) {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : allyFighters)
            if (e.isAlive() && e.position().distanceToSqr(pos) <= range * range)
                list.add(e);
        return list;
    }

    // enemy troops last seen around pos, in sight or not: what we'd likely meet there. One out of sight whose last
    // spot we're looking at now has left, so it doesn't count
    public List<LivingEntity> rememberedNear(BlockPos pos, int range) {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : recentEnemyArmy()) {
            BlockPos last = lastSeenPos(e);
            if (last.distSqr(pos) < (long) range * range && (visibleIds.contains(e.getId()) || !isSeen(last)))
                list.add(e);
        }
        return list;
    }

    // where enemy troops were seen in the last minute: no building goes up there
    public List<BlockPos> dangerSpots(long tick) {
        List<BlockPos> list = new ArrayList<>();
        for (LivingEntity e : recentEnemyArmy())
            if (tick - seenUnitTick.getOrDefault(e.getId(), 0L) < 60 * 20)
                list.add(lastSeenPos(e));
        return list;
    }

    public int recentEnemyArmyPop() {
        return BotMath.popOf(recentEnemyArmy());
    }

    // enemies near pos as the combat sim sees them (in sight only)
    public List<CombatSim.Fighter> fightersNear(Vec3 pos, double range) {
        return BotFighters.of(enemiesNear(pos, range), true);
    }

    static double attackWeight(double distance, boolean isCapitol) {
        return (isCapitol ? 2.0 : 1.0) / Math.max(16, distance);
    }

    // index picked with probability proportional to its weight; roll is in [0, 1)
    static int weightedPick(List<Double> weights, double roll) {
        double total = 0;
        for (double w : weights)
            total += w;
        double at = roll * total;
        for (int i = 0; i < weights.size(); i++) {
            at -= weights.get(i);
            if (at < 0)
                return i;
        }
        return weights.size() - 1;
    }

    // where to send an attack. The current target holds while it still stands; otherwise a random remembered
    // building, closer ones and capitols likelier, so attacks don't always hit the same spot. With nothing
    // known, the next start position nobody has looked at yet.
    public BlockPos getAttackTarget(BlockPos from, RandomSource random, @Nullable BlockPos current) {
        List<BlockPos> known = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (SeenBuilding sb : enemyBuildings.values()) {
            if (sb.pos.equals(current))
                return current;
            known.add(sb.pos);
            weights.add(attackWeight(Math.sqrt(sb.pos.distSqr(from)), sb.isCapitol));
        }
        if (!known.isEmpty())
            return known.get(weightedPick(weights, random.nextDouble()));
        // nothing left to look at: enemies may have moved, so check every enemy start again
        if (unscoutedStarts.isEmpty())
            unscoutedStarts.addAll(enemyStarts);
        return unscoutedStarts.isEmpty() ? null : unscoutedStarts.get(0);
    }
}
