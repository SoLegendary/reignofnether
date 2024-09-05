package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.buildings.monsters.Dungeon;
import com.solegendary.reignofnether.building.buildings.piglins.FlameSanctuary;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
import com.solegendary.reignofnether.building.buildings.villagers.IronGolemBuilding;
import com.solegendary.reignofnether.fogofwar.FrozenChunkClientboundPacket;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.research.ResearchServer;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class BuildingServerEvents {

    private static final int BUILDING_SYNC_TICKS_MAX = 20; // how often we send out unit syncing packets
    private static int buildingSyncTicks = BUILDING_SYNC_TICKS_MAX;

    private static ServerLevel serverLevel = null;

    // buildings that currently exist serverside
    private static final ArrayList<Building> buildings = new ArrayList<>();

    public static final ArrayList<NetherConversionZone> netherConversionZones = new ArrayList<>();

    public static ArrayList<Building> getBuildings() { return buildings; }

    private static int ticksToSave = 0;
    @SubscribeEvent
    public static void saveBuildings(TickEvent.LevelTickEvent evt) {
        if (evt.level.isClientSide() || evt.phase != TickEvent.Phase.END)
            return;

        ticksToSave += 1;
        if (ticksToSave == 2400) {
            SavedBuildingData data = SavedBuildingData.getInstance(evt.level);
            getBuildings().forEach(b -> {
                data.getBuildings().add(new SavedBuilding(
                        b.originPos,
                        evt.level,
                        b.name,
                        b.ownerName,
                        b.rotation,
                        b.isDiagonalBridge
                ));
                System.out.println("saved building in serverevents: " + b.originPos);
            });
            data.save();
        }
    }

    @SubscribeEvent
    public static void loadBuildings(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        if (level != null) {
            SavedBuildingData data = SavedBuildingData.getInstance(level);
            data.getBuildings().forEach(b -> {
                Building building = BuildingUtils.getNewBuilding(b.name, level, b.originPos, b.rotation, b.ownerName, b.isDiagonalBridge);
                BuildingServerEvents.getBuildings().add(building);
                BuildingClientboundPacket.placeBuilding(b.originPos, b.name, b.rotation, b.ownerName,0, b.isDiagonalBridge, false);

                System.out.println("loaded building in serverevents: " + b.originPos);
            });
        }
    }

    public static void placeBuilding(String buildingName, BlockPos pos, Rotation rotation, String ownerName,
                                     int[] builderUnitIds, boolean queue, boolean isDiagonalBridge) {
        Building newBuilding = BuildingUtils.getNewBuilding(buildingName, serverLevel, pos, rotation, ownerName, isDiagonalBridge);
        boolean buildingExists = false;
        for (Building building : buildings)
            if (building.originPos == pos) {
                buildingExists = true;
                break;
            }

        if (newBuilding != null && !buildingExists) {

            // special check for iron golem buildings
            if (newBuilding instanceof IronGolemBuilding) {
                int currentPop = UnitServerEvents.getCurrentPopulation(serverLevel, ownerName);
                int popSupply = BuildingServerEvents.getTotalPopulationSupply(ownerName);

                boolean canAffordPop = false;
                for (Resources resources : ResourcesServerEvents.resourcesList) {
                    if (resources.ownerName.equals(ownerName)) {
                        canAffordPop = (currentPop + ResourceCosts.IRON_GOLEM.population) <= popSupply;
                        break;
                    }
                }
                if (!canAffordPop) {
                    ResourcesClientboundPacket.warnInsufficientPopulation(ownerName);
                    return;
                }
            }

            if (newBuilding.canAfford(ownerName)) {
                buildings.add(newBuilding);
                newBuilding.forceChunk(true);

                int minY = BuildingUtils.getMinCorner(newBuilding.blocks).getY();

                if (!(newBuilding instanceof AbstractBridge)) {
                    for (BuildingBlock block : newBuilding.blocks) {
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
                                    newBuilding.getScaffoldBlocks().add(scaffold);
                                    newBuilding.addToBlockPlaceQueue(scaffold);
                                    yBelow += 1;
                                }
                            }
                        }
                    }
                }

                for (BuildingBlock block : newBuilding.blocks) {
                    // place all blocks on the lowest y level
                    if (block.getBlockPos().getY() == minY &&
                            newBuilding.startingBlockTypes.contains(block.getBlockState().getBlock()))
                        newBuilding.addToBlockPlaceQueue(block);
                }
                BuildingClientboundPacket.placeBuilding(pos, buildingName, rotation, ownerName,
                        newBuilding.blockPlaceQueue.size(), isDiagonalBridge, false);

                ResourcesServerEvents.addSubtractResources(new Resources(
                    newBuilding.ownerName,
                    -newBuilding.foodCost,
                    -newBuilding.woodCost,
                    -newBuilding.oreCost
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
                            workerUnit.getBuildRepairGoal().queuedBuildings.add(newBuilding);
                            if (workerUnit.getBuildRepairGoal().getBuildingTarget() == null)
                                workerUnit.getBuildRepairGoal().startNextQueuedBuilding();
                        } else {
                            ((Unit) entity).resetBehaviours();
                            WorkerUnit.resetBehaviours(workerUnit);
                            workerUnit.getBuildRepairGoal().setBuildingTarget(newBuilding);
                        }
                    }
                }
            }
            else if (!PlayerServerEvents.isBot(ownerName))
                ResourcesClientboundPacket.warnInsufficientResources(newBuilding.ownerName,
                    ResourcesServerEvents.canAfford(newBuilding.ownerName, ResourceName.FOOD, newBuilding.foodCost),
                    ResourcesServerEvents.canAfford(newBuilding.ownerName, ResourceName.WOOD, newBuilding.woodCost),
                    ResourcesServerEvents.canAfford(newBuilding.ownerName, ResourceName.ORE, newBuilding.oreCost)
                );
        }
    }

    public static void cancelBuilding(Building building) {
        if (building == null || building.isCapitol || TutorialServerEvents.isEnabled())
            return;

        // remove from tracked buildings, all of its leftover queued blocks and then blow it up
        buildings.remove(building);
        FrozenChunkClientboundPacket.setBuildingDestroyedServerside(building.originPos);

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
                building.blockPlaceQueue.size(),
                building instanceof AbstractBridge bridge && bridge.isDiagonalBridge,
                true
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
                    evt.getEntity().discard();
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

        // need to remove from the list first as destroy() will read it to check defeats
        List<Building> buildingsToDestroy = buildings.stream().filter(Building::shouldBeDestroyed).toList();
        buildings.removeIf(b -> {
            if (b.shouldBeDestroyed()) {
                if (b instanceof NetherConvertingBuilding nb && nb.getZone() != null)
                    nb.getZone().startRestoring();
                FrozenChunkClientboundPacket.setBuildingDestroyedServerside(b.originPos);
                return true;
            }
            return false;
        });

        for (Building building : buildingsToDestroy)
            building.destroy(serverLevel);

        for (Building building : buildings)
            building.tick(serverLevel);

        for (NetherConversionZone netherConversionZone : netherConversionZones)
            netherConversionZone.tick();

        netherConversionZones.removeIf(NetherConversionZone::isDone);
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
        PillagerUnit pillagerUnit = null;

        if (evt.getExplosion().getSourceMob() instanceof CreeperUnit cUnit) {
            creeperUnit = cUnit;
        } // generic means it was from random blocks broken, so don't consider it or we might keep chaining
        else if (evt.getExplosion().getSourceMob() instanceof PillagerUnit pUnit) {
            pillagerUnit = pUnit;
        }
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

        // explosive arrows from mounted pillagers
        if (exp.getSourceMob() instanceof PillagerUnit pUnit && pUnit.isPassenger())
            for (Entity entity : evt.getAffectedEntities())
                if (entity instanceof LivingEntity le)
                    le.setHealth(le.getHealth() - 2); // for some reason there's still iframes so we cant use hurt()

        // apply creeper, ghast and mounted pillager attack damage as bonus damage to buildings
        // this is dealt in addition to the actual blocks destroyed by the explosion itself
        if (creeperUnit != null || ghastUnit != null || pillagerUnit != null) {
            Set<Building> affectedBuildings = new HashSet<>();
            for (BlockPos bp : evt.getAffectedBlocks()) {
                Building building = BuildingUtils.findBuilding(false, bp);
                if (building != null)
                    affectedBuildings.add(building);
            }
            for (Building building : affectedBuildings) {
                int atkDmg = 0;
                if (ghastUnit != null) {
                    atkDmg = (int) ghastUnit.getUnitAttackDamage();
                } else if (creeperUnit != null) {
                    atkDmg = (int) creeperUnit.getUnitAttackDamage();
                    if (creeperUnit.isPowered())
                        atkDmg *= 2;
                } else if (pillagerUnit != null) {
                    atkDmg = (int) pillagerUnit.getUnitAttackDamage() / 2;
                }
                building.destroyRandomBlocks(atkDmg);
            }
        }
        // don't do any block damage apart from the scripted building damage above or damage to leaves
        evt.getAffectedBlocks().removeIf(bp -> {
            BlockState bs = evt.getLevel().getBlockState(bp);
            return !(bs.getBlock() instanceof LeavesBlock);
        });
    }

    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent evt) {
        if (BuildingUtils.isPosInsideAnyBuilding(evt.getEntity().getLevel().isClientSide(), evt.getEntity().getOnPos()))
            evt.setCanceled(true);
    }

    public static void replaceClientBuilding(BlockPos buildingPos) {
        for (Building building : buildings) {
            if (building.originPos.equals(buildingPos)) {
                BuildingClientboundPacket.placeBuilding(
                        building.originPos,
                        building.name,
                        building.rotation,
                        building.ownerName,
                        building.blockPlaceQueue.size(),
                        building instanceof AbstractBridge bridge && bridge.isDiagonalBridge,
                        false
                );
                return;
            }
        }
    }
}
