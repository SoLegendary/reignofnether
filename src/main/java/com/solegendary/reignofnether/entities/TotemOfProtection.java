package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.unit.units.monsters.AbstractTotem;
import com.solegendary.reignofnether.util.MobEffectHelpers;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import com.solegendary.reignofnether.entities.TotemOfProtection;

public class TotemOfProtection extends AbstractTotem {

    public TotemOfProtection(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.auraEffects.put(MobEffectHelpers.holder(MobEffects.DAMAGE_RESISTANCE), 0);
        this.lifeTimeTicks = UnitItems.TOTEM_OF_PROTECTION_DURATION_SECONDS * 20;
    }

    @Override protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.METAL_HIT;
    }
    @Override protected SoundEvent getDeathSound() {
        return SoundEvents.METAL_BREAK;
    }
}
