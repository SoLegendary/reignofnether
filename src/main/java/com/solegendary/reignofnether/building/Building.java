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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

public abstract class Building {

    public String structureName;
    // building collapses at a certain % blocks remaining so players don't have to destroy every single block
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

    public Building(String structureName) {
        this.structureName = structureName;
    }

    public static Vec3i getBuildingSize(ArrayList<BuildingBlock> blocks) {
        Vec3i min = new Vec3i(
                blocks.stream().min(Comparator.comparing(block -> block.getBlockPos().getX())).get().getBlockPos().getX(),
                blocks.stream().min(Comparator.comparing(block -> block.getBlockPos().getY())).get().getBlockPos().getY(),
                blocks.stream().min(Comparator.comparing(block -> block.getBlockPos().getZ())).get().getBlockPos().getZ()
        );
        Vec3i max = new Vec3i(
                blocks.stream().max(Comparator.comparing(block -> block.getBlockPos().getX())).get().getBlockPos().getX(),
                blocks.stream().max(Comparator.comparing(block -> block.getBlockPos().getY())).get().getBlockPos().getY(),
                blocks.stream().max(Comparator.comparing(block -> block.getBlockPos().getZ())).get().getBlockPos().getZ()
        );
        Vec3i size = new Vec3i(
                max.getX() - min.getX(),
                max.getY() - min.getY(),
                max.getZ() - min.getZ()
        );
        return size;
    }

    // get BlockPos values with absolute world positions
    public static ArrayList<BuildingBlock> getAbsoluteBlockData(ArrayList<BuildingBlock> staticBlocks, LevelAccessor level, BlockPos originPos, Rotation rotation) {
        ArrayList<BuildingBlock> blocks = new ArrayList<>();

        for (BuildingBlock block : staticBlocks) {
            block = block.rotate(level, rotation);
            BlockPos bp = block.getBlockPos();

            block.setBlockPos(new BlockPos(
                bp.getX() + originPos.getX(),
                bp.getY() + originPos.getY() + 1,
                bp.getZ() + originPos.getZ()
            ));
            blocks.add(block);
        }
        return blocks;
    }


    // get BlockPos values with relative positions
    public static ArrayList<BuildingBlock> getRelativeBlockData() { return new ArrayList<>(); }
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
