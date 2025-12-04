package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.buildings.monsters.Laboratory;
import com.solegendary.reignofnether.building.buildings.neutral.NeutralTransportPortal;
import com.solegendary.reignofnether.building.buildings.placements.*;
import com.solegendary.reignofnether.building.buildings.villagers.Castle;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.fogofwar.FrozenChunkClientboundPacket;
import com.solegendary.reignofnether.nether.NetherBlocks;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BuildingServerEvents {

    private static final int BUILDING_SYNC_TICKS_MAX = 20; // how often we send out unit syncing packets
    private static int buildingSyncTicks = BUILDING_SYNC_TICKS_MAX;

    private static final int TNT_BUILDING_BASE_DAMAGE = 20;
    private static final int MAX_SCAFFOLD_DEPTH = 5;

    private static ServerLevel serverLevel = null;

    public static ServerLevel getServerLevel() { return serverLevel; }

    // buildings that currently exist serverside
    private static final ArrayList<BuildingPlacement> buildings = new ArrayList<>();

    public static final ArrayList<NetherZone> netherZones = new ArrayList<>();

    public static ArrayList<BuildingPlacement> getBuildings() {
        return buildings;
    }

    public static final Random random = new Random();

    private static final int SAVE_TICKS_MAX = 600;
    private static int saveTicks = 0;
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        saveTicks += 1;
        if (saveTicks >= SAVE_TICKS_MAX) {
            ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
            if (level != null) {
                saveBuildings(level);
                saveNetherZones(level);
                saveTicks = 0;
            }
        }
    }

    public static void saveBuildings(ServerLevel level) {
        BuildingSaveData buildingData = BuildingSaveData.getInstance(serverLevel);
        buildingData.buildings.clear();

        getBuildings().forEach(b -> {
            PortalPlacement.PortalType portalType = null;
            if (b instanceof PortalPlacement portal) {
                portalType = portal.getPortalType();
            }
            buildingData.buildings.add(new BuildingSave(b.originPos,
                    level,
                    b.getBuilding(),
                    b.ownerName,
                    b.rotation,
                    b instanceof ProductionPlacement pb ? pb.getFinalRallyPoint() : b.originPos,
                    b.isDiagonalBridge,
                    b.isBuilt,
                    b.getUpgradeLevel(),
                    portalType,
                    b instanceof PortalPlacement portal && portal.hasDestination() ? portal.destination : new BlockPos(0,0,0)
            ));
            //ReignOfNether.LOGGER.info("saved buildings/nether in serverevents: " + b.originPos);
        });

        // deduplicate saved buildings by removing any additional buildings with the same originPos
        Set<BlockPos> seenOriginPoses = new HashSet<>();
        buildingData.buildings.removeIf(b -> !seenOriginPoses.add(b.originPos));

        buildingData.save();
        level.getDataStorage().save();
    }

    public static void saveNetherZones(ServerLevel level) {
        NetherZoneSaveData netherData = NetherZoneSaveData.getInstance(level);
        netherData.netherZones.clear();
        netherData.netherZones.addAll(netherZones);
        netherData.save();
        level.getDataStorage().save();

        ReignOfNether.LOGGER.info("saved " + netherZones.size() + " netherzones in serverevents");
    }

    @SubscribeEvent
    public static void loadBuildingsAndNetherZones(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        if (level != null) {
            CustomBuildingServerEvents.loadCustomBuildings(level);
            BuildingSaveData buildingData = BuildingSaveData.getInstance(level);
            NetherZoneSaveData netherData = NetherZoneSaveData.getInstance(level);
            ArrayList<BlockPos> placedNZs = new ArrayList<>();
            BuildingServerEvents.getBuildings().clear();
            buildingData.buildings.forEach(b -> {
                BuildingPlacement building = BuildingUtils.getNewBuildingPlacement(b.building,
                    level,
                    b.originPos,
                    b.rotation,
                    b.ownerName,
                    b.isDiagonalBridge
                );

                if (building != null) {
                    building.isBuilt = b.isBuilt;
                    BuildingServerEvents.getBuildings().add(building);

                    if (building instanceof ProductionPlacement pb) {
                        pb.setRallyPoint(b.rallyPoint);
                    }

                    if (b.upgradeLevel > 0) {
                        if (building.getBuilding() instanceof Castle) {
                            building.changeStructure(Castle.upgradedStructureName);
                        } else if (building.getBuilding() instanceof Laboratory) {
                            building.changeStructure(Laboratory.upgradedStructureName);
                        } else if (building instanceof PortalPlacement portal) {
                            if (!(building.getBuilding() instanceof NeutralTransportPortal)) {
                                portal.changeStructure(b.portalType);
                            } if (b.portalDestination != null && !b.portalDestination.equals(new BlockPos(0,0,0))) {
                                portal.destination = b.portalDestination;
                            }
                        } else if (building.getBuilding() instanceof Library) {
                            building.changeStructure(Library.upgradedStructureName);
                        } else if (building instanceof BeaconPlacement beacon) {
                            beacon.changeStructure(b.upgradeLevel);
                        }
                    }
                    // setNetherZone can only be run once - this supercedes where it normally happens in tick() ->
                    // onBuilt()
                    if (building instanceof NetherConvertingBuilding ncb && ncb.getMaxNetherRange() > 0) {
                        for (NetherZone nz : netherData.netherZones)
                            if (building.isPosInsideBuilding(nz.getOrigin())) {
                                ncb.setNetherZone(nz, false);
                                placedNZs.add(nz.getOrigin());
                                ReignOfNether.LOGGER.info("loaded netherzone for: " + b.building.name + "|" + b.originPos);
                                break;
                            }
                    }
                    ReignOfNether.LOGGER.info("loaded building in serverevents: " + b.building.name + "|" + b.originPos);
                }
            });
            netherData.netherZones.forEach(nz -> {
                if (!placedNZs.contains(nz.getOrigin())) {
                    BuildingServerEvents.netherZones.add(nz);
                    nz.startRestoring();
                    ReignOfNether.LOGGER.info("loaded orphaned netherzone: " + nz.getOrigin());
                }
            });
            saveNetherZones(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
        if (level != null) {
            saveNetherZones(level);
            saveBuildings(level);
        }
    }

    @Nullable
    public static BuildingPlacement placeBuilding(
        Building building,
        BlockPos pos,
        Rotation rotation,
        String ownerName,
        int[] builderUnitIds,
        boolean queue,
        boolean isDiagonalBridge
    ) {
        BuildingPlacement newBuilding = BuildingUtils.getNewBuildingPlacement(building,
            serverLevel,
            pos,
            rotation,
            ownerName,
            isDiagonalBridge
        );
        boolean buildingExists = buildings.stream().anyMatch(b -> b.originPos.equals(pos));

        if (newBuilding != null && !buildingExists) {
            // Handle special building (Iron Golem)
            if (newBuilding instanceof IronGolemPlacement) {
                int currentPop = UnitServerEvents.getCurrentPopulation(serverLevel, ownerName);
                int popSupply = BuildingServerEvents.getTotalPopulationSupply(ownerName);

                boolean canAffordPop = ResourcesServerEvents.resourcesList.stream()
                    .anyMatch(r -> r.ownerName.equals(ownerName)
                        && (currentPop + ResourceCosts.IRON_GOLEM.population) <= popSupply);

                if (!canAffordPop) {
                    ResourcesClientboundPacket.warnInsufficientPopulation(ownerName);
                    return null;
                }
            }

            if (newBuilding.canAfford(ownerName)) {
                if (serverLevel.getGameRules().getRule(GameRuleRegistrar.SLANTED_BUILDING).get() &&
                    !(newBuilding instanceof BridgePlacement)) {
                    BuildingUtils.clearBuildingArea(newBuilding);
                }
                buildings.add(newBuilding);
                newBuilding.forceChunk(true);
                int minY = BuildingUtils.getMinCorner(newBuilding.blocks).getY();

                if (!(newBuilding instanceof BridgePlacement))
                    for (BuildingBlock block : newBuilding.blocks)
                        if (block.getBlockPos().getY() == minY && !block.getBlockState().isAir())
                            placeScaffoldingUnder(block, newBuilding);

                newBuilding.blocks.stream()
                    .filter(block -> block.getBlockPos().getY() <= minY + (newBuilding.getBuilding().foundationYLayers - 1)
                        && newBuilding.getBuilding().startingBlockTypes.contains(block.getBlockState().getBlock()))
                    .forEach(newBuilding::addToBlockPlaceQueue);

                BuildingClientboundPacket.placeBuilding(pos,
                    building,
                    rotation,
                    ownerName,
                    newBuilding.blockPlaceQueue.size(),
                    isDiagonalBridge,
                    0,
                    false,
                    PortalPlacement.PortalType.BASIC,
                    pos,
                    false
                );
                ResourcesServerEvents.addSubtractResources(new Resources(ownerName,
                    -newBuilding.getBuilding().cost.food,
                    -newBuilding.getBuilding().cost.wood,
                    -newBuilding.getBuilding().cost.ore
                ));

                if (ownerName.isEmpty() || ownerName.equals("Enemy"))
                    newBuilding.selfBuilding = true;

                assignBuilderUnits(builderUnitIds, queue, newBuilding);

                UnitServerEvents.getAllUnits()
                        .stream()
                        .filter(entity -> entity instanceof Unit unit && unit.getOwnerName().equals(ownerName) &&
                                newBuilding.isPosInsideBuilding(entity.getOnPos().above().above()) &&
                                (unit.getMoveGoal().getMoveTarget() == null ||
                                newBuilding.isPosInsideBuilding(unit.getMoveGoal().getMoveTarget())))
                        .forEach(entity -> moveNonBuildersAwayFromBuildingFoundations(entity, builderUnitIds, newBuilding));

            } else if (!PlayerServerEvents.isBot(ownerName)) {
                warnInsufficientResources(newBuilding);
            }
            if (SandboxServer.isAnyoneASandboxPlayer() && builderUnitIds.length == 0) {
                newBuilding.getBuilding().shouldDestroyOnReset = false;
                saveBuildings(getServerLevel());
            }
            return newBuilding;
        }
        return null;
    }

    private static void placeScaffoldingUnder(BuildingBlock block, BuildingPlacement newBuilding) {
        BlockPos basePos = block.getBlockPos();
        int yBelow = 0;
        BlockState bsBelow;

        // Search downward for a solid block up to -5 levels below
        while (yBelow > -MAX_SCAFFOLD_DEPTH) {
            yBelow--;
            BlockPos bpBelow = basePos.offset(0, yBelow, 0);
            if (MiscUtil.isSolidBlocking(newBuilding.level, bpBelow)) {
                break; // Found a solid block, exit loop
            }
        }
        if (yBelow <= -MAX_SCAFFOLD_DEPTH) {
            return;
        }

        // Place scaffolding from the lowest point back to the original block's level
        for (int y = yBelow + 1; y < 0; y++) {
            BlockPos scaffoldPos = basePos.offset(0, y, 0);
            BuildingBlock scaffold = new BuildingBlock(scaffoldPos, Blocks.SCAFFOLDING.defaultBlockState());
            newBuilding.getScaffoldBlocks().add(scaffold);
            newBuilding.addToBlockPlaceQueue(scaffold);
        }
    }


    private static void assignBuilderUnits(int[] builderUnitIds, boolean queue, BuildingPlacement newBuilding) {
        for (int id : builderUnitIds) {
            Entity entity = serverLevel.getEntity(id);
            if (entity instanceof WorkerUnit workerUnit) {
                if (queue) {
                    if (workerUnit.getBuildRepairGoal().queuedBuildings.isEmpty()) {
                        ((Unit) entity).resetBehaviours();
                        WorkerUnit.resetBehaviours(workerUnit);
                    }
                    workerUnit.getBuildRepairGoal().queuedBuildings.add(newBuilding);
                    if (workerUnit.getBuildRepairGoal().getBuildingTarget() == null) {
                        workerUnit.getBuildRepairGoal().startNextQueuedBuilding();
                    }
                } else {
                    ((Unit) entity).resetBehaviours();
                    WorkerUnit.resetBehaviours(workerUnit);
                    workerUnit.getBuildRepairGoal().setBuildingTarget(newBuilding);
                }
            }
        }
    }

    private static void warnInsufficientResources(BuildingPlacement newBuilding) {
        ResourcesClientboundPacket.warnInsufficientResources(newBuilding.ownerName,
            ResourcesServerEvents.canAfford(newBuilding.ownerName, ResourceName.FOOD, newBuilding.getBuilding().cost.food),
            ResourcesServerEvents.canAfford(newBuilding.ownerName, ResourceName.WOOD, newBuilding.getBuilding().cost.wood),
            ResourcesServerEvents.canAfford(newBuilding.ownerName, ResourceName.ORE, newBuilding.getBuilding().cost.ore)
        );
    }

    private static void moveNonBuildersAwayFromBuildingFoundations(
        LivingEntity entity, int[] builderUnitIds, BuildingPlacement newBuilding
    ) {
        if (Arrays.stream(builderUnitIds).noneMatch(id -> id == entity.getId())) {
            UnitServerEvents.addActionItem(((Unit) entity).getOwnerName(),
                UnitAction.MOVE,
                -1,
                new int[] { entity.getId() },
                newBuilding.getClosestGroundPos(entity.getOnPos(), 2),
                new BlockPos(0, 0, 0)
            );
        }
    }

    public static void cancelBuilding(BuildingPlacement building, String playerName) {
        if (building == null)
            return;
        if (building.isBuilt && !SandboxServer.isSandboxPlayer(playerName) &&
            BuildingUtils.getTotalCompletedBuildingsOwned(false, building.ownerName) == 1)
            return;

        // remove from tracked buildings, all of its leftover queued blocks and then blow it up
        buildings.remove(building);
        if (building instanceof NetherConvertingBuilding ncb && ncb.getMaxNetherRange() > 0 && ncb.getNetherZone() != null) {
            ncb.getNetherZone().startRestoring();
            saveNetherZones(serverLevel);
        }
        FrozenChunkClientboundPacket.setBuildingDestroyedServerside(building.originPos);

        // AOE2-style refund: return the % of the non-built portion of the building
        // eg. cancelling a building at 70% completion will refund only 30% cost
        // in survival, refund 50% of this amount
        if (!building.isBuilt || SurvivalServerEvents.isEnabled()) {

            float buildPercent = building.getBlocksPlacedPercent();
            int food = Math.round(building.getBuilding().cost.food * (1 - buildPercent));
            int wood = Math.round(building.getBuilding().cost.wood * (1 - buildPercent));
            int ore = Math.round(building.getBuilding().cost.ore * (1 - buildPercent));

            if (building.isBuilt && SurvivalServerEvents.isEnabled()) {
                food = Math.round(building.getBuilding().cost.food * 0.5f * buildPercent);
                wood = Math.round(building.getBuilding().cost.wood * 0.5f * buildPercent);
                ore = Math.round(building.getBuilding().cost.ore * 0.5f * buildPercent);
            }
            if (food > 0 || wood > 0 || ore > 0) {
                Resources res = new Resources(building.ownerName, food, wood, ore);
                ResourcesServerEvents.addSubtractResources(res);
                ResourcesClientboundPacket.showFloatingText(res, building.centrePos);
            }
        }
        building.destroy((ServerLevel) building.getLevel());
    }

    public static int getTotalPopulationSupply(String ownerName) {
        if (ResearchServerEvents.playerHasCheat(ownerName, "foodforthought")) {
            return UnitServerEvents.maxPopulation;
        }

        int totalPopulationSupply = 0;
        for (BuildingPlacement building : buildings)
            if (building.ownerName.equals(ownerName) && building.isBuilt) {
                totalPopulationSupply += building.getBuilding().cost.population;
            }
        return Math.min(UnitServerEvents.maxPopulation, totalPopulationSupply);
    }

    // similar to BuildingClientEvents getPlayerToBuildingRelationship: given a Unit and Building, what is the
    // relationship between them
    public static Relationship getUnitToBuildingRelationship(Unit unit, BuildingPlacement building) {
        if (unit.getOwnerName().equals(building.ownerName)) {
            return Relationship.OWNED;
        } else {
            return Relationship.HOSTILE;
        }
    }

    private static void placeBuildingsClientside() {
        for (BuildingPlacement building : buildings) {
            BuildingClientboundPacket.placeBuilding(building.originPos,
                    building.getBuilding(),
                    building.rotation,
                    building.ownerName,
                    building.blockPlaceQueue.size(),
                    building instanceof BridgePlacement bridge && bridge.isDiagonalBridge,
                    building.getUpgradeLevel(),
                    building.isBuilt,
                    building instanceof PortalPlacement p ? p.getPortalType() : PortalPlacement.PortalType.BASIC,
                    building instanceof PortalPlacement p && p.hasDestination() ? p.destination : new BlockPos(0, 0, 0),
                    true
            );
        }
    }

    public static void placeBuildingClientside(BlockPos pos) {
        for (BuildingPlacement building : buildings) {
            if (building.originPos.equals(pos)) {
                BuildingClientboundPacket.placeBuilding(building.originPos,
                        building.getBuilding(),
                        building.rotation,
                        building.ownerName,
                        building.blockPlaceQueue.size(),
                        building instanceof BridgePlacement bridge && bridge.isDiagonalBridge,
                        building.getUpgradeLevel(),
                        building.isBuilt,
                        building instanceof PortalPlacement p ? p.getPortalType() : PortalPlacement.PortalType.BASIC,
                        building instanceof PortalPlacement p && p.hasDestination() ? p.destination : new BlockPos(0, 0, 0),
                        true
                );
            }
            break;
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (!PlayerServerEvents.rtsSyncingEnabled) {
            return;
        }
        MinecraftServer server = evt.getEntity().level().getServer();
        if (server == null || !server.isDedicatedServer()) {
            CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS).execute(BuildingServerEvents::placeBuildingsClientside);
        } else {
            placeBuildingsClientside();
        }
        //ReignOfNether.LOGGER.info("Synced " + buildings.size() + " buildings with player logged in");
    }

    // if blocks are destroyed manually by a player then help it along by causing periodic explosions
    @SubscribeEvent
    public static void onPlayerBlockBreak(BlockEvent.BreakEvent evt) {
        if (!evt.getLevel().isClientSide()) {
            for (BuildingPlacement building : buildings)
                if (building.isPosPartOfBuilding(evt.getPos(), true)) {
                    building.onBlockBreak((ServerLevel) evt.getLevel(), evt.getPos(), true);
                }
        }
    }

    // prevent dungeons spawners from actually spawning
    @SubscribeEvent
    public static void onLivingSpawn(MobSpawnEvent.FinalizeSpawn evt) {
        if (evt.getSpawnType() == MobSpawnType.SPAWNER) {
            if (evt.getSpawner() != null && evt.getSpawner().getSpawnerBlockEntity() != null) {
                BlockEntity be = evt.getSpawner().getSpawnerBlockEntity();
                BlockPos bp = evt.getSpawner().getSpawnerBlockEntity().getBlockPos();
                if (BuildingUtils.findBuilding(false, bp) instanceof DungeonPlacement ||
                    BuildingUtils.findBuilding(false, bp) instanceof FlameSanctuaryPlacement) {
                    evt.getEntity().discard();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD) {
            return;
        }

        serverLevel = (ServerLevel) evt.level;

        buildingSyncTicks -= 1;
        if (buildingSyncTicks <= 0) {
            buildingSyncTicks = BUILDING_SYNC_TICKS_MAX;
            for (BuildingPlacement building : buildings)
                BuildingClientboundPacket.syncBuilding(building.originPos, building.getBlocksPlaced(), building.ownerName);
        }
        // need to remove from the list first as destroy() will read it to check defeats
        List<BuildingPlacement> buildingsToDestroy = buildings.stream().filter(BuildingPlacement::shouldBeDestroyed).toList();
        buildings.removeIf(b -> {
            if (b.shouldBeDestroyed()) {
                if (b instanceof NetherConvertingBuilding ncb && ncb.getMaxNetherRange() > 0 && ncb.getNetherZone() != null) {
                    ncb.getNetherZone().startRestoring();
                    saveNetherZones(serverLevel);
                }
                FrozenChunkClientboundPacket.setBuildingDestroyedServerside(b.originPos);
                return true;
            }
            return false;
        });

        for (BuildingPlacement building : buildingsToDestroy)
            building.destroy(serverLevel);

        for (BuildingPlacement building : buildings)
            building.tick(serverLevel);

        for (NetherZone netherConversionZone : netherZones)
            netherConversionZone.tick(serverLevel);

        int nzSizeBefore = netherZones.size();
        netherZones.removeIf(NetherZone::isDone);
        int nzSizeAfter = netherZones.size();
        if (nzSizeBefore != nzSizeAfter) {
            saveNetherZones(serverLevel);
        }
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

        if (evt.getExplosion().getExploder() instanceof CreeperUnit cUnit) {
            creeperUnit = cUnit;
        }
        else if (evt.getExplosion().getExploder() instanceof PillagerUnit pUnit) {
            pillagerUnit = pUnit;
        } else if (evt.getExplosion().getExploder() instanceof LargeFireball fireball && fireball.getOwner() instanceof GhastUnit gUnit) {
            ghastUnit = gUnit;
        }

        if (exp.getExploder() == null && exp.getExploder() == null && ghastUnit == null) {
            evt.getAffectedEntities().clear();
        }

        // apply creeper, ghast and mounted pillager attack damage as bonus damage to buildings
        // this is dealt in addition to the actual blocks destroyed by the explosion itself
        if (creeperUnit != null || ghastUnit != null || pillagerUnit != null || exp.getExploder() instanceof PrimedTnt) {
            Set<BuildingPlacement> affectedBuildings = new HashSet<>();

            if (pillagerUnit != null) {
                Vec3 pos = evt.getExplosion().getPosition();
                evt.getAffectedBlocks().add(new BlockPos((int) pos.x, (int) pos.y - 1, (int) pos.z));
            }

            for (BlockPos bp : evt.getAffectedBlocks()) {
                BuildingPlacement building = BuildingUtils.findBuilding(false, bp);

                if (building != null) {
                    // prevent enemy ghasts friendly firing their own buildings
                    if (!(SurvivalServerEvents.isEnabled() && ghastUnit != null &&
                            SurvivalServerEvents.ENEMY_OWNER_NAME.equals(ghastUnit.getOwnerName()) &&
                            SurvivalServerEvents.ENEMY_OWNER_NAME.equals(building.ownerName)))
                        affectedBuildings.add(building);
                }
            }
            for (BuildingPlacement building : affectedBuildings) {
                int atkDmg = 0;
                if (ghastUnit != null) {
                    atkDmg = (int) ghastUnit.getUnitAttackDamage();
                    building.lastAttacker = ghastUnit;
                } else if (creeperUnit != null) {
                    atkDmg = (int) creeperUnit.getUnitAttackDamage();
                    if (creeperUnit.isPowered()) {
                        atkDmg *= CreeperUnit.CHARGED_DAMAGE_MULT;
                    }
                    building.lastAttacker = creeperUnit;
                } else if (pillagerUnit != null) {
                    atkDmg = (int) pillagerUnit.getUnitAttackDamage() / 2;
                    building.lastAttacker = pillagerUnit;
                } else if (exp.getExploder() instanceof PrimedTnt) {
                    atkDmg = TNT_BUILDING_BASE_DAMAGE;
                }

                if (atkDmg > 0) {
                    // all explosion damage will directly hit all occupants at an average of 1/4 rate
                    if (building instanceof GarrisonableBuilding garr && garr.getCapacity() > 0) {
                        for (LivingEntity le : garr.getOccupants())
                            le.hurt(exp.getDamageSource(), (random.nextInt(atkDmg + 1)) / 2f);
                    }

                    if (building instanceof BridgePlacement) {
                        atkDmg /= 2;
                    }

                    building.destroyRandomBlocks(atkDmg);
                }

            }
        }
        // don't do any block damage apart from the scripted building damage above or damage to leaves/tnt
        if (!serverLevel.getGameRules().getRule(GameRuleRegistrar.DO_UNIT_GRIEFING).get()) {
            evt.getAffectedBlocks().removeIf(bp -> {
                BlockState bs = evt.getLevel().getBlockState(bp);
                return !(bs.getBlock() instanceof LeavesBlock) && !(bs.getBlock() instanceof TntBlock);
            });
        }

    }

    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent evt) {
        BuildingPlacement building = BuildingUtils.findBuilding(evt.getEntity().level().isClientSide(), evt.getEntity().getOnPos());
        if (building != null) {
            evt.setCanceled(true);

            if (evt.getEntity() instanceof ServerPlayer player &&
                !player.isSpectator() &&
                (AlliancesServerEvents.isAllied(player.getName().getString(), building.ownerName) ||
                building.getBuilding() instanceof NeutralTransportPortal ||
                player.getName().getString().equals(building.ownerName)) &&
                building instanceof PortalPlacement portal &&
                portal.hasDestination()) {

                player.teleportTo(portal.destination.getX(), portal.destination.getY(), portal.destination.getZ());
                building.level.playSound(null, building.centrePos, SoundEvents.ENDERMAN_TELEPORT,
                        player.getSoundSource(), 1.0F, 1.0F);
                building.level.playSound(null, portal.destination, SoundEvents.ENDERMAN_TELEPORT,
                        player.getSoundSource(), 1.0F, 1.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onCropTrample(BlockEvent.FarmlandTrampleEvent evt) {
        if (BuildingUtils.isPosInsideAnyBuilding(evt.getEntity().level().isClientSide(), evt.getPos())) {
            evt.setCanceled(true);
        }
    }

    public static void replaceClientBuilding(BlockPos buildingPos) {
        if (!PlayerServerEvents.rtsSyncingEnabled) {
            return;
        }
        for (BuildingPlacement building : buildings) {
            if (building.originPos.equals(buildingPos)) {
                BuildingClientboundPacket.placeBuilding(
                        building.originPos,
                        building.getBuilding(),
                        building.rotation,
                        building.ownerName,
                        building.blockPlaceQueue.size(),
                        building instanceof BridgePlacement bridge && bridge.isDiagonalBridge,
                        building.getUpgradeLevel(),
                        building.isBuilt,
                        building instanceof PortalPlacement p ? p.getPortalType() : PortalPlacement.PortalType.BASIC,
                        building instanceof PortalPlacement p && p.getPortalType() == PortalPlacement.PortalType.TRANSPORT ? p.destination : new BlockPos(0,0,0),
                        false
                );
                return;
            }
        }
    }

    private static final float MIN_NETHER_BLOCKS_PERCENT = 0.8f;

    public static boolean isOnNetherBlocks(List<BuildingBlock> blocks, BlockPos originPos, ServerLevel level) {
        int netherBlocksBelow = 0;
        int blocksBelow = 0;
        for (BuildingBlock block : blocks) {
            if (block.getBlockPos().getY() == originPos.getY() + 1 && level != null) {
                BlockPos bp = block.getBlockPos();
                BlockState bs = block.getBlockState(); // building block

                if (bs.isSolid()) {
                    blocksBelow += 1;
                    if (NetherBlocks.isNetherBlock(level, bp.below())) {
                        netherBlocksBelow += 1;
                    }
                }
            }
        }
        if (blocksBelow <= 0) {
            return false; // avoid division by 0
        }
        return ((float) netherBlocksBelow / (float) blocksBelow) > MIN_NETHER_BLOCKS_PERCENT;
    }

    // does the player own one of these buildings?
    public static boolean playerHasFinishedBuilding(Building building, String playerName) {
        for (BuildingPlacement bpl : buildings) {
            if (bpl.getBuilding().isTypeOf(building) && bpl.isBuilt &&
                    (bpl.ownerName.equals(playerName))) {
                return true;
            }
        }
        return false;
    }
}
