package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.heroAbilities.enchanter.EnchantedTether;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.EnchanterUnit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Set;

// Enchanter: a buffer, not a fighter. Civil and Martial are switched to autocast once learnt (the Enchanter then
// picks targets itself: Civil on workers, Martial on troops), Enchanted Tether goes on our biggest troops in a
// fight, and March of Progress is switched on while the army attacks.
final class EnchanterBot implements HeroBot {

    private static final Set<UnitAction> ENCHANTMENTS = Set.of(UnitAction.CIVIL_ENCHANTMENT, UnitAction.MARTIAL_ENCHANTMENT);
    private static final int TETHER_RECAST_TICKS = 200; // the ability refuses a target with more than this left

    @Override
    public boolean cast(BotBrain bot, LivingEntity hero, List<LivingEntity> enemies) {
        Unit u = (Unit) hero;
        for (Ability a : u.getAbilities().get())
            if (ENCHANTMENTS.contains(a.action) && a.autocastEnableAction != null && BotHeroes.learnt(hero, a) && !a.isAutocasting(u))
                return bot.order(a.autocastEnableAction, List.of(hero), hero.blockPosition());
        if (!enemies.isEmpty() && BotHeroes.ready(hero, UnitAction.ENCHANTED_TETHER) != null) {
            LivingEntity t = tetherTarget(bot, hero);
            if (t != null)
                return bot.orderTarget(UnitAction.ENCHANTED_TETHER, List.of(hero), t);
        }
        boolean attacking = bot.army.state == BotArmy.State.ATTACK;
        if (hero instanceof EnchanterUnit enchanter && attacking != enchanter.isAuraEnabled() &&
                BotHeroes.ready(hero, UnitAction.MARCH_OF_PROGRESS_TOGGLE) != null)
            return bot.order(UnitAction.MARCH_OF_PROGRESS_TOGGLE, List.of(hero), hero.blockPosition());
        return false;
    }

    // the biggest of our troops in range that isn't tethered yet (heroes can't be)
    private static LivingEntity tetherTarget(BotBrain bot, LivingEntity hero) {
        LivingEntity best = null;
        for (LivingEntity e : bot.fighters) {
            if (e instanceof HeroUnit || e.distanceToSqr(hero) > EnchantedTether.RANGE * EnchantedTether.RANGE)
                continue;
            MobEffectInstance tether = e.getEffect(MobEffectRegistrar.ENCHANTED_TETHER.get());
            if (tether != null && tether.getDuration() > TETHER_RECAST_TICKS)
                continue;
            if (best == null || BotMath.popOf(e) > BotMath.popOf(best))
                best = e;
        }
        return best;
    }

    @Override
    public boolean sharedCasting(UnitAction action) {
        return !ENCHANTMENTS.contains(action) && action != UnitAction.ENCHANTED_TETHER && action != UnitAction.MARCH_OF_PROGRESS_TOGGLE;
    }
}
