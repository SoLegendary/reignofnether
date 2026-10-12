package com.solegendary.reignofnether.mixin;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 1.21.1 made {@code LivingEntity#getWaterSlowDown()} protected, and the access transformer
 * entry that used to open it up is not visible to {@code compileJava}. Several units override it
 * to change their in-water movement speed, so {@code Unit#getMovementSpeed} has to read the
 * overridden value rather than a hardcoded 0.8.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Invoker("getWaterSlowDown")
    float reignOfNether$getWaterSlowDown();
}
