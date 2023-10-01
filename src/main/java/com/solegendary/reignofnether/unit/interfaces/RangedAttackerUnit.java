package com.solegendary.reignofnether.unit.interfaces;

import net.minecraft.world.entity.LivingEntity;

public interface RangedAttackerUnit {
    default void performUnitRangedAttack(LivingEntity pTarget, float pDistanceFactor) { }
}
