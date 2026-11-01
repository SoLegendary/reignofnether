package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.EnchantAbility;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitRole;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.WitchUnit;
import net.minecraft.core.BlockPos;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.Vec3;

import java.util.*;

// What a player does with single units mid-fight, from the bot's combat budget: a bot isn't smarter than a player,
// it's faster, so it micros what a player can't keep up with. Units dodge creepers and potion clouds, ranged units
// focus fire the weakest enemy they can reach, ranged units kite melee, badly hurt units step back to a healer
// (or home), witches stay behind their army, ravagers carry a pillager and siege. Plus the "set and forget"
// clicks: researched autocasts, witch potions, the Library's enchant.
public class BotMicro {

    private static final int FIGHT_RANGE = 14;
    private static final int DODGE_RANGE = 5; // a creeper's blast
    private static final int WITCH_BEHIND = 5, WITCH_EVERY = 2 * 20;
    private static final int HEALER_RANGE = 24;
    private static final float HEALED = 0.7f;
    private static final int SIEGE_MELEE = 8;
    private static final int KITE_DISTANCE = 6, KITE_STEP = 5;
    private static final float HURT = 0.25f;
    private static final int ENCHANT_TRIP_EVERY = 10 * 20;
    private static final int ENCHANT_TRIP_SIZE = 3;
    // researched abilities the bot uses through their autocast, since it never clicks them itself
    private static final Map<UnitAction, ProductionItem> AUTOCASTS = Map.of(
            UnitAction.CAST_SUMMON_VEXES_AUTOCAST_ENABLE, ProductionItems.RESEARCH_EVOKER_VEXES);

    private final BotBrain bot;
    private long lastEnchantTrip = 0;
    private final Map<BotUnit.Squad, Long> witchOrdered = new EnumMap<>(BotUnit.Squad.class);
    private boolean healsKnown = false;

    BotMicro(BotBrain bot) {
        this.bot = bot;
    }

    void think() {
        enableAutocasts();
        splitWitches();
        libraryEnchant();
        dodge(); // a blast first: nothing else matters to a unit about to be in it
        ravagers();
        keepWitchesBack();
        focusFire();
        BlockPos rally = bot.army.rallyPoint();
        for (LivingEntity e : bot.fighters) {
            if (!bot.hasActions(1))
                return;
            BotUnit u = bot.unit(e);
            if (BotBrain.isHero(e) || e.isPassenger())
                continue; // heroes have their own (BotHeroes); a rider goes where its ravager goes
            if (u.fleeing) {
                healUp(e, u);
                continue;
            }
            List<LivingEntity> enemies = bot.memory.enemiesNear(e.position(), FIGHT_RANGE);
            if (!enemies.isEmpty() && !stepBack(e, rally))
                kite(e, enemies);
        }
    }

    // ---------------------------------------------------------------- dodging

    // a creeper about to blow, or a cloud of their potion: everyone in it steps out, away from its middle
    private void dodge() {
        for (LivingEntity t : bot.memory.visibleEnemies)
            if (t instanceof Creeper c && c.getSwellDir() > 0)
                stepOut(c.position(), DODGE_RANGE);
        // round the main army and round home: where our units are
        Set<AreaEffectCloud> clouds = new HashSet<>();
        List<LivingEntity> main = bot.army.main();
        for (BlockPos at : main.isEmpty() ? List.of(bot.home) : List.of(bot.home, BotMath.centroid(main)))
            clouds.addAll(bot.level.getEntitiesOfClass(AreaEffectCloud.class, new AABB(at).inflate(48, 16, 48),
                    c -> c.getOwner() instanceof Unit u && bot.memory.isEnemy(u.getOwnerName())));
        for (AreaEffectCloud cloud : clouds)
            stepOut(cloud.position(), cloud.getRadius() + 1);
    }

    private void stepOut(Vec3 from, double radius) {
        List<LivingEntity> in = bot.units.stream().filter(e -> !e.isPassenger() && e.position().distanceToSqr(from) < radius * radius).toList();
        if (in.isEmpty())
            return;
        BlockPos at = BlockPos.containing(from);
        bot.order(UnitAction.MOVE, in, BotMath.towards(at, Vec3.atCenterOf(BotMath.centroid(in)), radius + 3,
                BotMath.towards(at, Vec3.atCenterOf(bot.home), radius + 3, bot.home)));
    }

    // ---------------------------------------------------------------- focus fire

    // ranged units shoot the same enemy, the weakest one among those any of them can reach without walking: kills
    // come sooner and nobody walks past their own melee into the enemy army. One order per target
    private void focusFire() {
        Map<LivingEntity, List<LivingEntity>> shots = new LinkedHashMap<>();
        for (LivingEntity e : bot.fighters) {
            if (!BotBrain.has(e, UnitRole.RANGED) || !(e instanceof AttackerUnit a) || BotBrain.isHero(e) || bot.unit(e).fleeing)
                continue;
            int reach = (int) Math.ceil(a.getAttackRange() + 1);
            List<LivingEntity> inReach = bot.memory.enemiesNear(e.position(), reach).stream()
                    .filter(t -> CombatSim.dpsAgainst(BotFighters.of(e, true), BotFighters.of(t, true), 1) > 0).toList();
            if (inReach.isEmpty())
                continue;
            LivingEntity t = shots.keySet().stream().filter(inReach::contains).min(Comparator.comparingDouble(LivingEntity::getHealth))
                    .orElseGet(() -> inReach.stream().min(Comparator.comparingDouble(LivingEntity::getHealth)).orElseThrow());
            if (((Unit) e).getTargetGoal().getTarget() != t)
                shots.computeIfAbsent(t, k -> new ArrayList<>()).add(e);
            else
                shots.computeIfAbsent(t, k -> new ArrayList<>());
        }
        shots.forEach((t, who) -> {
            if (!who.isEmpty())
                bot.orderTarget(UnitAction.ATTACK, who, t);
        });
    }

    // ---------------------------------------------------------------- witches

    // witches have no attack, only potions: they stay a few blocks behind their own army's fighters, throwing over
    // their heads, instead of walking into the enemy with them. Every 2 s per army
    private void keepWitchesBack() {
        for (BotUnit.Squad squad : List.of(BotUnit.Squad.MAIN, BotUnit.Squad.HOME, BotUnit.Squad.RAID)) {
            if (bot.tick - witchOrdered.getOrDefault(squad, -1_000_000L) < WITCH_EVERY)
                continue;
            List<LivingEntity> attackers = new ArrayList<>(), witches = new ArrayList<>();
            for (LivingEntity e : bot.fighters)
                if (bot.unit(e).squad == squad && !bot.unit(e).fleeing && !BotBrain.isHero(e))
                    (e instanceof AttackerUnit ? attackers : witches).add(e);
            if (attackers.isEmpty() || witches.isEmpty())
                continue;
            BlockPos spot = behind(attackers);
            List<LivingEntity> go = witches.stream().filter(w -> w.blockPosition().distSqr(spot) > 4 * 4).toList();
            if (bot.order(UnitAction.MOVE, go, spot))
                witchOrdered.put(squad, bot.tick);
        }
    }

    // a few blocks behind these units, on the side towards home
    BlockPos behind(List<LivingEntity> units) {
        BlockPos at = BotMath.centroid(units);
        return BotMath.towards(at, Vec3.atCenterOf(bot.home), WITCH_BEHIND, at);
    }

    // ---------------------------------------------------------------- ravagers

    // with Ravager Cavalry, a free pillager rides each ravager; a ridden ravager sieges (long range explosive arrows,
    // can't move) when enemy buildings or a group are in reach and no melee is close, and gets up again when melee
    // closes in, nothing is left in reach, or the army pulls back
    private void ravagers() {
        boolean cavalry = ResearchServerEvents.playerHasResearch(bot.name, ProductionItems.RESEARCH_RAVAGER_CAVALRY);
        for (LivingEntity e : bot.fighters) {
            if (!(e instanceof RavagerUnit r) || r.isSiegeTransitioning())
                continue;
            if (!r.hasPillagerRider()) {
                if (!cavalry || r.isSiegeLocked())
                    continue;
                bot.fighters.stream().filter(f -> f instanceof PillagerUnit && !f.isPassenger() && !bot.unit(f).fleeing &&
                                bot.unit(f).squad == bot.unit(e).squad && f.distanceToSqr(e) < 24 * 24)
                        .min(Comparator.comparingDouble(f -> f.distanceToSqr(e)))
                        .ifPresent(p -> bot.orderTarget(UnitAction.MOUNT_RAVAGER, List.of(p), e));
                continue;
            }
            float reach = ((PillagerUnit) r.getFirstPassenger()).getSiegeAttackRange();
            boolean meleeClose = bot.memory.enemiesNear(e.position(), SIEGE_MELEE).stream()
                    .anyMatch(t -> t instanceof AttackerUnit && !BotBrain.has(t, UnitRole.RANGED) && !BotBrain.has(t, UnitRole.FLYER));
            boolean inReach = bot.memory.enemiesNear(e.position(), (int) reach).size() >= 3 ||
                    bot.memory.enemyBuildings.values().stream().anyMatch(b -> b.pos().distSqr(e.blockPosition()) < reach * reach);
            boolean pullingBack = bot.army.state == BotArmy.State.RETREAT;
            boolean sieged = r.getSiegeState() == RavagerUnit.SiegeState.SIEGED;
            boolean want = inReach && !meleeClose && !pullingBack;
            if (sieged != want && (sieged || r.canStartSiege()) && BotHeroes.ready(e, UnitAction.TOGGLE_SIEGE_MODE) != null)
                bot.order(UnitAction.TOGGLE_SIEGE_MODE, List.of(e), e.blockPosition());
        }
    }

    // a badly hurt unit steps back to a healing witch nearby, and comes back once healed: the enemy loses its target
    // and we keep the unit. With no healer, it leaves the attack for good and joins the home army: stepping back only
    // for the next attack order to send it in again just wastes it. The home army fights to the end, it's the last line
    private boolean stepBack(LivingEntity e, BlockPos rally) {
        BotUnit u = bot.unit(e);
        if (e.getHealth() >= e.getMaxHealth() * HURT || e.getMaxHealth() < 30)
            return false;
        LivingEntity healer = healerNear(e);
        if (healer != null) {
            if (!bot.order(UnitAction.MOVE, List.of(e), healer.blockPosition()))
                return false;
            u.fleeing = true; // the army leaves it alone until it's healed (healUp)
            return true;
        }
        if (u.squad == BotUnit.Squad.HOME || e.blockPosition().distSqr(rally) <= 12 * 12)
            return false;
        if (!bot.order(UnitAction.MOVE, List.of(e), rally))
            return false;
        u.squad = BotUnit.Squad.HOME;
        u.lentFrom = null;
        return true;
    }

    @org.jetbrains.annotations.Nullable
    private LivingEntity healerNear(LivingEntity e) {
        return bot.fighters.stream().filter(w -> w != e && w instanceof WitchUnit && Boolean.TRUE.equals(bot.unit(w).witchHeals) &&
                w.distanceToSqr(e) < HEALER_RANGE * HEALER_RANGE).min(Comparator.comparingDouble(w -> w.distanceToSqr(e))).orElse(null);
    }

    // stepped back to a healer: stays by it until healed, then the army picks it up again. Healer gone: home
    private void healUp(LivingEntity e, BotUnit u) {
        LivingEntity healer = healerNear(e);
        if (e.getHealth() > e.getMaxHealth() * HEALED || healer == null) {
            u.fleeing = false;
            return;
        }
        if (e.distanceToSqr(healer) > 6 * 6)
            bot.order(UnitAction.MOVE, List.of(e), healer.blockPosition());
    }

    // ranged units step away from melee and shoot again: a crossbow while it reloads (it slows its holder), any other
    // ranged unit when melee is on top of it and it's at least as fast, so it can actually get away
    private boolean kite(LivingEntity e, List<LivingEntity> enemies) {
        if (!BotBrain.has(e, UnitRole.RANGED) || !(e instanceof AttackerUnit))
            return false;
        LivingEntity melee = enemies.stream().filter(t -> !BotBrain.has(t, UnitRole.RANGED) && !BotBrain.has(t, UnitRole.FLYER))
                .min(Comparator.comparingDouble(t -> t.distanceToSqr(e))).orElse(null);
        if (!(e instanceof PillagerUnit p)) {
            if (melee == null || melee.distanceTo(e) >= 3 || bot.unit(e).sinceOrdered(bot.tick) < 20 ||
                    e.getAttributeValue(Attributes.MOVEMENT_SPEED) < melee.getAttributeValue(Attributes.MOVEMENT_SPEED))
                return false;
            Vec3 away = e.position().subtract(melee.position()).multiply(1, 0, 1);
            return bot.order(UnitAction.MOVE, List.of(e), BlockPos.containing(e.position().add(away.normalize().scale(KITE_STEP))));
        }
        boolean loaded = CrossbowItem.isCharged(p.getMainHandItem()) && !p.isChargingCrossbow();
        if (melee != null && !loaded && melee.distanceTo(e) < KITE_DISTANCE) {
            Vec3 away = e.position().subtract(melee.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 0.01)
                away = Vec3.atCenterOf(bot.home).subtract(e.position());
            return bot.order(UnitAction.MOVE, List.of(e), BlockPos.containing(e.position().add(away.normalize().scale(KITE_STEP))));
        }
        if (loaded && ((Unit) e).isIdle()) {
            LivingEntity t = BotMath.closest(e, enemies);
            return t != null && bot.orderTarget(UnitAction.ATTACK, List.of(e), t);
        }
        return false;
    }

    // ---------------------------------------------------------------- set and forget

    private void enableAutocasts() {
        for (LivingEntity e : bot.fighters) {
            BotUnit s = bot.unit(e);
            if (s.autocastsOn || !(e instanceof Unit u))
                continue;
            boolean pending = false;
            for (Ability a : u.getAbilities().get()) {
                ProductionItem needs = a.autocastEnableAction == null ? null : AUTOCASTS.get(a.autocastEnableAction);
                if (needs == null || a.isAutocasting(u))
                    continue;
                if (!ResearchServerEvents.playerHasResearch(bot.name, needs) || !bot.order(a.autocastEnableAction, List.of(e), e.blockPosition()))
                    pending = true;
            }
            s.autocastsOn = !pending;
        }
    }

    // witches have no attack, only potions: half of them throw healing, the rest harming. A new witch takes the
    // side that's short, so deaths don't skew it. Against undead, harming heals them: all witches heal
    private void splitWitches() {
        // healing potions need their research: until then every witch throws harming, then they split again
        boolean canHeal = ResearchServerEvents.playerHasResearch(bot.name, ProductionItems.RESEARCH_HEALING_POTIONS);
        if (canHeal && !healsKnown)
            for (LivingEntity e : bot.fighters)
                bot.unit(e).witchHeals = null;
        healsKnown = canHeal;
        boolean undead = bot.memory.recentEnemyArmy().stream().filter(e -> BotBrain.has(e, UnitRole.UNDEAD)).count() * 2 >
                bot.memory.recentEnemyArmy().size();
        long healers = 0, total = 0;
        for (LivingEntity e : bot.fighters)
            if (e instanceof WitchUnit && bot.unit(e).witchHeals != null) {
                total++;
                if (bot.unit(e).witchHeals)
                    healers++;
            }
        for (LivingEntity e : bot.fighters) {
            BotUnit s = bot.unit(e);
            if (!(e instanceof WitchUnit))
                continue;
            boolean heals = canHeal && (undead || (s.witchHeals != null ? s.witchHeals : healers * 2 <= total));
            if (s.witchHeals != null && s.witchHeals == heals)
                continue;
            UnitAction autocast = heals ? UnitAction.THROW_HEALING_POTION_AUTOCAST_ENABLE : UnitAction.THROW_LINGERING_HARMING_POTION_AUTOCAST_ENABLE;
            if (bot.order(autocast, List.of(e), e.blockPosition())) {
                if (s.witchHeals == null) {
                    total++;
                    if (heals)
                        healers++;
                }
                s.witchHeals = heals;
            }
        }
    }

    // the matchup's Library enchant (Multishot against monsters) is set to autocast once the Library is upgraded,
    // and while the army waits between waves, units without it walk past the Library a few at a time
    private void libraryEnchant() {
        if (bot.plan.library() == null || bot.matchup.libraryEnchants().isEmpty())
            return;
        BuildingPlacement library = bot.buildings.stream()
                .filter(b -> b.isBuilt && b.getBuilding() == bot.plan.library() && b.getUpgradeLevel() > 0).findFirst().orElse(null);
        if (library == null)
            return;
        EnchantAbility enchant = null;
        for (UnitAction action : bot.matchup.libraryEnchants())
            for (Ability a : library.getAbilities())
                if (a.action == action && a instanceof EnchantAbility ea && enchant == null)
                    enchant = ea;
        if (enchant == null)
            return;
        if (!Library.isAutoCasting(library, enchant) && bot.spendAction()) {
            Library.toggleAutoCast(library, enchant);
            bot.log("library: autocasting {}", enchant.action);
        }
        if (bot.army.state != BotArmy.State.RALLY || bot.defence.isDefending() || bot.tick - lastEnchantTrip < ENCHANT_TRIP_EVERY)
            return;
        EnchantAbility ea = enchant;
        List<LivingEntity> need = bot.fighters.stream()
                .filter(e -> ea.isCorrectUnitAndEquipment(e) && !ea.hasSameEnchant(e) &&
                        e.blockPosition().distSqr(library.centrePos) > ea.range * ea.range)
                .limit(ENCHANT_TRIP_SIZE).toList();
        if (!need.isEmpty() && bot.order(UnitAction.MOVE, need, library.centrePos))
            lastEnchantTrip = bot.tick;
    }
}
