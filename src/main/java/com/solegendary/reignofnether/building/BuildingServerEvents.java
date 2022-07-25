package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.buildings.VillagerHouse;
import com.solegendary.reignofnether.building.buildings.VillagerTower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class BuildingServerEvents {

    private static ServerLevel serverLevel = null;
    private static ClientLevel clientLevel = null;

    // buildings that currently exist serverside
    private static ArrayList<Building> buildings = new ArrayList<>();
    private static ArrayList<BuildingBlock> blockPlaceQueue = new ArrayList<>();
    private static ArrayList<BlockPos> blockDestroyQueue = new ArrayList<>();

    public static void placeBuilding(String buildingName, BlockPos pos, Rotation rotation) {
        Building building = null;

        switch(buildingName) {
            case VillagerHouse.buildingName -> building = new VillagerHouse(serverLevel, pos, rotation);
            case VillagerTower.buildingName -> building = new VillagerTower(serverLevel, pos, rotation);
        }
        if (building != null) {
            buildings.add(building);
            blockPlaceQueue.addAll(building.blocks);
        }
    }

    // destroys any building that overlaps the given BlockPos
    public static void destroyBuilding(BlockPos pos) {

    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        if (!evt.world.isClientSide())
            serverLevel = (ServerLevel) evt.world;
        if (clientLevel == null)
            clientLevel = Minecraft.getInstance().level;

        if (serverLevel != null && clientLevel != null) {
            for (BuildingBlock placeBlock : blockPlaceQueue) {
                clientLevel.setBlock(placeBlock.getBlockPos(), placeBlock.getBlockState(), 1);
                serverLevel.setBlock(placeBlock.getBlockPos(), placeBlock.getBlockState(), 1);
            }
            blockPlaceQueue = new ArrayList<>();

            for (BlockPos destroyBlock : blockDestroyQueue) {
                clientLevel.destroyBlock(destroyBlock, false);
                serverLevel.destroyBlock(destroyBlock, false);
            }
            blockDestroyQueue = new ArrayList<>();
        }
    }
}
