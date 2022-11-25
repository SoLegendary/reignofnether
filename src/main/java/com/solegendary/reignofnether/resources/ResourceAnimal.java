package com.solegendary.reignofnether.resources;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

// constants for values of food that each animal is worth
public class ResourceAnimal {
    public int foodValue;
    public String animalName;

    public ResourceAnimal(int foodValue,String animalName) {
        this.foodValue = foodValue;
        this.animalName = animalName;
    }
}
