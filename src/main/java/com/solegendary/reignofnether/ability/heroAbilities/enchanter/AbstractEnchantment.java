package com.solegendary.reignofnether.ability.heroAbilities.enchanter;

import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

public abstract class AbstractEnchantment extends HeroAbility {

    public AbstractEnchantment(int maxRank, int manaCost, UnitAction action, int cooldownMax, float range, float radius, boolean canTargetEntities) {
        super(maxRank, manaCost, action, cooldownMax, range, radius, canTargetEntities);
    }

    public List<EntityType<? extends Mob>> getAllowedMobTypes() {
        return List.of();
    }

    public boolean canEnchant(LivingEntity le) {
        return le instanceof Unit;
    }
}
