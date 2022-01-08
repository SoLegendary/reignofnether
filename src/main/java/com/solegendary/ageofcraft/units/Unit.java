package com.solegendary.ageofcraft.units;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;

import java.util.Random;
import java.util.function.Predicate;

/**
 * Parent class of all RTS units which allows for:
 * - selection
 * - movement
 * - attacking
 * - complex commands such as attack move, patrol, etc.
 *
 * @author SoLegendary
 */
public class Unit extends PathfinderMob {

    protected Unit(EntityType<? extends Unit> p_33002_, Level p_33003_) {
        super(p_33002_, p_33003_);
    }

    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    public void aiStep() {
        super.aiStep();
    }

    protected SoundEvent getSwimSound() {
        return SoundEvents.HOSTILE_SWIM;
    }

    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.HOSTILE_SPLASH;
    }

    protected SoundEvent getHurtSound(DamageSource p_33034_) {
        return SoundEvents.HOSTILE_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.HOSTILE_DEATH;
    }

    protected SoundEvent getFallDamageSound(int p_33041_) {
        return p_33041_ > 4 ? SoundEvents.HOSTILE_BIG_FALL : SoundEvents.HOSTILE_SMALL_FALL;
    }

    public float getWalkTargetValue(BlockPos p_33013_, LevelReader p_33014_) {
        return 0.5F - p_33014_.getBrightness(p_33013_);
    }

    public static AttributeSupplier.Builder createUnitAttributes() {
        return Mob.createMobAttributes();
    }
}