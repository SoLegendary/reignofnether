package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
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
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BuildingServerEvents {

    private static final int BUILDING_SYNC_TICKS_MAX = 20; // how often we send out unit syncing packets
    private static int buildingSyncTicks = BUILDING_SYNC_TICKS_MAX;

    private static ServerLevel serverLevel = null;

    // buildings that currently exist serverside
    private static final ArrayList<Building> buildings = new ArrayList<>();

    private static final ArrayList<Building> buildingsBackup = new ArrayList<>();

    public static ArrayList<Building> getBuildings() {
        return buildings;
    }

    public static void placeBuilding(String buildingName, BlockPos pos, Rotation rotation, String ownerName, int[] builderUnitIds) {
        Building building = BuildingUtils.getNewBuilding(buildingName, serverLevel, pos, rotation, ownerName);
        if (building != null) {

            if (building.canAfford(ownerName)) {
                buildings.add(building);
                // place all blocks on the lowest y level
                int minY = BuildingUtils.getMinCorner(building.blocks).getY();
                for (BuildingBlock block : building.blocks)
                    if (block.getBlockPos().getY() == minY &&
                        building.startingBlockTypes.contains(block.getBlockState().getBlock()))
                        building.addToBlockPlaceQueue(block);

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
                    if (entity instanceof WorkerUnit workerUnit)
                        workerUnit.getBuildRepairGoal().setBuildingTarget(building);
                }
            }
            else
                ResourcesClientboundPacket.warnInsufficientResources(building.ownerName,
                    ResourcesServerEvents.canAfford(building.ownerName, ResourceName.FOOD, building.foodCost),
                    ResourcesServerEvents.canAfford(building.ownerName, ResourceName.WOOD, building.woodCost),
                    ResourcesServerEvents.canAfford(building.ownerName, ResourceName.ORE, building.oreCost)
                );
        }
    }

    public static void cancelBuilding(Building building) {
        // remove from tracked buildings, all of its leftover queued blocks and then blow it up
        buildings.remove(building);

        // AOE2-style refund: return the % of the non-built portion of the building
        // eg. cancelling a building at 70% completion will refund only 30% cost
        float buildPercent = building.getBlocksPlacedPercent();
        ResourcesServerEvents.addSubtractResources(new Resources(
                building.ownerName,
                Math.round(building.foodCost * (1 - buildPercent)),
                Math.round(building.woodCost * (1 - buildPercent)),
                Math.round(building.oreCost * (1 - buildPercent))
        ));
        building.destroy((ServerLevel) building.getLevel());
    }

    public static int getTotalPopulationSupply(String ownerName) {
        int totalPopulationSupply = 0;
        for (Building building : buildings)
            if (building.ownerName.equals(ownerName) && building.isBuilt)
                totalPopulationSupply += building.popSupply;
        return Math.min(ResourceCosts.MAX_POPULATION, totalPopulationSupply);
    }

    // similar to BuildingClientEvents getPlayerToBuildingRelationship: given a Unit and Building, what is the relationship between them
    public static Relationship getUnitToBuildingRelationship(Unit unit, Building building) {
        if (unit.getOwnerName().equals(building.ownerName))
            return Relationship.OWNED;
        else
            return Relationship.HOSTILE;
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        for (Building building : buildings)
            BuildingClientboundPacket.placeBuilding(
                building.originPos,
                building.name,
                building.rotation,
                building.ownerName
            );
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
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD)
            return;

        serverLevel = (ServerLevel) evt.level;

        buildingSyncTicks -= 1;
        if (buildingSyncTicks <= 0) {
            buildingSyncTicks = BUILDING_SYNC_TICKS_MAX;
            for (Building building : buildings)
                BuildingClientboundPacket.syncBuilding(building.originPos, building.getBlocksPlaced());
        }

        for (Building building : buildings)
            building.tick(serverLevel);
        buildings.removeIf(Building::shouldBeDestroyed);
    }

    // cancel all explosion damage to non-building blocks
    // cancel damage to entities and non-building blocks if it came from a non-entity source such as:
    // - building block breaks
    // - beds (vanilla)
    // - respawn anchors (vanilla)
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate evt) {
        Explosion exp = evt.getExplosion();

        if (exp.getExploder() == null && exp.getSourceMob() == null) {
            evt.getAffectedEntities().clear();
            evt.getAffectedBlocks().removeIf((BlockPos bp) -> {
                boolean isPartOfBuilding = false;
                for (Building building : buildings)
                    if (building.isPosPartOfBuilding(bp, true))
                        isPartOfBuilding = true;
                return !isPartOfBuilding;
            });
        }
        else {
            evt.getAffectedBlocks().removeIf((BlockPos bp) -> {
                boolean isPartOfBuilding = false;
                for (Building building : buildings)
                    if (building.isPosPartOfBuilding(bp, true))
                        isPartOfBuilding = true;
                return !isPartOfBuilding;
            });
        }
    }
}
