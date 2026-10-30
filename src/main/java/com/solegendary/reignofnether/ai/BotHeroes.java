package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// What every hero does: trained first (it's the early army), revived when it falls, levelled in the plan's skill
// order, run home when badly hurt, sent shopping (BotMarket), and its abilities cast by what they target. What only
// one hero does lives in its own HeroBot (RoyalGuardBot, EnchanterBot).
public class BotHeroes {

    private static final float FLEE_HP = 0.3f, RETURN_HP = 0.7f;
    private static final int MIN_ARMY_FOR_SECOND_HERO = 4;
    private static final int FLEE_NO_HEAL_TICKS = 30 * 20; // villager heroes don't regenerate: go back anyway
    private static final int CAST_RANGE = 16, MIN_ENEMIES_SELF_CAST = 3;

    // the shared casting, by what an ability aims at
    private static final Set<UnitAction> ENEMY_TARGET = EnumSet.of(UnitAction.MACE_SLAM, UnitAction.INSOMNIA_CURSE,
            UnitAction.THROW_TNT, UnitAction.SCORCHING_GAZE, UnitAction.MOLTEN_BOMB);
    private static final Set<UnitAction> FRIENDLY_TARGET = EnumSet.of(UnitAction.CIVIL_ENCHANTMENT,
            UnitAction.MARTIAL_ENCHANTMENT);
    private static final Set<UnitAction> SELF_IN_COMBAT = EnumSet.of(UnitAction.TAUNTING_CRY, UnitAction.RAISE_DEAD,
            UnitAction.CHILLING_SCREECH, UnitAction.AVATAR, UnitAction.BLIZZARD, UnitAction.LOOT_EXPLOSION, UnitAction.SOULS_AFLAME);

    private final BotBrain bot;
    final BotMarket market;

    BotHeroes(BotBrain bot) {
        this.bot = bot;
        this.market = new BotMarket(bot);
    }

    // cooldown and mana are only charged when a cast completes, and any new order restarts the channel,
    // so nothing may order a hero while it's casting
    static boolean busyCasting(LivingEntity e) {
        return e instanceof Unit u && u.isCasting();
    }

    void think() {
        for (LivingEntity e : bot.fighters) {
            if (!(e instanceof HeroUnit hero))
                continue;
            spendSkillPoints(hero);
            if (flee(e) || busyCasting(e) || market.shop(e))
                continue;
            List<LivingEntity> enemies = bot.memory.enemiesNear(e.position(), CAST_RANGE);
            HeroBot own = HeroBot.of(e);
            if (!own.cast(bot, e, enemies))
                cast(e, hero, own, enemies);
        }
    }

    // runs before troops and workers spend (BotBrain): a dead hero is worth more than any unit, so while its revive
    // can't be paid for, the money for it is held back (BotBrain.heroReserve)
    void trainAndRevive() {
        bot.heroReserve = null;
        if (!bot.hasFinished(bot.plan.heroBuilding()))
            return;
        // canProduce only passes for a revive whose hero is actually dead
        for (BotPlan.Hero h : bot.plan.heroes()) {
            ProductionPlacement pp = bot.findProducer(bot.plan.heroBuilding(), h.revive());
            if (pp == null || !h.revive().canProduce(pp) || pp.productionQueue.stream().anyMatch(ap -> ap.item == h.revive()))
                continue;
            if (!bot.startProduction(pp, h.revive()) && !h.revive().canAfford(pp) && bot.heroReserve == null)
                bot.heroReserve = h.revive().getCost(false, bot.name);
        }
        // the first hero comes right away; a second waits for a few troops so it can't starve the army.
        // canProduce enforces the allowedHeroes gamerule and the tier rule for a second hero
        if (bot.fighters.stream().anyMatch(BotBrain::isHero) && BotMath.popOf(bot.fighters) < MIN_ARMY_FOR_SECOND_HERO)
            return;
        for (BotPlan.Hero h : bot.plan.heroes()) {
            ProductionPlacement pp = bot.findProducer(bot.plan.heroBuilding(), h.unit());
            if (pp != null && pp.productionQueue.isEmpty() && h.unit().canProduce(pp) && bot.canAfford(h.unit().getCost(false, bot.name), false)) {
                bot.startProduction(pp, h.unit());
                return;
            }
        }
    }

    // ---------------------------------------------------------------- levelling

    // the matchup's order for this hero (ability class names; repeating a name ranks it again). A point the order
    // can't use yet (level too low) waits for it; with the order done, the ultimate goes first, then every ability evenly
    private void spendSkillPoints(HeroUnit hero) {
        List<HeroAbility> abilities = new ArrayList<>();
        for (Ability a : ((Unit) hero).getAbilities().get())
            if (a instanceof HeroAbility ha)
                abilities.add(ha);
        List<String> order = bot.matchup.heroSkills().getOrDefault(heroKey((LivingEntity) hero), List.of());
        while (hero.getSkillPoints() > 0) {
            List<HeroAbility> next = behindOrder(hero, abilities, order);
            if (next.stream().anyMatch(a -> a.rankUp(hero)))
                continue;
            if (!next.isEmpty())
                return;
            abilities.sort(Comparator.comparing((HeroAbility a) -> a.maxRank != 1).thenComparingInt(a -> a.getRank(hero)));
            if (abilities.stream().noneMatch(a -> a.rankUp(hero)))
                return;
        }
    }

    // abilities below the rank the order wants for them, in the order's order
    static List<HeroAbility> behindOrder(HeroUnit hero, List<HeroAbility> abilities, List<String> order) {
        Map<String, Integer> wanted = new HashMap<>();
        List<HeroAbility> behind = new ArrayList<>();
        for (String name : order) {
            int rank = wanted.merge(name, 1, Integer::sum);
            for (HeroAbility a : abilities)
                if (a.getClass().getSimpleName().equals(name) && a.getRank(hero) < Math.min(rank, a.maxRank) && !behind.contains(a))
                    behind.add(a);
        }
        return behind;
    }

    // the hero's production item id, e.g. "royal_guard", as the plan's hero_skills names it
    private String heroKey(LivingEntity hero) {
        for (BotPlan.Hero h : bot.plan.heroes()) {
            var type = Factions.unitTypeOf(h.unit());
            ResourceLocation key = ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(h.unit());
            if (type == hero.getType() && key != null)
                return key.getPath();
        }
        return "";
    }

    // ---------------------------------------------------------------- staying alive

    private boolean flee(LivingEntity e) {
        BotUnit s = bot.unit(e);
        float hp = e.getHealth() / e.getMaxHealth();
        if (s.fightsOn && hp > RETURN_HP)
            s.fightsOn = false;
        if (!s.fleeing && !s.fightsOn && hp < FLEE_HP) {
            s.fleeing = true;
            s.fleeBestHp = hp;
            s.fleeHealedTick = bot.tick;
            bot.log("hero: {} flees home at {}% health", e.getId(), (int) (hp * 100));
            Ability blink = ready(e, UnitAction.FROSTBLINK); // blink first if it can; walking home waits a think
            if (blink != null) {
                Vec3 dir = Vec3.atCenterOf(bot.home).subtract(e.position()).normalize().scale(Math.max(4, blink.range - 1));
                if (bot.order(UnitAction.FROSTBLINK, List.of(e), BlockPos.containing(e.position().add(dir))))
                    return true;
            }
            bot.order(UnitAction.MOVE, List.of(e), bot.home);
            return true;
        }
        if (!s.fleeing)
            return false;
        if (hp > s.fleeBestHp) {
            s.fleeBestHp = hp;
            s.fleeHealedTick = bot.tick;
        }
        boolean home = e.blockPosition().distSqr(bot.home) < 16 * 16;
        if (!home && BotBrain.isFree(e))
            bot.order(UnitAction.MOVE, List.of(e), bot.home);
        if (hp > RETURN_HP || (home && bot.tick - s.fleeHealedTick > FLEE_NO_HEAL_TICKS)) {
            s.fleeing = false;
            s.fightsOn = hp < RETURN_HP; // couldn't heal: running again a second later would only waste it
            bot.log("hero: {} back to the army at {}% health", e.getId(), (int) (hp * 100));
        }
        return true;
    }

    // ---------------------------------------------------------------- shared casting

    @Nullable
    static Ability ready(LivingEntity e, UnitAction action) {
        for (Ability a : ((Unit) e).getAbilities().get())
            if (a.action == action && isReady(a, e))
                return a;
        return null;
    }

    static boolean learnt(LivingEntity e, Ability a) {
        return !(a instanceof HeroAbility ha) || ha.getRank((HeroUnit) e) > 0;
    }

    private static boolean isReady(Ability a, LivingEntity e) {
        if (a instanceof HeroAbility ha && (ha.getRank((HeroUnit) e) <= 0 || ((HeroUnit) e).getMana() < ha.manaCost))
            return false;
        return a.action != null && a.action != UnitAction.NONE && a.isOffCooldown((Unit) e);
    }

    private void cast(LivingEntity e, HeroUnit hero, HeroBot own, List<LivingEntity> enemies) {
        for (Ability a : ((Unit) e).getAbilities().get()) {
            UnitAction action = a.action;
            if (!isReady(a, e) || !own.sharedCasting(action))
                continue;
            float range = Math.max(a.range, 4);
            if (ENEMY_TARGET.contains(action)) {
                LivingEntity t = BotMath.closest(e, bot.memory.enemiesNear(e.position(), range));
                if (t == null)
                    continue;
                if (a.canTargetEntities)
                    bot.orderTarget(action, List.of(e), t);
                else
                    bot.order(action, List.of(e), t.blockPosition());
                return;
            }
            if (FRIENDLY_TARGET.contains(action)) {
                LivingEntity t = pickFriendly(e, action, range);
                if (t != null) {
                    bot.orderTarget(action, List.of(e), t);
                    return;
                }
                continue;
            }
            if (SELF_IN_COMBAT.contains(action) && enemies.size() >= MIN_ENEMIES_SELF_CAST) {
                bot.order(action, List.of(e), e.blockPosition());
                return;
            }
            if (action == UnitAction.FANCY_FEAST && armyHurt(e)) {
                bot.order(action, List.of(e), e.blockPosition());
                return;
            }
        }
    }

    @Nullable
    private LivingEntity pickFriendly(LivingEntity hero, UnitAction action, float range) {
        LivingEntity best = null;
        for (LivingEntity u : bot.units) {
            if (u.distanceToSqr(hero) > range * range)
                continue;
            boolean fits = action == UnitAction.CIVIL_ENCHANTMENT ? u instanceof WorkerUnit : !(u instanceof WorkerUnit);
            if (fits && (best == null || u.distanceToSqr(hero) < best.distanceToSqr(hero)))
                best = u;
        }
        return best;
    }

    private boolean armyHurt(LivingEntity hero) {
        int hurt = 0, near = 0;
        for (LivingEntity u : bot.fighters) {
            if (u.distanceToSqr(hero) > 10 * 10)
                continue;
            near++;
            if (u.getHealth() < u.getMaxHealth() * 0.6f)
                hurt++;
        }
        return near >= 3 && hurt * 2 >= near;
    }
}
