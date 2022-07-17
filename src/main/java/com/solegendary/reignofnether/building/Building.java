package com.solegendary.reignofnether.building;

import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

public class Building {

    // players shouldn't have to destroy every single block so building collapses at a certain % blocks remaining
    public String structureName;
    public final float minBlocksPercent = 0.25f;
    public int health;
    public int maxHealth;
    public boolean isBuilt; // set true when health reaches 100% the first time
    public boolean isBuilding; // a builder is assigned and actively building or repairing
    public float buildRate; // rate at which health increases each tick when building or repairing
    // chance for a mini explosion to destroy extra blocks if a player is breaking it
    // should be higher for large fragile buildings so players don't take ages to destroy it
    public float explodeChance;
    protected ArrayList<BuildingBlock> blocks = new ArrayList<>();
    public BlockPos originPos = null; // origin of structure, but mouse location will be close to centre

    public Building(String structureName) {
        this.structureName = structureName;
    }

    public static Vec3i getBuildingSize(ArrayList<BuildingBlock> blocks) {
        return new Vec3i(
                blocks.stream().max(Comparator.comparingInt(block -> block.getBlockPos().getX())).get().getBlockPos().getX() + 1,
                blocks.stream().max(Comparator.comparingInt(block -> block.getBlockPos().getY())).get().getBlockPos().getY() + 1,
                blocks.stream().max(Comparator.comparingInt(block -> block.getBlockPos().getZ())).get().getBlockPos().getZ() + 1
        );
    }
    // static returns of the base data
    public static ArrayList<BuildingBlock> getBlockData() {
        return new ArrayList<>();
    }
    public static ArrayList<BlockState> getPaletteData() {
        return new ArrayList<>();
    }
    // non-static returns of the instanced live data
    public ArrayList<BuildingBlock> getBlocks() {
        return this.blocks;
    }

    public int getTotalBlocks() {
        return blocks.size();
    }
    public int getCurrentBlocks() {
        return blocks.stream().filter(b -> b.isPlaced).toList().size();
    }
    public float getBlocksPercent() {
        return (float) getCurrentBlocks() / (float) getTotalBlocks();
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
