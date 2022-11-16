package com.solegendary.reignofnether.resources;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

public class ResourceBlock {
    public String name;
    public List<Block> validBlocks; // list of different blocks that will have the same ResourceBlock properties
    public int ticksToGather;
    public int resourceValue;
    public ResourceName resourceName; // food, wood or ore
    public Predicate<BlockState> blockStateTest; // checks whether the given blockstate is valid for this resource block

    public ResourceBlock(String name, List<Block> validBlocks, int ticksToGather, int resourceValue,
                         ResourceName resourceName, Predicate<BlockState> blockStateTest) {
        this.name = name;
        this.validBlocks = validBlocks;
        this.ticksToGather = ticksToGather;
        this.resourceValue = resourceValue;
        this.resourceName = resourceName;
        this.blockStateTest = blockStateTest;
    }

    public ResourceBlock(String name, List<Block> validBlocks, int ticksToGather, int resourceValue, ResourceName resourceName) {
        this.name = name;
        this.validBlocks = validBlocks;
        this.ticksToGather = ticksToGather;
        this.resourceValue = resourceValue;
        this.resourceName = resourceName;
        this.blockStateTest = (bs) -> true;
    }
}
