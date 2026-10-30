package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

// Royal Guard: the frontline hero. Mace Slam and Avatar go through the shared casting (when enemies are close).
// Taunting Cry is only worth it against charged creepers: it pulls them onto the hero instead of the army.
final class RoyalGuardBot implements HeroBot {

    private static final int CREEPER_RANGE = 8;

    @Override
    public boolean cast(BotBrain bot, LivingEntity hero, List<LivingEntity> enemies) {
        boolean chargedCreeper = enemies.stream().anyMatch(e -> e instanceof CreeperUnit c && c.isPowered() && c.distanceTo(hero) < CREEPER_RANGE);
        return chargedCreeper && BotHeroes.ready(hero, UnitAction.TAUNTING_CRY) != null &&
                bot.order(UnitAction.TAUNTING_CRY, List.of(hero), hero.blockPosition());
    }

    @Override
    public boolean sharedCasting(UnitAction action) {
        return action != UnitAction.TAUNTING_CRY;
    }
}
