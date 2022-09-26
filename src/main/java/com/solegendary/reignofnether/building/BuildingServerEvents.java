package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuildingServerEvents {

    private static ServerLevel serverLevel = null;

    // buildings that currently exist serverside
    private static List<Building> buildings = Collections.synchronizedList(new ArrayList<>());
    private static List<BuildingBlock> blockPlaceQueue = Collections.synchronizedList(new ArrayList<>());
    private static List<BlockPos> blockDestroyQueue = Collections.synchronizedList(new ArrayList<>());

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

    // if blocks are destroyed manually by a player then help it along by causing periodic explosions
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent evt) {
        if (!evt.getLevel().isClientSide()) {
            for (Building building : buildings)
                building.onBlockBreak(evt);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (!evt.level.isClientSide() && evt.level.dimension() == Level.OVERWORLD && evt.phase == TickEvent.Phase.END) {
            serverLevel = (ServerLevel) evt.level;

            for (Building building : buildings)
                building.onWorldTick(serverLevel);
            buildings.removeIf(Building::isDestroyed);

            if (blockPlaceQueue.size() > 0) {
                BuildingBlock nextBlock = blockPlaceQueue.get(0);
                BlockPos bp = nextBlock.getBlockPos();
                BlockState bs = nextBlock.getBlockState();
                if (serverLevel.isLoaded(bp)) {
                    serverLevel.setBlockAndUpdate(bp, bs);
                    serverLevel.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, bp, Block.getId(bs));
                    serverLevel.levelEvent(bs.getSoundType().getPlaceSound().hashCode(), bp, Block.getId(bs));
                    blockPlaceQueue.removeIf(i -> i.equals(nextBlock));
                }
            }

            if (blockDestroyQueue.size() > 0) {
                BlockPos bp = blockDestroyQueue.get(0);
                if (serverLevel.isLoaded(bp)) {
                    serverLevel.destroyBlock(bp, false);
                    blockDestroyQueue.removeIf(b -> b.equals(bp));
                }
            }
        }
    }

    // cancel damage to entities and non-building blocks if an explosion originated from a non-entity, including from:
    // - building block breaks
    // - beds (vanilla)
    // - respawn anchors (vanilla)
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate evt) {
        Explosion exp = evt.getExplosion();
        if (exp.getExploder() == null && exp.getSourceMob() == null) {
            evt.getAffectedEntities().removeIf((Entity entity) -> true);
            evt.getAffectedBlocks().removeIf((BlockPos bp) -> {
                for (Building building : buildings)
                    if (!building.isPosPartOfBuilding(bp, true))
                        return true;
                return false;
            });
        }
    }
}
