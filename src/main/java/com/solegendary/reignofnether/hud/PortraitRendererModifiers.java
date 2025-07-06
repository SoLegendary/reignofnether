package com.solegendary.reignofnether.hud;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.unit.units.monsters.NecromancerUnit;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import com.solegendary.reignofnether.unit.units.piglins.GruntUnit;
import com.solegendary.reignofnether.unit.units.piglins.HeadhunterUnit;
import com.solegendary.reignofnether.unit.units.piglins.PiglinMerchantUnit;
import com.solegendary.reignofnether.unit.units.villagers.RoyalGuardUnit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.warden.Warden;

public class PortraitRendererModifiers {

    public static Pair<Integer, Integer> getPortraitRendererModifiers(LivingEntity entity) {
        int yOffset = 0;
        int scale = 0;

        if (entity instanceof Warden) {
            yOffset = -60;
            scale = -21;
        } else if (entity instanceof Horse || entity instanceof SkeletonHorse || entity instanceof ZombieHorse) {
            yOffset = -6;
            scale = -10;
        } else if (entity instanceof Panda) {
            yOffset = -15;
            scale = -15;
        } else if (entity instanceof PolarBear) {
            yOffset = -15;
            scale = -15;
        } else if (entity instanceof Pig) {
            yOffset = 10;
        } else if (entity instanceof IronGolem) {
            yOffset = -54;
            scale = -17;
        } else if (entity instanceof AbstractFish) {
            yOffset = 20;
        } else if (entity instanceof Squid) {
            yOffset = 10;
            scale = -20;
        } else if (entity instanceof Turtle) {
            yOffset = 14;
            scale = -14;
        } else if (entity instanceof CaveSpider) {
            yOffset = 9;
            scale = -11;
        } else if (entity instanceof Spider) {
            scale = -18;
        } else if (entity instanceof Rabbit) {
            yOffset = 18;
            scale = 15;
        } else if (entity instanceof Chicken) {
            yOffset = 14;
        } else if (entity instanceof Blaze) {
            yOffset = -10;
            scale = -5;
        } else if (entity instanceof MushroomCow) {
            scale = -5;
        } else if (entity instanceof Donkey || entity instanceof Mule) {
            scale = -5;
        } else if (entity instanceof Ocelot || entity instanceof Cat) {
            yOffset = 7;
        } else if (entity instanceof Fox) {
            yOffset = 20;
        } else if (entity instanceof Vex) {
            yOffset = 5;
        }  else if (entity instanceof Phantom) {
            yOffset = 20;
        } else if (entity instanceof Hoglin || entity instanceof Zoglin) {
            yOffset = -18;
            scale = -18;
        } else if (entity instanceof Slime slime) { // largest size only
            yOffset = 33 - (17 * slime.getSize());
            scale = -32;
        } else if (entity instanceof Wolf) {
            yOffset = 12;
        } else if (entity instanceof Silverfish) {
            yOffset = 26;
        } else if (entity instanceof EnderMan) {
            yOffset = -15;
        } else if (entity instanceof Ravager) {
            yOffset = -54;
            scale = -25;
        } else if (entity instanceof Dolphin) {
            yOffset = 20;
            scale = -10;
        } else if (entity instanceof Bee) {
            yOffset = 20;
            scale = -5;
        } else if (entity instanceof WitherSkeleton) {
            yOffset = -15;
            scale = -4;
        } else if (entity instanceof Ghast) {
            yOffset = -118;
            scale = -37;
        } else if (entity instanceof NecromancerUnit) {
            yOffset = -11;
            scale = -9;
        } else if (entity instanceof PiglinMerchantUnit) {
            yOffset = -35;
            scale = -27;
        } else if (entity instanceof RoyalGuardUnit royalGuardUnit) {
            yOffset = -34;
            scale = -16;
            float avatarPercent = (float) royalGuardUnit.avatarScaleTicks / royalGuardUnit.AVATAR_SCALE_TICKS_MAX;
            yOffset -= (26 * avatarPercent);
        } else if (entity instanceof GruntUnit ||
                    entity instanceof BruteUnit ||
                    entity instanceof HeadhunterUnit) {
            yOffset = -6;
        }

        return new Pair<>(yOffset, scale);
    }
}
