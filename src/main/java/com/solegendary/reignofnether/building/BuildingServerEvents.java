package com.solegendary.reignofnether.building;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class BuildingServerEvents {

    private static ArrayList<Pair<BlockPos, CompoundTag>> placeQueue = new ArrayList<>();
    private static ArrayList<BlockPos> destroyQueue = new ArrayList<>();

    public static void placeBlock(BlockPos bp, CompoundTag nbt) {
        placeQueue.add(new Pair<>(bp, nbt));
    }

    public static void destroyBlock(BlockPos bp) {
        destroyQueue.add(bp);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        if (evt.world.isClientSide())
            return;

        if (!BuildingTemplates.initedStructures)
            BuildingTemplates.initStructures((ServerLevel) evt.world);

        for (Pair<BlockPos, CompoundTag> placeBlock : placeQueue) {
            evt.world.setBlock(placeBlock.getFirst(), Blocks.OAK_LOG.defaultBlockState(), 1);
        }
        placeQueue = new ArrayList<>();

        for (BlockPos destroyBlock : destroyQueue) {
            evt.world.destroyBlock(destroyBlock, false);
        }
        destroyQueue = new ArrayList<>();
    }
}
