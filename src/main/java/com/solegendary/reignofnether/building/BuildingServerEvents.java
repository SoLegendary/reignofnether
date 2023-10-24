package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.buildings.monsters.Dungeon;
import com.solegendary.reignofnether.building.buildings.piglins.FlameSanctuary;
import com.solegendary.reignofnether.research.ResearchServer;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

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

    public static void placeBuilding(String buildingName, BlockPos pos, Rotation rotation, String ownerName, int[] builderUnitIds, boolean queue) {
        Building building = BuildingUtils.getNewBuilding(buildingName, serverLevel, pos, rotation, ownerName);
        if (building != null) {

            if (building.canAfford(ownerName)) {
                buildings.add(building);
                building.forceChunk(true);

                int minY = BuildingUtils.getMinCorner(building.blocks).getY();

                for (BuildingBlock block : building.blocks) {
                    // place scaffolding underneath all solid blocks that don't have support
                    if (block.getBlockPos().getY() == minY && !block.getBlockState().isAir()) {
                        int yBelow = 0;
                        boolean tooDeep = false;
                        BlockState bsBelow;
                        do {
                            yBelow -= 1;
                            bsBelow = serverLevel.getBlockState(block.getBlockPos().offset(0, yBelow, 0));
                            if (yBelow < -5)
                                tooDeep = true;
                        }
                        while (!bsBelow.getMaterial().isSolidBlocking());
                        yBelow += 1;

                        if (!tooDeep) {
                            while (yBelow < 0) {
                                BlockPos bp = block.getBlockPos().offset(0, yBelow, 0);
                                BuildingBlock scaffold = new BuildingBlock(bp, Blocks.SCAFFOLDING.defaultBlockState());
                                building.getScaffoldBlocks().add(scaffold);
                                building.addToBlockPlaceQueue(scaffold);
                                yBelow += 1;
                            }
                        }
                    }
                }

                for (BuildingBlock block : building.blocks) {
                    // place all blocks on the lowest y level
                    if (block.getBlockPos().getY() == minY &&
                            building.startingBlockTypes.contains(block.getBlockState().getBlock()))
                        building.addToBlockPlaceQueue(block);
                }

                BuildingClientboundPacket.placeBuilding(pos, buildingName, rotation, ownerName, building.blockPlaceQueue.size());
                ResourcesServerEvents.addSubtractResources(new Resources(
                    building.ownerName,
                    -building.foodCost,
                    -building.woodCost,
                    -building.oreCost
                ));
                // assign the builder unit that placed this building
                for (int id : builderUnitIds) {
                    Entity entity = serverLevel.getEntity(id);
                    if (entity instanceof WorkerUnit workerUnit) {
                        if (queue) {
                            if (workerUnit.getBuildRepairGoal().queuedBuildings.size() == 0) {
                                ((Unit) entity).resetBehaviours();
                                WorkerUnit.resetBehaviours(workerUnit);
                            }
                            workerUnit.getBuildRepairGoal().queuedBuildings.add(building);
                            if (workerUnit.getBuildRepairGoal().getBuildingTarget() == null)
                                workerUnit.getBuildRepairGoal().startNextQueuedBuilding();
                        } else {
                            ((Unit) entity).resetBehaviours();
                            WorkerUnit.resetBehaviours(workerUnit);
                            workerUnit.getBuildRepairGoal().setBuildingTarget(building);
                        }
                    }
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
        if (building.isCapitol)
            return;

        // remove from tracked buildings, all of its leftover queued blocks and then blow it up
        buildings.remove(building);

        // AOE2-style refund: return the % of the non-built portion of the building
        // eg. cancelling a building at 70% completion will refund only 30% cost
        if (!building.isBuilt) {
            float buildPercent = building.getBlocksPlacedPercent();
            ResourcesServerEvents.addSubtractResources(new Resources(
                    building.ownerName,
                    Math.round(building.foodCost * (1 - buildPercent)),
                    Math.round(building.woodCost * (1 - buildPercent)),
                    Math.round(building.oreCost * (1 - buildPercent))
            ));
        }
        building.destroy((ServerLevel) building.getLevel());
    }

    public static int getTotalPopulationSupply(String ownerName) {
        if (ResearchServer.playerHasCheat(ownerName, "foodforthought"))
            return Integer.MAX_VALUE;

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

    // does the player own one of these buildings?
    public static boolean playerHasFinishedBuilding(String playerName, String buildingName) {
        for (Building building : buildings)
            if (building.name.equals(buildingName) && building.isBuilt && building.ownerName.equals(playerName))
                return true;
        return false;
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        for (Building building : buildings)
            BuildingClientboundPacket.placeBuilding(
                building.originPos,
                building.name,
                building.rotation,
                building.ownerName,
                building.blockPlaceQueue.size()
            );
    }

    // if blocks are destroyed manually by a player then help it along by causing periodic explosions
    @SubscribeEvent
    public static void onPlayerBlockBreak(BlockEvent.BreakEvent evt) {
        if (!evt.getLevel().isClientSide()) {
            for (Building building : buildings)
                if (building.isPosPartOfBuilding(evt.getPos(), true))
                    building.onBlockBreak((ServerLevel) evt.getLevel(), evt.getPos(), true);
        }
    }

    // prevent dungeons spawners from actually spawning
    @SubscribeEvent
    public static void onLivingSpawn(LivingSpawnEvent.SpecialSpawn evt) {
        if (evt.getSpawnReason() == MobSpawnType.SPAWNER) {
            if (evt.getSpawner() != null &&
                    evt.getSpawner().getSpawnerBlockEntity() != null) {
                BlockEntity be = evt.getSpawner().getSpawnerBlockEntity();
                BlockPos bp = evt.getSpawner().getSpawnerBlockEntity().getBlockPos();
                if (BuildingUtils.findBuilding(false, bp) instanceof Dungeon ||
                    BuildingUtils.findBuilding(false, bp) instanceof FlameSanctuary)
                    evt.setResult(Event.Result.DENY);
            }
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

        GhastUnit ghastUnit = null;
        CreeperUnit creeperUnit = null;

        if (evt.getExplosion().getSourceMob() instanceof CreeperUnit cUnit) {
            creeperUnit = cUnit;
        } // generic means it was from random blocks broken, so don't consider it or we might keep chaining
        else if (exp.getDamageSource() != DamageSource.GENERIC) {
            for (Entity entity : evt.getAffectedEntities()) {
                if (entity instanceof LargeFireball fireball &&
                        fireball.getOwner() instanceof GhastUnit gUnit) {
                    ghastUnit = gUnit;
                    exp.damageSource = new EntityDamageSource("explosion", ghastUnit);
                }
            }
        }

        // set fire to random blocks from a ghast fireball
        if (ghastUnit != null) {

            List<BlockPos> flammableBps = evt.getAffectedBlocks().stream().filter(bp -> {
                BlockState bs = evt.getLevel().getBlockState(bp);
                BlockState bsAbove = evt.getLevel().getBlockState(bp.above());
                return bs.getMaterial().isSolidBlocking() && bsAbove.isAir() ||
                        bsAbove.getBlock() instanceof TallGrassBlock ||
                        bsAbove.getBlock() instanceof RootsBlock;
            }).toList();

            if (flammableBps.size() > 0) {
                Random rand = new Random();
                for (int i = 0; i < GhastUnit.FIREBALL_FIRE_BLOCKS; i++) {
                    BlockPos bp = flammableBps.get(rand.nextInt(flammableBps.size()));
                    evt.getLevel().setBlockAndUpdate(bp.above(), Blocks.FIRE.defaultBlockState());
                }
            }
        }

        if (exp.getExploder() == null && exp.getSourceMob() == null && ghastUnit == null)
            evt.getAffectedEntities().clear();

        // apply creeper attack damage as bonus damage to buildings
        if (creeperUnit != null) {
            Set<Building> affectedBuildings = new HashSet<>();
            for (BlockPos bp : evt.getAffectedBlocks()) {
                Building building = BuildingUtils.findBuilding(false, bp);
                if (building != null)
                    affectedBuildings.add(building);
            }
            for (Building building : affectedBuildings) {
                int atkDmg = (int) creeperUnit.getUnitAttackDamage();
                if (creeperUnit.isPowered())
                    atkDmg *= 2;
                building.destroyRandomBlocks(atkDmg);
            }
        } // apply ghast attack damage as bonus damage to buildings
        else if (ghastUnit != null) {
            BlockPos centreBp = new BlockPos(evt.getExplosion().getPosition());
            Building affectedBuilding = BuildingUtils.findBuilding(false, centreBp);
            if (affectedBuilding != null) {
                affectedBuilding.destroyRandomBlocks((int) ghastUnit.getUnitAttackDamage());
            }
        }

        // don't do any block damage apart from the scripted building damage above
        evt.getAffectedBlocks().clear();
    }

    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent evt) {
        if (BuildingUtils.isPosInsideAnyBuilding(evt.getEntity().getLevel().isClientSide(), evt.getEntity().getOnPos()))
            evt.setCanceled(true);
    }
}
