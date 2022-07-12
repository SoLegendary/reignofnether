package com.solegendary.reignofnether.building;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Optional;

public class BuildingServerEvents {

    // buildings that currently exist serverside
    private static ArrayList<Building> buildings = new ArrayList<>();

    private static ArrayList<Pair<BlockPos, BlockState>> blockPlaceQueue = new ArrayList<>();
    private static ArrayList<BlockPos> blockDestroyQueue = new ArrayList<>();

    public static void placeBlock(BlockPos bp, BlockState bs) {
        blockPlaceQueue.add(new Pair<>(bp, bs));
    }

    public static void destroyBlock(BlockPos bp) {
        blockDestroyQueue.add(bp);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        if (evt.world.isClientSide())
            return;

        for (Pair<BlockPos, BlockState> placeBlock : blockPlaceQueue) {
            evt.world.setBlock(placeBlock.getFirst(), placeBlock.getSecond(), 1);
        }
        blockPlaceQueue = new ArrayList<>();

        for (BlockPos destroyBlock : blockDestroyQueue) {
            evt.world.destroyBlock(destroyBlock, false);
        }
        blockDestroyQueue = new ArrayList<>();
    }
}
