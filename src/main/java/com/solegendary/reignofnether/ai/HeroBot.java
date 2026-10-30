package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.units.villagers.EnchanterUnit;
import com.solegendary.reignofnether.unit.units.villagers.RoyalGuardUnit;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;

// What one hero does that the shared hero logic (BotHeroes) doesn't: its own casting rules. A hero with no
// HeroBot still plays fine on the shared logic, which casts each ability by what it targets.
interface HeroBot {

    Map<Class<?>, HeroBot> BY_CLASS = Map.of(
            RoyalGuardUnit.class, new RoyalGuardBot(),
            EnchanterUnit.class, new EnchanterBot());

    HeroBot SHARED_ONLY = new HeroBot() {};

    static HeroBot of(LivingEntity hero) {
        return BY_CLASS.getOrDefault(hero.getClass(), SHARED_ONLY);
    }

    // a cast of its own this think; true when it ordered one (the shared casting then waits)
    default boolean cast(BotBrain bot, LivingEntity hero, List<LivingEntity> enemies) {
        return false;
    }

    // false: the shared casting leaves this ability alone (this hero handles it, or it's never worth it)
    default boolean sharedCasting(UnitAction action) {
        return true;
    }
}
