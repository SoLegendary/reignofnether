package com.solegendary.reignofnether.building;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class BuildingServerEvents {

    private static ServerLevel serverLevel = null;
    private static ClientLevel clientLevel = null;

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
        BuildingClientboundPacket.placeBuilding(buildingName, pos, rotation, ownerName);
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
        if (clientLevel == null)
            clientLevel = Minecraft.getInstance().level;
        // WorldTickEvent actually ticks for every dimension
        if (serverLevel == null && clientLevel != null)
            if (evt.world.dimension().equals(clientLevel.dimension()))
                serverLevel = (ServerLevel) evt.world;

        if (serverLevel != null && !evt.world.isClientSide()) {
            for (Building building : buildings)
                building.onWorldTick(serverLevel);
            buildings.removeIf((Building building) -> building.getBlocksPlaced() <= 0 );
        }

        if (serverLevel != null && clientLevel != null) {
            for (BuildingBlock blockToPlace : blockPlaceQueue) {
                // place and destroy it once first so we get the break effect
                clientLevel.setBlock(blockToPlace.getBlockPos(), blockToPlace.getBlockState(), 1);
                clientLevel.destroyBlock(blockToPlace.getBlockPos(), false);
                clientLevel.setBlock(blockToPlace.getBlockPos(), blockToPlace.getBlockState(), 1);
                serverLevel.setBlock(blockToPlace.getBlockPos(), blockToPlace.getBlockState(), 1);
            }
            blockPlaceQueue = new ArrayList<>();

            for (BlockPos blockToDestroy : blockDestroyQueue) {
                clientLevel.destroyBlock(blockToDestroy, false);
                serverLevel.destroyBlock(blockToDestroy, false);
            }
            blockDestroyQueue = new ArrayList<>();
        }
    }
}
