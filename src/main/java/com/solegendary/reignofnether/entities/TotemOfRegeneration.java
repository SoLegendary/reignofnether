package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.unit.units.monsters.AbstractTotem;
import com.solegendary.reignofnether.util.MobEffectHelpers;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import com.solegendary.reignofnether.entities.TotemOfRegeneration;

public class TotemOfRegeneration extends AbstractTotem {

    public TotemOfRegeneration(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.auraEffects.put(MobEffectHelpers.holder(MobEffects.REGENERATION), 0);
        this.lifeTimeTicks = UnitItems.TOTEM_OF_REGENERATION_DURATION_SECONDS * 20;
        this.auraDurationTicks = 60;
    }
}
