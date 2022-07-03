package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Building {

    // players shouldn't have to destroy every single block so building collapses at a certain % blocks remaining
    public final float minBlocksPercent = 0.25f;
    public int health;
    public int maxHealth;
    public boolean isBuilt; // set true when health reaches 100% the first time
    public boolean isBuilding; // a builder is assigned and actively building or repairing
    public float buildRate; // rate at which health increases each tick when building or repairing
    // chance for a mini explosion to destroy extra blocks if a player is breaking it
    // should be higher for large fragile buildings so players don't take ages to destroy it
    public float explodeChance;
    private StructureTemplate structTemplate;

    public Building() {

    }

    public float getBlocksPercent() {
        return 1f;
    }

    public float getHealthPercent() {
        return ((float) health / (float) maxHealth);
    }

    public boolean isFunctional() {
        return this.isBuilt && this.getHealthPercent() >= 0.5f;
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent evt) {
        // when a player breaks a block that's part of the building:
        // - roll explodeChance to cause explosion effects and destroy more blocks
        // - cause fire if < 50% health
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent evt) {
        // match healthPercent to (blocksBuiltPercent + minBlocksPercent)

        // if health <= 0: destroy the building in big explosion

        // if builder is assigned, increase health by buildRate
        // if fires exist, put them out one by one (or remove them all if healthPercent > 50%)

        // calculate number of blocks to place based on new healthPercent and blocksBuiltPercent
        // eg. if there are 100/200 blocks built, and health raised from 50% -> 60%, place 20 blocks to match

        // place blocks as required from bottom to top (obeying gravity if possible)

    }
}
