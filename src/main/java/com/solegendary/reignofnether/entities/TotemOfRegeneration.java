package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.unit.units.monsters.AbstractTotem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public class TotemOfRegeneration extends AbstractTotem {

    public TotemOfRegeneration(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.auraEffects.put(MobEffects.REGENERATION, 0);
    }
}
