package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.unit.units.monsters.AbstractTotem;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

public class TotemOfCasting extends AbstractTotem {

    public TotemOfCasting(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        //this.auraEffects.put(MobEffectRegistrar.VIGOR, 0);
    }
}
