package com.solegendary.reignofnether.building;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class BuildingServerEvents {

    private static ServerLevel serverLevel = null;

    // buildings that currently exist serverside
    private static ArrayList<Building> buildings = new ArrayList<>();
    private static ArrayList<BuildingBlock> blockPlaceQueue = new ArrayList<>();
    private static ArrayList<BlockPos> blockDestroyQueue = new ArrayList<>();

    public static void placeBlock(BuildingBlock block) {
        blockPlaceQueue.add(block);
    }
    public static void destroyBlock(BlockPos pos) {
        blockDestroyQueue.add(pos);
    }

    public static void placeBuilding(String buildingName, BlockPos pos, Rotation rotation, String ownerName) {
        Building building = Building.getNewBuilding(buildingName, serverLevel, pos, rotation, ownerName);
        if (building != null) {
            buildings.add(building);
            // place all blocks on the lowest y level
            int minY = Building.getMinCorner(building.blocks).getY();
            for (BuildingBlock block : building.blocks)
                if (block.getBlockPos().getY() == minY)
                    block.place();
        }
        BuildingClientboundPacket.placeBuilding(pos, buildingName, rotation, ownerName);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent evt) {
        if (!evt.getWorld().isClientSide()) {
            for (Building building : buildings)
                building.onBlockBreak(evt);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        if (!evt.world.isClientSide() && evt.world.dimension() == Level.OVERWORLD) {
            serverLevel = (ServerLevel) evt.world;

            for (Building building : buildings)
                building.onWorldTick(serverLevel);
            buildings.removeIf((Building building) -> building.getBlocksPlaced() <= 0 && building.tickAge > 10);

            for (BuildingBlock blockToPlace : blockPlaceQueue) {
                BlockPos bp = blockToPlace.getBlockPos();
                BlockState bs = blockToPlace.getBlockState();
                BuildingClientboundPacket.placeBlock(bp, Block.getId(bs));
                serverLevel.setBlock(bp, bs, 1);
            }
            blockPlaceQueue = new ArrayList<>();

            for (BlockPos blockToDestroy : blockDestroyQueue) {
                BuildingClientboundPacket.destroyBlock(blockToDestroy);
                serverLevel.destroyBlock(blockToDestroy, false);
            }
            blockDestroyQueue = new ArrayList<>();
        }
    }
}
