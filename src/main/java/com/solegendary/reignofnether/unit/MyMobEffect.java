package com.solegendary.reignofnether.unit;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

// makes MobEffect public, to allow continuous particle-showing effects
public class MyMobEffect extends MobEffect {
    public MyMobEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }
}
