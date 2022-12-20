package com.solegendary.reignofnether.resources;

import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;

import java.util.ArrayList;
import java.util.List;

// constants for values of food that each animal is worth
public class ResourceAnimals {

    // babies are worth half the food value
    public static final List<ResourceAnimal> animals = List.of(
            new ResourceAnimal(100, "Panda"),
            new ResourceAnimal(75, "Sheep"),
            new ResourceAnimal(75, "Cow"),
            new ResourceAnimal(75, "Pig"),
            new ResourceAnimal(50, "Chicken"),

            new ResourceAnimal(50, "Llama"),
            new ResourceAnimal(50, "Mule"),
            new ResourceAnimal(50, "Donkey"),
            new ResourceAnimal(50, "Horse"),
            new ResourceAnimal(50, "Goat"),

            new ResourceAnimal(30, "Ocelot"),
            new ResourceAnimal(30, "Wolf"),
            new ResourceAnimal(30, "Rabbit")
    );
}
