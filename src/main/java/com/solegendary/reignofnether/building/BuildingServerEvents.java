package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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

    public static List<Building> getBuildings() {
        return buildings;
    }

    public static void placeBlock(BuildingBlock block) {
        blockPlaceQueue.add(block);
    }
    public static void destroyBlock(BlockPos pos) {
        blockDestroyQueue.add(pos);
    }

    public static void placeBuilding(String buildingName, BlockPos pos, Rotation rotation, String ownerName, int[] builderUnitIds) {
        Building building = BuildingUtils.getNewBuilding(buildingName, serverLevel, pos, rotation, ownerName);
        if (building != null) {

            if (building.canAfford(ownerName)) {
                buildings.add(building);
                // place all blocks on the lowest y level
                int minY = BuildingUtils.getMinCorner(building.blocks).getY();
                for (BuildingBlock block : building.blocks)
                    if (block.getBlockPos().getY() == minY)
                        block.place();
                BuildingClientboundPacket.placeBuilding(pos, buildingName, rotation, ownerName);
                ResourcesServerEvents.addSubtractResources(new Resources(
                    building.ownerName,
                    -building.foodCost,
                    -building.woodCost,
                    -building.oreCost
                ));
                // assign the builder unit that placed this building
                for (int id : builderUnitIds) {
                    Entity entity = serverLevel.getEntity(id);
                    if (entity instanceof Unit unit)
                        unit.setBuildRepairTarget(building);
                }
            }
            else
                ResourcesClientboundPacket.warnInsufficientResources(building.ownerName,
                    building.canAffordFood(building.ownerName),
                    building.canAffordWood(building.ownerName),
                    building.canAffordOre(building.ownerName)
                );
        }
    }

    public static void cancelBuilding(Building building) {
        // remove from tracked buildings, all of its leftover queued blocks and then blow it up
        buildings.remove(building);
        for (BuildingBlock block : building.getBlocks())
            blockPlaceQueue.removeIf(queuedBlock -> queuedBlock.getBlockPos().equals(block.getBlockPos()));
        building.destroy((ServerLevel) building.level);

        // AOE2-style refund: return the % of the non-built portion of the building
        // eg. cancelling a building at 70% completion will refund only 30% cost
        float buildPercent = building.getBlocksPlacedPercent();
        ResourcesServerEvents.addSubtractResources(new Resources(
                building.ownerName,
                Math.round(building.foodCost * (1 - buildPercent)),
                Math.round(building.woodCost * (1 - buildPercent)),
                Math.round(building.oreCost * (1 - buildPercent))
        ));
    }

    public static int getTotalPopulationSupply(String ownerName) {
        int totalPopulationSupply = 0;
        for (Building building : buildings)
            if (building.ownerName.equals(ownerName) && building.isBuilt)
                totalPopulationSupply += building.popSupply;
        return Math.min(ResourceCosts.MAX_POPULATION, totalPopulationSupply);
    }

    // if blocks are destroyed manually by a player then help it along by causing periodic explosions
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent evt) {
        if (!evt.getLevel().isClientSide()) {
            for (Building building : buildings)
                if (building.isPosPartOfBuilding(evt.getPos(), true))
                    building.onBlockBreak(evt);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (!evt.level.isClientSide() && evt.level.dimension() == Level.OVERWORLD && evt.phase == TickEvent.Phase.END) {
            serverLevel = (ServerLevel) evt.level;

            for (Building building : buildings)
                building.tick(serverLevel);
            buildings.removeIf(Building::shouldBeDestroyed);

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

    // cancel all explosion damage to non-building blocks
    // cancel damage to entities if it came from a non-entity source such as:
    // - building block breaks
    // - beds (vanilla)
    // - respawn anchors (vanilla)
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate evt) {
        Explosion exp = evt.getExplosion();

        if (exp.getExploder() == null && exp.getSourceMob() == null)
            evt.getAffectedEntities().removeIf((Entity entity) -> true);

        evt.getAffectedBlocks().removeIf((BlockPos bp) -> {
            for (Building building : buildings)
                if (!building.isPosPartOfBuilding(bp, true))
                    return true;
            return false;
        });
    }
}
