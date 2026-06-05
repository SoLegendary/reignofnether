package com.solegendary.reignofnether.unit;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.building.buildings.placements.GraveyardPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
import com.solegendary.reignofnether.building.buildings.shared.AbstractFarm;
import com.solegendary.reignofnether.building.buildings.villagers.IronGolemBuilding;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.hero.HeroServerboundPacket;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.*;
import com.solegendary.reignofnether.unit.packets.UnitActionServerboundPacket;
import com.solegendary.reignofnether.unit.packets.UnitSyncServerboundPacket;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import com.solegendary.reignofnether.unit.units.piglins.HeadhunterUnit;
import com.solegendary.reignofnether.unit.units.piglins.HoglinUnit;
import com.solegendary.reignofnether.unit.units.villagers.*;
import com.solegendary.reignofnether.util.ArrayUtil;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

import static com.solegendary.reignofnether.building.BuildingClientEvents.getPlayerToBuildingRelationship;
import static com.solegendary.reignofnether.cursor.CursorClientEvents.getPreselectedBlockPos;
import static com.solegendary.reignofnether.hud.HudClientEvents.hudSelectedEntity;
import static com.solegendary.reignofnether.unit.Checkpoint.CHECKPOINT_TICKS_FADE;
import static net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES;


public class UnitClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    // list of vecs used in RenderChunkRegionMixin to replace leaf rendering
    private static final int WINDOW_RADIUS = 5; // size of area to hide leaves
    public static final int WINDOW_UPDATE_TICKS_MAX = 5; // size of area to hide leaves
    public static final List<ArrayList<Vec3>> unitWindowVecs = Collections.synchronizedList(new ArrayList<>());
    public static final List<BlockPos> windowPositions = Collections.synchronizedList(new ArrayList<>());
    public static int windowUpdateTicks = UnitClientEvents.WINDOW_UPDATE_TICKS_MAX;

    // list of ids that correspond to idle workers - should only be updated from server side
    public static final ArrayList<Integer> idleWorkerIds = new ArrayList<>();

    // units moused over or inside a box select
    private static final ArrayList<LivingEntity> preselectedUnits = new ArrayList<>();
    // units selected by click or box select
    private static final ArrayList<LivingEntity> selectedUnits = new ArrayList<>();
    private static ArrayList<LivingEntity> sortedSelectedUnits = new ArrayList<>();
    private static boolean sortedSelectedUnitsChanged = true;
    // tracking of all existing units
    private static final ArrayList<LivingEntity> allUnits = new ArrayList<>();

    @Nullable
    private static UnitActionItem lastClientUAIActioned = null;

    public static ArrayList<LivingEntity> getPreselectedUnits() {
        return preselectedUnits;
    }

    public static ArrayList<LivingEntity> getSelectedUnits() {
        return selectedUnits;
    }

    private static void markSelectedUnitsChanged() {
        sortedSelectedUnitsChanged = true;
    }



    public static ArrayList<LivingEntity> getSortedSelectedUnits() {
        if (!sortedSelectedUnitsChanged) {
            return sortedSelectedUnits;
        }
        ArrayList<LivingEntity> units = new ArrayList<>(UnitClientEvents.getSelectedUnits());
        units.sort((a, b) -> {
            var isHeroA = a instanceof HeroUnit;
            var isHeroB = b instanceof HeroUnit;
            if (isHeroA && !isHeroB) return -1;
            if (!isHeroA && isHeroB) return 1;
            return HudClientEvents.getModifiedEntityName(a).compareTo(HudClientEvents.getModifiedEntityName(b));
        });
        sortedSelectedUnits = units;
        sortedSelectedUnitsChanged = false;
        return units;
    }

    public static ArrayList<LivingEntity> getAllUnits() {
        return allUnits;
    }

    private static boolean rightClickMoveDeferred = false;
    private static boolean rightClickActionTaken = false;

    public static void addPreselectedUnit(LivingEntity unit) {
        if (unit instanceof Player player && (player.isSpectator() || player.isCreative()))
            return;
        if (!FogOfWarClientEvents.isInBrightChunk(unit))
            return;
        if (unit.isPassenger())
            return;
        preselectedUnits.add(unit);
        markSelectedUnitsChanged();
    }
    public static void addSelectedUnit(LivingEntity unit) {
        CursorClientEvents.setLeftClickAction(null);
        if (!FogOfWarClientEvents.isInBrightChunk(unit))
            return;
        if (unit.isPassenger())
            return;
        selectedUnits.add(unit);
        selectedUnits.sort(Comparator.comparing(MiscUtil::getSimpleEntityName));
        selectedUnits.sort(Comparator.comparing(Entity::getId));
        BuildingClientEvents.clearSelectedBuildings();
        NonUnitClientEvents.isMoveCheckpointGreen = true;
        markSelectedUnitsChanged();
    }
    public static void clearPreselectedUnits() {
        preselectedUnits.clear();
    }
    public static void clearSelectedUnits() {
        selectedUnits.clear();
        NonUnitClientEvents.isMoveCheckpointGreen = true;
    }

    private static long lastLeftClickTime = 0; // to track double clicks
    private static final long DOUBLE_CLICK_TIME_MS = 500;

    private static boolean isLeftClickAttack() {
        return CursorClientEvents.getLeftClickAction() == UnitAction.ATTACK;
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent evt) {
        if (evt.getLevel().isClientSide())
            selectedUnits.removeIf(e -> e.getId() == evt.getEntityMounting().getId());
        markSelectedUnitsChanged();
    }

    public static int getCurrentPopulation(String playerName) {
        int currentPopulation = 0;
        if (MC.level != null) {
            for (LivingEntity entity : allUnits) {
                if (entity instanceof Unit unit)
                    if (unit.getOwnerName().equals(playerName))
                        currentPopulation += unit.getCost().population;
            }
            for (BuildingPlacement building : BuildingClientEvents.getBuildings())
                if (building.ownerName.equals(playerName))
                    if (building instanceof ProductionPlacement prodBuilding) {
                        for (ActiveProduction prodItem : prodBuilding.productionQueue)
                            if (!(prodBuilding instanceof GraveyardPlacement gy) || gy.getUpgradeLevel() <= 0)
                                currentPopulation += prodItem.item.getCost(true, playerName).population;
                    } else if (building.getBuilding() instanceof IronGolemBuilding) {
                        currentPopulation += ResourceCosts.IRON_GOLEM.population;
                    }
        }
        return currentPopulation;
    }

    public static void sendUnitCommandManual(String playerName, UnitAction action, int unitId, int[] unitIds,
                                             BlockPos preselectedBlockPos, BlockPos selectedBuildingPos) {
        if (MC.player != null && playerName.equals(MC.player.getName().getString()))
            sendUnitCommandManual(action, unitId, unitIds, preselectedBlockPos, selectedBuildingPos);
    }

    public static void sendUnitCommandManual(UnitAction action, int unitId, int[] unitIds,
                                             BlockPos preselectedBlockPos) {
        sendUnitCommandManual(action, unitId, unitIds, preselectedBlockPos, new BlockPos(0,0,0));
    }

    public static void sendUnitCommandManual(UnitAction action, int unitId, int[] unitIds,
                                             BlockPos preselectedBlockPos, boolean clientside, boolean serverside) {
        sendUnitCommandManual(action, unitId, unitIds, preselectedBlockPos,
                new BlockPos(0,0,0), clientside, serverside);
    }

    public static void sendUnitCommandManual(UnitAction action, int unitId, int[] unitIds) {
        sendUnitCommandManual(action, unitId, unitIds,
                new BlockPos(0,0,0),
                new BlockPos(0,0,0));
    }

    public static void sendUnitCommandManual(UnitAction action, int[] unitIds) {
        sendUnitCommandManual(action, -1, unitIds,
                new BlockPos(0,0,0),
                new BlockPos(0,0,0));
    }

    public static void sendUnitCommandManual(UnitAction action, int unitId, int[] unitIds,
                                             BlockPos preselectedBlockPos, BlockPos selectedBuildingPos) {
        if (MC.player != null) {
            UnitActionItem actionItem = new UnitActionItem(
                MC.player.getName().getString(),
                action, unitId, unitIds,
                preselectedBlockPos,
                selectedBuildingPos
            );
            // prevent spam clicking the same action repeatedly
            if (!actionItem.equals(lastClientUAIActioned)) {
                actionItem.action(MC.level);
                lastClientUAIActioned = actionItem;
            }

            PacketHandler.INSTANCE.sendToServer(new UnitActionServerboundPacket(
                MC.player.getName().getString(),
                action, unitId, unitIds,
                preselectedBlockPos,
                selectedBuildingPos,
                Keybindings.shiftMod.isDown()
            ));
        }
    }

    public static void sendUnitCommandManual(UnitAction action, int unitId, int[] unitIds,
                                             BlockPos preselectedBlockPos, BlockPos selectedBuildingPos,
                                             boolean clientside, boolean serverside) {
        if (MC.player != null) {
            if (clientside) {
                UnitActionItem actionItem = new UnitActionItem(
                        MC.player.getName().getString(),
                        action, unitId, unitIds,
                        preselectedBlockPos,
                        selectedBuildingPos
                );
                actionItem.action(MC.level);
            }
            if (serverside) {
                PacketHandler.INSTANCE.sendToServer(new UnitActionServerboundPacket(
                        MC.player.getName().getString(),
                        action, unitId, unitIds,
                        preselectedBlockPos,
                        selectedBuildingPos,
                        Keybindings.shiftMod.isDown()
                ));
            }
        }
    }

    public static void sendUnitCommand(UnitAction action) {
        sendUnitCommand(action, null);
    }

    public static void sendUnitCommand(UnitAction action, Predicate<LivingEntity> includeUnit) {
        BlockPos bp = getPreselectedBlockPos();

        if (action.name().toLowerCase().contains("startrts")) {
            if (action == UnitAction.STARTRTS_VILLAGERS) {
                PlayerServerboundPacket.startRTS(Faction.VILLAGERS, (double) bp.getX(), (double) bp.getY(), (double) bp.getZ());
            } else if (action == UnitAction.STARTRTS_MONSTERS) {
                PlayerServerboundPacket.startRTS(Faction.MONSTERS, (double) bp.getX(), (double) bp.getY(), (double) bp.getZ());
            } else if (action == UnitAction.STARTRTS_PIGLINS) {
                PlayerServerboundPacket.startRTS(Faction.PIGLINS, (double) bp.getX(), (double) bp.getY(), (double) bp.getZ());
            }
            return;
        }
        else if (action.name().toLowerCase().contains("sandbox_spawn")) {
            if (action == UnitAction.STARTRTS_VILLAGERS) {
                PlayerServerboundPacket.startRTS(Faction.VILLAGERS, (double) bp.getX(), (double) bp.getY(), (double) bp.getZ());
            } else if (action == UnitAction.STARTRTS_MONSTERS) {
                PlayerServerboundPacket.startRTS(Faction.MONSTERS, (double) bp.getX(), (double) bp.getY(), (double) bp.getZ());
            } else if (action == UnitAction.STARTRTS_PIGLINS) {
                PlayerServerboundPacket.startRTS(Faction.PIGLINS, (double) bp.getX(), (double) bp.getY(), (double) bp.getZ());
            }
            return;
        }

        if (MC.player != null) {
            var selUnits = new LinkedList<LivingEntity>();
            loop:
            for (LivingEntity livingEntity : selectedUnits) {
                if (livingEntity instanceof Unit unit) {
                    for (Ability ability : unit.getAbilities().get()) {
                        if (ability.isCasting(unit) && ability.oneClickOneUse && ability.action == action) continue loop;
                    }
                } else if (!NonUnitClientEvents.canControlAllMobs()) {
                    continue;
                }
                if (includeUnit == null || includeUnit.test(livingEntity))
                    selUnits.add(livingEntity);
            }
            String playerName = MC.player.getName().getString();

            UnitActionItem actionItem = new UnitActionItem(
                playerName,
                action,
                    !preselectedUnits.isEmpty() ? preselectedUnits.get(0).getId() : -1,
                    ArrayUtil.livingEntityListToIdArray(selUnits),
                bp,
                HudClientEvents.hudSelectedPlacement != null ? HudClientEvents.hudSelectedPlacement.originPos : new BlockPos(0,0,0)
            );
            actionItem.action(MC.level);

            PacketHandler.INSTANCE.sendToServer(new UnitActionServerboundPacket(
                playerName,
                action,
                    !preselectedUnits.isEmpty() ? preselectedUnits.get(0).getId() : -1,
                    ArrayUtil.livingEntityListToIdArray(selUnits),
                bp,
                HudClientEvents.hudSelectedPlacement != null ? HudClientEvents.hudSelectedPlacement.originPos : new BlockPos(0,0,0),
                Keybindings.shiftMod.isDown()
            ));
        }
    }

    private static void resolveMoveAction() {
        if (CursorClientEvents.isRightDragCouldStart() && !selectedUnits.isEmpty()) {
            rightClickMoveDeferred = true;
            return;
        }
        doResolveMoveAction();
    }

    private static void resolveMoveActionDeferred() {
        if (!selectedUnits.isEmpty()) {
            rightClickMoveDeferred = true;
            return;
        }
        doResolveMoveAction();
    }

    private static void doResolveMoveAction() {
        // follow friendly unit
        if (preselectedUnits.size() == 1 && !targetingSelf()) {
            if (hudSelectedEntity instanceof WitchUnit witchUnit) {
                sendUnitCommand(UnitAction.THROW_LINGERING_REGEN_POTION);
            } else {
                sendUnitCommand(UnitAction.FOLLOW);
            }
        }
        // move to ground pos (disabled during camera manip)
        else if (!Keybindings.altMod.isDown() && !selectedUnits.isEmpty() && MC.level != null) {
            ResourceName resName = ResourceSources.getBlockResourceName(getPreselectedBlockPos(), MC.level);
            boolean isGathering = hudSelectedEntity instanceof WorkerUnit && resName != ResourceName.NONE;

            sendUnitCommand(UnitAction.MOVE);

            for (LivingEntity le : selectedUnits)
                if (!isGathering && le instanceof Unit unit && unit.getMoveGoal() != null)
                    unit.getMoveGoal().lastSelectedMoveTarget = getPreselectedBlockPos();
        }
    }

    public static ResourceName getSelectedUnitResourceTarget() {
        Entity entity = hudSelectedEntity;
        if (entity instanceof WorkerUnit workerUnit)
            return workerUnit.getGatherResourceGoal().getTargetResourceName();
        return ResourceName.NONE;
    }

    public static void syncOwnerName(int entityId, String ownerName) {
        for (LivingEntity entity : allUnits)
            if (entity.getId() == entityId && MC.level != null)
                if (entity instanceof Unit unit)
                    unit.setOwnerName(ownerName);
    }

    public static void syncScenarioRoleIndex(int entityId, int scenarioRoleIndex) {
        for (LivingEntity entity : allUnits)
            if (entity.getId() == entityId && MC.level != null)
                if (entity instanceof Unit unit)
                    unit.setScenarioRoleIndex(scenarioRoleIndex);
    }

    /**
     * Update data on a unit from serverside, mainly to ensure unit HUD data is up-to-date
     * Only try to update health and pos if out of view
     */
    public static void syncUnitStats(int entityId, float health, float absorb, Vec3 pos, String ownerName) {
        for (LivingEntity entity : allUnits) {
            if (entity.getId() == entityId && MC.level != null) {
                boolean isLoadedClientside = MC.level.getEntity(entityId) != null;
                if (!isLoadedClientside) {
                    entity.setHealth(health);
                    entity.setPos(pos);
                }
                entity.setAbsorptionAmount(absorb);
                MinimapClientEvents.removeMinimapUnit(entityId);
                return;
            }
        }
        // if the unit doesn't exist at all clientside, create a MinimapUnit to at least track its minimap position
        MinimapClientEvents.syncMinimapUnits(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), entityId, ownerName);
    }

    public static void syncWorkerUnit(int entityId, boolean isBuilding, ResourceName gatherName, BlockPos gatherPos, int gatherTicks) {
        for(LivingEntity entity : allUnits) {
            if (entity.getId() == entityId && MC.level != null) {
                if (entity instanceof WorkerUnit workerUnit) {
                    workerUnit.getBuildRepairGoal().setIsBuildingServerside(isBuilding);
                    workerUnit.getGatherResourceGoal().syncFromServer(gatherName, gatherPos, gatherTicks);
                }
            }
        }
    }

    public static void syncUnitResources(int entityId, Resources res) {
        for(LivingEntity entity : allUnits) {
            if (entity.getId() == entityId && MC.level != null) {
                if (entity instanceof Unit unit) {
                    unit.getItems().removeIf(i -> !ResourceSources.isPreparedFood(i.getItem()));
                    unit.getItems().add(new ItemStack(Items.SUGAR, res.food));
                    unit.getItems().add(new ItemStack(Items.STICK, res.wood));
                    unit.getItems().add(new ItemStack(Items.STONE, res.ore));
                }
            }
        }
    }

    public static void syncAnchorPos(int entityId, BlockPos bp) {
        for(LivingEntity entity : allUnits) {
            if (entity.getId() == entityId && MC.level != null) {
                if (entity instanceof Unit unit) {
                    unit.setAnchor(bp);
                    break;
                }
            }
        }
    }

    public static void removeAnchorPos(int entityId) {
        for(LivingEntity entity : allUnits) {
            if (entity.getId() == entityId && MC.level != null) {
                if (entity instanceof Unit unit) {
                    unit.setAnchor(null);
                    break;
                }
            }
        }
    }

    /*
    private static double variance = 0;
    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
                "var: " + variance,
        });
    }
     */

    private static final int VIS_CHECK_TICKS_MAX = 10;
    private static int ticksToNextVisCheck = VIS_CHECK_TICKS_MAX;
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        //if (MC.level != null)
        //    variance = WaveSpawner.getYVariance(MC.level, getPreselectedBlockPos(), 8);

        ticksToNextVisCheck -= 1;

        if (ticksToNextVisCheck <= 0) {
            ticksToNextVisCheck = VIS_CHECK_TICKS_MAX;

            // prevent selection of units out of view
            selectedUnits.removeIf(e -> !FogOfWarClientEvents.isInBrightChunk(e));
        }

        // calculate vecs used to hide leaf blocks around units
        if (MC.player != null && OrthoviewClientEvents.hideLeavesMethod == OrthoviewClientEvents.LeafHideMethod.AROUND_UNITS_AND_CURSOR &&
            OrthoviewClientEvents.isEnabled()) {

            synchronized (windowPositions) {
                windowPositions.clear();
                UnitClientEvents.getAllUnits().forEach(u -> {
                    if (FogOfWarClientEvents.isInBrightChunk(u))
                        windowPositions.add(u.getOnPos());
                });
                BlockPos cursorBp = CursorClientEvents.getPreselectedBlockPos();
                windowPositions.add(cursorBp);

                synchronized (unitWindowVecs) {
                    unitWindowVecs.clear();
                    windowPositions.forEach(bp -> {
                        float dist = Math.max(120, OrthoviewClientEvents.getZoom() * 2);
                        if (bp.distSqr(MC.player.getOnPos()) < (dist * dist))
                            unitWindowVecs.add(MyMath.prepIsPointInsideRect3d(Minecraft.getInstance(),
                                    new Vector3d(bp.getX() - WINDOW_RADIUS, bp.getY(), bp.getZ() - WINDOW_RADIUS), // tl
                                    new Vector3d(bp.getX() - WINDOW_RADIUS, bp.getY(), bp.getZ() + WINDOW_RADIUS), // bl
                                    new Vector3d(bp.getX() + WINDOW_RADIUS, bp.getY(), bp.getZ() + WINDOW_RADIUS)  // br
                            ));
                    });
                }
            }
        } else {
            synchronized (windowPositions) {
                windowPositions.clear();
            }
            synchronized (unitWindowVecs) {
                unitWindowVecs.clear();
            }
        }
        markSelectedUnitsChanged();
    }

    @SubscribeEvent
    public static void onEntityLeaveEvent(EntityLeaveLevelEvent evt) {
        try {
            synchronized (idleWorkerIds) {
                idleWorkerIds.removeIf(id -> id == evt.getEntity().getId());
            }
        } catch (Exception e) {
            ReignOfNether.LOGGER.warn("Error while trying to remove an idleWorkerId");
        }
    }

    /**
     * Clientside entities will join and leave based on render distance, but we want to keep entities tracked at all times
     * Therefore, only remove entities if they leave serverside via UnitClientboundPacket.
     */
    public static void onEntityLeave(int entityId) {
        selectedUnits.removeIf(e -> e.getId() == entityId);
        //System.out.println("selectedUnits removed entity: " + entityId);
        preselectedUnits.removeIf(e -> e.getId() == entityId);
        //System.out.println("preselectedUnits removed entity: " + entityId);
        allUnits.removeIf(e -> e.getId() == entityId);
        //System.out.println("allUnits removed entity: " + entityId);
        MinimapClientEvents.removeMinimapUnit(entityId);
        markSelectedUnitsChanged();
    }
    /**
     * Add and update entities from clientside action
     */

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        Entity entity = evt.getEntity();

        if (entity instanceof Unit unit && evt.getLevel().isClientSide) {
            TutorialClientEvents.updateStage();

            if (selectedUnits.removeIf(e -> e.getId() == entity.getId()))
                selectedUnits.add((LivingEntity) entity);
            if (preselectedUnits.removeIf(e -> e.getId() == entity.getId()))
                preselectedUnits.add((LivingEntity) entity);
            allUnits.removeIf(e -> e.getId() == entity.getId());
            allUnits.add((LivingEntity) entity);

            unit.initialiseGoals(); // for clientside data tracking - server automatically does this via registerGoals();
            unit.setupEquipmentAndUpgradesClient();

            addUnitPoofs(evt.getLevel(), entity);

            UnitSyncServerboundPacket.requestSyncAbilities(entity.getId());

            if (entity instanceof HeroUnit)
                HeroServerboundPacket.requestHeroSync(entity.getId());
        }
        if (entity instanceof LivingEntity le && (ResourceSources.isHuntableAnimal(le) || le instanceof PhantomSummon))
            addUnitPoofs(evt.getLevel(), entity);
        markSelectedUnitsChanged();
    }

    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Post evt) {
        if (!OrthoviewClientEvents.isEnabled()) return;
        if (MC.level == null) return;

        // prevent clicking behind HUDs
        if (HudClientEvents.isMouseOverAnyButtonOrHud()) {
            CursorClientEvents.setLeftClickAction(null);
            return;
        }

        // Can only detect clicks client side but only see and modify goals serverside so produce entity queues here
        // and consume in onWorldTick; we also can't add entities directly as they will not have goals populated
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {

            if (!selectedUnits.isEmpty() && isLeftClickAttack()) {
                // A + left click -> force attack single unit (even if friendly)
                if (preselectedUnits.size() == 1 && !targetingSelf()) {
                    sendUnitCommand(UnitAction.ATTACK);
                }
                // A + left click -> force attack building (even if friendly)
                else if (BuildingClientEvents.getPreselectedBuilding() != null)
                    sendUnitCommand(UnitAction.ATTACK_BUILDING);
                // A + left click -> attack move ground
                else
                    sendUnitCommand(UnitAction.ATTACK_MOVE);
            }

            // select all nearby units of the same type when the same unit is double-clicked
            // only works for owned units
            else if (selectedUnits.size() == 1 && MC.level != null && !Keybindings.shiftMod.isDown() &&
                    ((System.currentTimeMillis() - lastLeftClickTime) < DOUBLE_CLICK_TIME_MS || Keybindings.ctrlMod.isDown()) &&
                     !preselectedUnits.isEmpty() && selectedUnits.contains(preselectedUnits.get(0))) {

                lastLeftClickTime = 0;
                LivingEntity selectedUnit = selectedUnits.get(0);
                List<? extends LivingEntity> nearbyEntities = MiscUtil.getEntitiesWithinRange(
                        new Vector3d(selectedUnit.position().x, selectedUnit.position().y, selectedUnit.position().z),
                        OrthoviewClientEvents.getZoom(),
                        selectedUnits.get(0).getClass(),
                        MC.level
                );
                if (getPlayerToEntityRelationship(selectedUnit) == Relationship.OWNED ||
                        NonUnitClientEvents.canControlAllMobs() ||
                        AlliancesClient.canControlAlly(selectedUnit)) {

                    for (LivingEntity entity : nearbyEntities) {
                        boolean bothVillagers = entity instanceof VillagerUnit &&
                                                selectedUnit instanceof VillagerUnit;
                        boolean sameProfession = entity instanceof VillagerUnit vUnit1 &&
                                                selectedUnit instanceof VillagerUnit vUnit2 &&
                                                vUnit1.getUnitProfession() == vUnit2.getUnitProfession();
                        boolean garrisoned1 = selectedUnit instanceof Unit unit1 && GarrisonableBuildingAddon.getGarrison(unit1) != null;
                        boolean garrisoned2 = entity instanceof Unit unit2 && GarrisonableBuildingAddon.getGarrison(unit2) != null;
                        boolean garrionStatusMatches = (garrisoned1 && garrisoned2) || (!garrisoned1 && !garrisoned2);
                        if (entity == selectedUnit) continue;
                        if ((getPlayerToEntityRelationship(entity) == Relationship.OWNED ||
                                NonUnitClientEvents.canControlAllMobs() ||
                                AlliancesClient.canControlAlly(entity)) &&
                                (!bothVillagers || sameProfession) && garrionStatusMatches) {
                            addSelectedUnit(entity);
                        }
                    }
                    HudClientEvents.setLowestCdHudEntity();
                }
            }
            // move on left click
            else if (CursorClientEvents.getLeftClickAction() == UnitAction.MOVE)
                resolveMoveAction();
            // resolve any other abilities not explicitly covered here
            else if (CursorClientEvents.getLeftClickAction() != null && MC.player != null) {
                sendUnitCommand(CursorClientEvents.getLeftClickAction());
            }
            /*else if (
                    !selectedUnits.isEmpty() &&
                    BuildingClientEvents.getPreselectedBuilding() == null &&
                    preselectedUnits.isEmpty() &&
                    !BuildingClientEvents.isBuilt
            ) {
                clearSelectedUnits();
            }*/

            // left click -> select a single unit
            // if shift is held, deselect a unit or add it to the selected group
            else if (preselectedUnits.size() == 1 && !isLeftClickAttack()) {
                boolean deselected = false;

                if (Keybindings.shiftMod.isDown())
                    deselected = selectedUnits.removeIf(id -> id.equals(preselectedUnits.get(0)));

                if (Keybindings.shiftMod.isDown() && !deselected &&
                    ((preselectedUnits.get(0) instanceof Unit && getPlayerToEntityRelationship(preselectedUnits.get(0)) == Relationship.OWNED) ||
                    AlliancesClient.canControlAlly(preselectedUnits.get(0)) ||
                    NonUnitClientEvents.canControlAllMobs())) {
                        addSelectedUnit(preselectedUnits.get(0));
                }
                else if (!deselected) { // select a single unit - this should be the only code path that allows you to select a non-owned unit
                    clearSelectedUnits();
                    addSelectedUnit(preselectedUnits.get(0));
                }
            }
            // deselect any non-owned units if we managed to select them with owned units
            // and disallow selecting > 1 non-owned unit or the client player
            if (selectedUnits.size() > 1) {
                selectedUnits.removeIf(e ->
                    (getPlayerToEntityRelationship(e) != Relationship.OWNED && !NonUnitClientEvents.canControlAllMobs() && !AlliancesClient.canControlAlly(e)) ||
                            e.getId() == MC.player.getId()
                );
            }
            BuildingClientEvents.isBuilt = false;
            lastLeftClickTime = System.currentTimeMillis();
        }
        else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            //UnitClientEvents.sendUnitCommand(UnitAction.DEBUG1);

            if (BuildingClientEvents.getBuildingToPlace() != null) {
                BuildingClientEvents.setBuildingToPlace(null);
                return;
            }
			if (MinimapClientEvents.isPointInsideMinimap(evt.getMouseX(), evt.getMouseY()))
				return;

            rightClickActionTaken = false;
            if (!selectedUnits.isEmpty()) {
                BuildingPlacement preSelBuilding = BuildingClientEvents.getPreselectedBuilding();

                // right click -> mount friendly unit
                UnitAction mountAction = null;
                if (preselectedUnits.size() == 1) {
                    mountAction = getMountAction(hudSelectedEntity, preselectedUnits.get(0));
                }
                if (mountAction != null) {
                    sendUnitCommand(mountAction);
                    rightClickActionTaken = true;
                }
                // right click -> garrison friendly building
                else if (preSelBuilding != null && preSelBuilding.getBuilding().hasActiveAddon(GarrisonableBuildingAddon.class) &&
                        hudSelectedEntity instanceof RangedAttackerUnit &&
                        hudSelectedEntity instanceof Unit unit && unit.canGarrison() &&
                        preSelBuilding.ownerName.equals(unit.getOwnerName())) {
                    sendUnitCommand(UnitAction.GARRISON);
                    rightClickActionTaken = true;
                }
                // right click -> attack unfriendly unit
                else if (preselectedUnits.size() == 1 &&
                    !targetingSelf() &&
                    (hudSelectedEntity instanceof Unit || NonUnitClientEvents.canAttack(hudSelectedEntity)) &&
                    ((GameruleClient.neutralAggro && getPlayerToEntityRelationship(preselectedUnits.get(0)) == Relationship.NEUTRAL) ||
                    getPlayerToEntityRelationship(preselectedUnits.get(0)) == Relationship.HOSTILE ||
                     ResourceSources.isHuntableAnimal(preselectedUnits.get(0)))) {

                     if (hudSelectedEntity instanceof WitchUnit witchUnit) {
                         sendUnitCommand(UnitAction.THROW_LINGERING_HARMING_POTION);
                     } else {
                         sendUnitCommand(UnitAction.ATTACK);
                     }
                     rightClickActionTaken = true;
                }
                // right click -> attack unfriendly building
                else if (hudSelectedEntity instanceof AttackerUnit &&
                        (preSelBuilding != null) &&
                        !preSelBuilding.getBuilding().invulnerable &&
                        !(preSelBuilding.getBuilding() instanceof AbstractBridge) &&
                        ((GameruleClient.neutralAggro && getPlayerToBuildingRelationship(preSelBuilding) == Relationship.NEUTRAL) ||
                        getPlayerToBuildingRelationship(preSelBuilding) == Relationship.HOSTILE)) {
                    sendUnitCommand(UnitAction.ATTACK_BUILDING);
                    rightClickActionTaken = true;
                }
                // right click -> return resources
                else if (hudSelectedEntity instanceof Unit unit &&
                        unit.getReturnResourcesGoal() != null &&
                        Resources.getTotalResourcesFromItems(unit.getItems()).getTotalValue() > 0 &&
                        preSelBuilding != null && preSelBuilding.getBuilding().canAcceptResources && preSelBuilding.isBuilt &&
                        unit.getOwnerName().equals(preSelBuilding.ownerName)) {
                    sendUnitCommand(UnitAction.RETURN_RESOURCES);
                    rightClickActionTaken = true;
                }
                // right click -> build or repair preselected building
                else if (hudSelectedEntity instanceof WorkerUnit && preSelBuilding != null &&
                        (getPlayerToBuildingRelationship(preSelBuilding) == Relationship.OWNED || AlliancesClient.canControlAlly(hudSelectedEntity)) ||
                        (preSelBuilding != null && preSelBuilding.getBuilding() instanceof AbstractBridge)) {

                    if (preSelBuilding.getBuilding() instanceof AbstractFarm && preSelBuilding.isBuilt)
                        sendUnitCommand(UnitAction.FARM);
                    else if (BuildingUtils.isBuildingBuildable(true, preSelBuilding))
                        sendUnitCommand(UnitAction.BUILD_REPAIR);
                    else
                        resolveMoveActionDeferred();
                }
                // right click -> follow friendly unit or go to preselected blockPos
                else
                    resolveMoveActionDeferred();
            }
        }
        // clear all cursor actions
        CursorClientEvents.setLeftClickAction(null);
        markSelectedUnitsChanged();
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre evt) {
        if (!OrthoviewClientEvents.isEnabled() || MC.level == null)
            return;

        if (Keybindings.altMod.isDown())
            return;

        if (!CursorClientEvents.isRightDragActive())
            return;

        // don't allow formation drag if an attack command was issued on right-click press
        if (rightClickActionTaken)
            return;

        ArrayList<LivingEntity> selUnits = getSelectedUnits();
        if (selUnits.isEmpty())
            return;

        BlockPos currentBp = CursorClientEvents.getPreselectedBlockPos();
        ArrayList<LivingEntity> actionableUnits = new ArrayList<>();
        for (LivingEntity unit : selUnits) {
            if ((getPlayerToEntityRelationship(unit) == Relationship.OWNED ||
                    NonUnitClientEvents.canControlAllMobs() ||
                    AlliancesClient.canControlAlly(unit)) && unit instanceof Unit) {
                actionableUnits.add(unit);
            }
        }

        if (actionableUnits.isEmpty())
            return;

        if (!FormationDragMove.isDragging()) {
            FormationDragMove.startDrag(CursorClientEvents.getRightClickStartBp());
        }

        FormationDragMove.updateDrag(currentBp, actionableUnits.size(), MC.level);
    }

    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseButtonReleased.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() || MC.level == null)
            return;

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
		if (MinimapClientEvents.minimapRightClickDown)
			return;
            if (Keybindings.altMod.isDown() && FormationDragMove.isDragging()) {
                FormationDragMove.cancelDrag();
            }
            boolean wasFormationDrag = FormationDragMove.isDragging();
            if (wasFormationDrag) {
                ArrayList<LivingEntity> selUnits = getSelectedUnits();
                ArrayList<LivingEntity> actionableUnits = new ArrayList<>();
                for (LivingEntity unit : selUnits) {
                    if ((getPlayerToEntityRelationship(unit) == Relationship.OWNED ||
                            NonUnitClientEvents.canControlAllMobs() ||
                            AlliancesClient.canControlAlly(unit)) && unit instanceof Unit) {
                        actionableUnits.add(unit);
                    }
                }
                resolveFormationMove(actionableUnits, Keybindings.shiftMod.isDown());
            }
            if (rightClickMoveDeferred) {
                rightClickMoveDeferred = false;
                if (!wasFormationDrag) {
                    doResolveMoveAction();
                }
            }
        }
    }

    private static void resolveFormationMove(ArrayList<LivingEntity> units, boolean queueOrders) {
        if (MC.player == null || MC.level == null) return;

        List<com.mojang.datafixers.util.Pair<LivingEntity, BlockPos>> pairs = FormationDragMove.endDrag(units);
        String playerName = MC.player.getName().getString();

        for (var pair : pairs) {
            LivingEntity le = pair.getFirst();
            BlockPos targetBp = pair.getSecond();
            if (le instanceof Unit unit) {
                int[] singleUnitId = new int[]{le.getId()};

                if (!queueOrders) {
                    new UnitActionItem(
                        playerName,
                        UnitAction.MOVE, -1, singleUnitId,
                        targetBp,
                        new BlockPos(0, 0, 0)
                    ).action(MC.level);
                } else {
                    MiscUtil.addUnitCheckpoint(unit, targetBp, true);
                }

                PacketHandler.INSTANCE.sendToServer(new UnitActionServerboundPacket(
                    playerName,
                    UnitAction.MOVE, -1, singleUnitId,
                    targetBp,
                    new BlockPos(0, 0, 0),
                    queueOrders
                ));
            }
        }
    }

    public static RenderLevelStageEvent.Stage stage = AFTER_ENTITIES;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (MC.level == null)
            return;
        if (evt.getStage() == stage) {
            ArrayList<LivingEntity> selectedUnits = getSelectedUnits();
            ArrayList<LivingEntity> preselectedUnits = getPreselectedUnits();

            Set<Entity> unitsToDraw = new HashSet<>();
            unitsToDraw.addAll(selectedUnits);
            unitsToDraw.addAll(preselectedUnits);

            // draw outlines on all (pre)selected units but only draw once per unit based on conditions
            // don't render preselection outlines if mousing over HUD
            var vertexConsumer = MC.renderBuffers().bufferSource().getBuffer(MyRenderer.LINES_NO_DEPTH_TEST);
            if (OrthoviewClientEvents.isEnabled()) {
                // evaluate conditions that will remain constant during the rendering stage
                boolean isMouseOverAnyButtonOrHud = HudClientEvents.isMouseOverAnyButtonOrHud();
                boolean isLeftClickAttack = isLeftClickAttack();
                boolean targetingSelf = targetingSelf();
                boolean isRightClickDown = MiscUtil.isRightClickDown(MC);
                // render outline for each selected and preselected entities
                for (Entity entity : unitsToDraw) {
                    if (!FogOfWarClientEvents.isInBrightChunk(entity))
                        continue;

                    AABB entityAABB = entity.getBoundingBox();
                    if (entity instanceof Unit unit) {
                        entityAABB = unit.getInflatedSelectionBox();
                    }

                    boolean isPreselected = preselectedUnits.contains(entity);
                    boolean isSelected = selectedUnits.contains(entity);

                    if (isPreselected && isLeftClickAttack && !targetingSelf && !isMouseOverAnyButtonOrHud)
                        MyRenderer.drawLineBoxOutlineOnly(evt.getPoseStack(), vertexConsumer, entityAABB, 1.0f, 0.3f, 0.3f, 1.0f, false);
                    else if (isSelected)
                        MyRenderer.drawLineBoxOutlineOnly(evt.getPoseStack(), vertexConsumer, entityAABB, 1.0f, 1.0f, 1.0f, 1.0f, false);
                    else if (isPreselected && !isMouseOverAnyButtonOrHud)
                        MyRenderer.drawLineBoxOutlineOnly(evt.getPoseStack(), vertexConsumer, entityAABB, 1.0f, 1.0f, 1.0f, isRightClickDown ? 1.0f : 0.5f, false);
                }
            }

            var selectedEntityIds = new HashSet<>();
            for (LivingEntity selectedUnit : selectedUnits) {
                Integer id = selectedUnit.getId();
                selectedEntityIds.add(id);
            }
            for (LivingEntity entity : allUnits) {
                if (!FogOfWarClientEvents.isInBrightChunk(entity) ||
                        entity.isPassenger())
                    continue;

                float alpha = 0.5f;
                if (selectedEntityIds.contains(entity.getId()))
                    alpha = 1.0f;

                // draw only the bottom of the outline boxes
                AABB entityAABB = entity.getBoundingBox();
                if (entity instanceof Unit unit) {
                    entityAABB = unit.getInflatedSelectionBox();
                }
                entityAABB = entityAABB.setMaxY(entityAABB.minY);
                boolean excludeMaxY = OrthoviewClientEvents.isEnabled();

                Color colorHex;
                if (entity instanceof Unit unit) {
                    if (PlayerClientEvents.isRTSPlayer(unit.getOwnerName())) {
                        colorHex = new Color(PlayerColors.getPlayerDisplayColorHex(unit.getOwnerName()));
                    } else {
                        colorHex = new Color(PlayerColors.COLOR_GRAY.hexCode, false);
                    }
                } else {
                    colorHex = new Color(0xFFFFFF, false);
                }

                float r = colorHex.getRed() / 255.0f;
                float g = colorHex.getGreen() / 255.0f;
                float b = colorHex.getBlue() / 255.0f;

                // always-shown highlights to indicate unit relationships
                if (OrthoviewClientEvents.isEnabled()) {
                    MyRenderer.drawLineBoxOutlineOnly(evt.getPoseStack(), vertexConsumer, entityAABB, 1.0f, 1.0f, 1.0f, alpha, excludeMaxY);
                }

                MyRenderer.drawBoxBottom(evt.getPoseStack(), entityAABB, r, g, b, 0.5f);
            }

            // render items in front of face for eating units
            for (LivingEntity entity : getAllUnits()) {
                if (entity instanceof Unit unit && unit.isEatingFood()) {
                    MyRenderer.renderItemInFrontOfEntityFace(evt.getPoseStack(), entity, evt.getPartialTick(), new ItemStack(unit.getFoodBeingEaten()));
                }
            }
        }

        // AFTER_CUTOUT_BLOCKS lets us see checkpoints through leaves
        if (OrthoviewClientEvents.isEnabled() && evt.getStage() == stage) {
            VertexConsumer vertexConsumerLine = MC.renderBuffers().bufferSource().getBuffer(RenderType.LINE_STRIP);
            ResourceLocation rl = ResourceLocation.parse("forge:textures/white.png");
            VertexConsumer vertexConsumerEntityTranslucent = MC.renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(rl));
            // draw unit checkpoints
            for (LivingEntity entity : getSelectedUnits()) {
                if (entity instanceof Unit unit) {
                    float entityYOffset1 = 1.74f - ((LivingEntity) unit).getEyeHeight() - 1;
                    Vec3 firstPos = ((LivingEntity) unit).getEyePosition().add(0, entityYOffset1,0);
                    Vec3 lastPos = firstPos;

                    for (Checkpoint cp : unit.getCheckpoints()) {
                        int ticksUnderFade = Math.min(cp.ticksLeft, CHECKPOINT_TICKS_FADE);
                        float a = ((float) ticksUnderFade / (float) CHECKPOINT_TICKS_FADE) * 0.5f;
                        if (cp.isForEntity()) {
                            MyRenderer.drawLine(evt.getPoseStack(), vertexConsumerLine, lastPos, cp.getPos(), cp.isGreen ? 0 : 1, cp.isGreen ? 1 : 0, 0, a);
                            lastPos = cp.getPos();
                        } else {
                            MyRenderer.drawLine(evt.getPoseStack(), vertexConsumerLine, lastPos, cp.getPos(), cp.isGreen ? 0 : 1, cp.isGreen ? 1 : 0, 0, a);
                            if (MiscUtil.isSnowLayerBlock(MC.level.getBlockState(cp.bp.offset(0,1,0)).getBlock())) {
                                AABB aabb = new AABB(cp.bp);
                                aabb = aabb.setMaxY(aabb.maxY + 0.13f);
                                MyRenderer.drawSolidBox(
                                        evt.getPoseStack(),
                                        vertexConsumerEntityTranslucent,
                                        aabb,
                                        Direction.UP,
                                        cp.isGreen ? 0 : 1,
                                        cp.isGreen ? 1 : 0,
                                        0,
                                        a,
                                        ResourceLocation.parse("forge:textures/white.png")
                                );
                            } else {
                                MyRenderer.drawBlockFace(evt.getPoseStack(), vertexConsumerEntityTranslucent, Direction.UP, cp.bp, cp.isGreen ? 0 : 1, cp.isGreen ? 1 : 0, 0, a);
                            }
                            lastPos = cp.getPos();
                        }
                    }

                    // draw anchor pos
                    if (SandboxClientEvents.isSandboxPlayer() && unit.getAnchor() != null && !unit.getAnchor().equals(new BlockPos(0,0,0))) {
                        BlockPos ap = unit.getAnchor();
                        float a = MiscUtil.getOscillatingFloat(0.25f, 0.75f);
                        Vec3 apVec3 = new Vec3(ap.getX() + 0.5f, ap.getY() + 1.0f, ap.getZ() + 0.5f);
                        MyRenderer.drawLine(evt.getPoseStack(), vertexConsumerLine, firstPos, apVec3, 1, 1, 0, a);

                        if (MiscUtil.isSnowLayerBlock(MC.level.getBlockState(ap.offset(0,1,0)).getBlock())) {
                            AABB aabb = new AABB(ap);
                            aabb = aabb.setMaxY(aabb.maxY + 0.13f);
                            MyRenderer.drawSolidBox(
                                    evt.getPoseStack(),
                                    vertexConsumerEntityTranslucent,
                                    aabb,
                                    Direction.UP,
                                    1, 1, 0, a,
                                    ResourceLocation.parse("forge:textures/white.png")
                            );
                        } else {
                            MyRenderer.drawBlockFace(evt.getPoseStack(), vertexConsumerEntityTranslucent, Direction.UP, ap, 1, 1, 0, a);
                        }
                    }

                    // draw mounted units' target lines
                    if (entity.isVehicle()) {
                        BlockPos blockTarget = null;
                        if (entity.getFirstPassenger() instanceof RangedAttackerUnit rau &&
                            rau.getRangedAttackGroundGoal() != null &&
                            rau.getRangedAttackGroundGoal().getGroundTarget() != null) {
                            blockTarget = rau.getRangedAttackGroundGoal().getGroundTarget();
                        }
                        float a = MiscUtil.getOscillatingFloat(0.25f, 0.75f);

                        if (blockTarget != null) {
                            Vec3 btVec3 = new Vec3(blockTarget.getX() + 0.5f, blockTarget.getY() + 1.0f, blockTarget.getZ() + 0.5f);
                            MyRenderer.drawLine(evt.getPoseStack(), vertexConsumerLine, firstPos, btVec3, 1, 0, 0, a);
                            if (MiscUtil.isSnowLayerBlock(MC.level.getBlockState(blockTarget.offset(0,1,0)).getBlock())) {
                                AABB aabb = new AABB(blockTarget);
                                aabb = aabb.setMaxY(aabb.maxY + 0.13f);
                                MyRenderer.drawSolidBox(
                                        evt.getPoseStack(),
                                        vertexConsumerEntityTranslucent,
                                        aabb,
                                        Direction.UP,
                                        1, 0, 0, MiscUtil.getOscillatingFloat(0.25f, 0.75f),
                                        ResourceLocation.parse("forge:textures/white.png")
                                );
                            }  else {
                                MyRenderer.drawBlockFace(evt.getPoseStack(), vertexConsumerEntityTranslucent, Direction.UP, blockTarget, 1, 0, 0, a);
                            }
                        }
                    }

                    // draw path nodes
                    /*
                    if (unit instanceof Mob mob && mob.getNavigation().getPath() != null) {
                        for (Node node : mob.getNavigation().getPath().nodes) {
                            BlockPos bp = new BlockPos(node.x, node.y, node.z).below();
                            MyRenderer.drawBlockFace(evt.getPoseStack(), Direction.UP, bp, 0, 1, 0, a);
                        }
                    }
                     */
                }
            }

            if (FormationDragMove.isDragging()) {
                VertexConsumer vertexConsumerLineFd = MC.renderBuffers().bufferSource().getBuffer(RenderType.LINES);
                VertexConsumer vertexConsumerEntityTranslucentFd = MC.renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("forge:textures/white.png")));
                float a = 0.5f;

                Vec3 lineStart = FormationDragMove.getLineStart();
                Vec3 lineEnd = FormationDragMove.getLineEnd();
                MyRenderer.drawLine(evt.getPoseStack(), vertexConsumerLineFd, lineStart, lineEnd, 0, 1, 0, a);

                for (BlockPos bp : FormationDragMove.getFormationTargets()) {
                    if (MiscUtil.isSnowLayerBlock(MC.level.getBlockState(bp.offset(0, 1, 0)).getBlock())) {
                        AABB aabb = new AABB(bp);
                        aabb = aabb.setMaxY(aabb.maxY + 0.13f);
                        MyRenderer.drawSolidBox(
                                evt.getPoseStack(),
                                vertexConsumerEntityTranslucentFd,
                                aabb,
                                Direction.UP,
                                0,
                                1,
                                0,
                                a,
                                ResourceLocation.parse("forge:textures/white.png")
                        );
                    } else {
                        MyRenderer.drawBlockFace(evt.getPoseStack(), vertexConsumerEntityTranslucentFd, Direction.UP, bp, 0, 1, 0, a);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onButtonPress(ScreenEvent.KeyPressed.Pre evt) {
        if (TextInputClientEvents.isAnyInputFocused())
            return;
        if (evt.getKeyCode() == GLFW.GLFW_KEY_DELETE || (evt.getKeyCode() == GLFW.GLFW_KEY_D && Keybindings.altMod.isDown() && Keybindings.ctrlMod.isDown())) {
            boolean isSandboxPlayer = MC.player != null && SandboxClientEvents.isSandboxPlayer(MC.player.getName().getString());
            LivingEntity entity = hudSelectedEntity;
            if ((entity != null && getPlayerToEntityRelationship(entity) == Relationship.OWNED || isSandboxPlayer) &&
                    !(entity instanceof CreeperUnit)) {
                if (entity != null && !entity.hasEffect(MobEffectRegistrar.PARTIALLY_POSSESSED.get())) {
                    sendUnitCommand(UnitAction.DELETE);
                }
            }
        }
    }

    public static boolean targetingSelf() {
        return selectedUnits.size() == 1 &&
                preselectedUnits.size() == 1 &&
                selectedUnits.get(0).equals(preselectedUnits.get(0));
    }

    public static Relationship getPlayerToPlayerRelationship(String ownerName) {
        if (MC.level != null && MC.player != null) {
            if (ownerName == null || ownerName.isBlank()) {
                return Relationship.NEUTRAL;
            }

            String playerName = MC.player.getName().getString();
            if (playerName.equals(ownerName)) {
                return Relationship.OWNED;
            } else if (AlliancesClient.isAllied(playerName, ownerName)) {
                return Relationship.FRIENDLY;
            } else {
                return Relationship.HOSTILE;
            }
        }

        // If the world or player is null, return NEUTRAL
        return Relationship.NEUTRAL;
    }
    public static Relationship getPlayerToEntityRelationship(LivingEntity entity) {
        if (MC.level != null && MC.player != null) {
            String playerName = MC.player.getName().getString();

            // Check if the entity is a Unit with no owner (neutral)
            if (entity instanceof Unit unit && unit.getOwnerName().isBlank()) {
                return Relationship.NEUTRAL;
            }

            // If the entity is a player, default to hostile unless further alliance checks are needed
            if (entity instanceof Player playerEntity) {
                String entityName = playerEntity.getName().getString();

                if (playerName.equals(entityName)) {
                    return Relationship.OWNED;
                } else if (AlliancesClient.isAllied(playerName, entityName)) {
                    return Relationship.FRIENDLY;
                } else {
                    return Relationship.HOSTILE;
                }
            }

            // Check if the entity is not a Unit (e.g., an NPC or neutral entity)
            if (!(entity instanceof Unit)) {
                return Relationship.NEUTRAL;
            }

            // For Units, check ownership and alliance
            String ownerName = ((Unit) entity).getOwnerName();

            if (playerName.equals(ownerName)) {
                return Relationship.OWNED;
            } else if (AlliancesClient.isAllied(playerName, ownerName)) {
                return Relationship.FRIENDLY;
            } else {
                return Relationship.HOSTILE;
            }
        }

        // If the world or player is null, return NEUTRAL
        return Relationship.NEUTRAL;
    }

    public static void syncConvertedUnits(String ownerName, int[] oldUnitIds, int[] newUnitIds) {

        for (int i = 0; i < oldUnitIds.length; i++) {
            if (MC.level == null)
                break;

            Entity oldEntity = MC.level.getEntity(oldUnitIds[i]);
            Entity newEntity = MC.level.getEntity(newUnitIds[i]);

            if (oldEntity instanceof Unit oldUnit &&
                newEntity instanceof Unit newUnit) {

                // retain selections
                int j = i;
                if (selectedUnits.removeIf(e -> e.getId() == oldUnitIds[j]))
                    selectedUnits.add((LivingEntity) newEntity);

                // retain control groups
                HudClientEvents.convertControlGroups(oldUnitIds, newUnitIds);

                if (oldUnit.getTargetGoal().getTarget() != null)
                    sendUnitCommandManual(
                        UnitAction.ATTACK,
                        oldUnit.getTargetGoal().getTarget().getId(),
                        new int[] { newEntity.getId() }
                    );
                if (oldUnit.getFollowTarget() != null)
                    sendUnitCommandManual(
                        UnitAction.FOLLOW,
                        oldUnit.getFollowTarget().getId(),
                        new int[] { newEntity.getId() }
                    );
                if (oldUnit.getMoveGoal().getMoveTarget() != null)
                    sendUnitCommandManual(
                        UnitAction.MOVE, -1,
                        new int[] { newEntity.getId() },
                        oldUnit.getMoveGoal().getMoveTarget()
                    );
                if (oldUnit.getReturnResourcesGoal() != null &&
                    oldUnit.getReturnResourcesGoal().getBuildingTarget() != null)
                    sendUnitCommandManual(
                        UnitAction.RETURN_RESOURCES, -1,
                        new int[] { newEntity.getId() },
                        oldUnit.getReturnResourcesGoal().getBuildingTarget().originPos,
                        new BlockPos(0,0,0)
                    );
            }
            if (oldEntity instanceof AttackerUnit oldAUnit &&
                newEntity instanceof AttackerUnit newAUnit) {

                if (oldAUnit.getAttackMoveTarget() != null)
                    sendUnitCommandManual(
                        UnitAction.ATTACK_MOVE, -1,
                        new int[] { newEntity.getId() },
                        oldAUnit.getAttackMoveTarget()
                    );
            }
        }
        // for some reason if we don't discard here first the vehicle also gets discarded
        if (MC.level != null) {
            for (int id : oldUnitIds) {
                Entity e = MC.level.getEntity(id);
                if (e instanceof ConvertableUnit cUnit)
                    cUnit.setShouldDiscard(true);
            }
        }
        sendUnitCommandManual(UnitAction.DISCARD, oldUnitIds);
        markSelectedUnitsChanged();
    }

    public static void syncUnitAnimation(UnitAnimationAction animAction, boolean startAnimation, int entityId, int targetId,
                                         BlockPos buildingBp) {
        for (LivingEntity entity : getAllUnits()) {
            if (entity instanceof EvokerUnit eUnit && eUnit.getId() == entityId) {
                if (eUnit.getCastFangsGoal() != null) {
                    if (startAnimation)
                        eUnit.getCastFangsGoal().startCasting();
                    else
                        eUnit.getCastFangsGoal().stop();
                }
            } else if (entity instanceof WardenUnit wUnit && wUnit.getId() == entityId) {
                if (wUnit.getSonicBoomGoal() != null) {
                    if (startAnimation)
                        wUnit.startSonicBoomAnimation();
                    else
                        wUnit.stopSonicBoomAnimation();
                }
            } else if (entity instanceof GhastUnit gUnit && gUnit.getId() == entityId && startAnimation) {
                gUnit.showShootingFace();
            } else if (entity instanceof BruteUnit bUnit && bUnit.getId() == entityId) {
                bUnit.setHoldingUpShield(startAnimation);
            } else if (entity instanceof WorkerUnit wUnit && entity instanceof AttackerUnit aUnit && entity.getId() == entityId) {
                if (startAnimation && MC.level != null) {
                    if (entity instanceof VillagerUnit vUnit && vUnit.getUnitProfession() == VillagerUnitProfession.HUNTER && vUnit.isVeteran())
                        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
                    else
                        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));

                    aUnit.setUnitAttackTarget((LivingEntity) MC.level.getEntity(targetId)); // set itself as a target just for animation purposes, doesn't tick clientside anyway
                } else {
                    entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.AIR));
                    aUnit.setUnitAttackTarget(null);
                }
            } else if ((entity instanceof VindicatorUnit || entity instanceof MilitiaUnit) && entity.getId() == entityId) {
                if (startAnimation && MC.level != null) {
                    if (targetId > 0) {
                        ((AttackerUnit) entity).setUnitAttackTarget((LivingEntity) MC.level.getEntity(targetId)); // set itself as a target just for animation purposes, doesn't tick clientside anyway
                    } else {
                        ((AttackerUnit) entity).setAttackBuildingTarget(buildingBp);
                    }
                } else {
                    ((AttackerUnit) entity).setUnitAttackTarget(null);
                    ((MeleeAttackBuildingGoal) ((AttackerUnit) entity).getAttackBuildingGoal()).stopAttacking();
                }
            }
        }
    }

    public static void playKeyframeAnimation(UnitAnimationAction animAction, int entityId) {
        for (LivingEntity entity : getAllUnits()) {
            if (entity instanceof KeyframeAnimated kfa && entity.getId() == entityId) {
                kfa.playSingleAnimation(animAction);
                return;
            }
        }
    }

    // usually used for attacking buildings
    public static void playAttackAnimation(int entityId) {
        for (LivingEntity entity : getAllUnits()) {
            if (entity.getId() == entityId) {
                if (entity instanceof IronGolemUnit ||
                    entity instanceof HoglinUnit ||
                    entity instanceof ZoglinUnit ||
                    entity instanceof RavagerUnit ||
                    entity instanceof WardenUnit) {
                    entity.handleEntityEvent((byte) 4);
                }
            }
        }
    }

    public static void syncIdleWorkers(int[] idleWorkerIds) {
        if (MC.level == null)
            return;

        UnitClientEvents.idleWorkerIds.clear();
        for (int id : idleWorkerIds) {
            for (LivingEntity entity : getAllUnits()) {
                if (entity.getId() == id &&
                    entity instanceof WorkerUnit unit &&
                    getPlayerToEntityRelationship(entity) == Relationship.OWNED)
                    UnitClientEvents.idleWorkerIds.add(id);
            }
        }
    }

    public static void addUnitPoofs(Level level, Entity entity) {
        MiscUtil.addParticleExplosion(ParticleTypes.POOF, 35, level, entity.position());
    }

    public static void makeVillagerVeteran(int unitId) {
        for (LivingEntity entity : getAllUnits())
            if (entity instanceof VillagerUnit vUnit && unitId == entity.getId())
                vUnit.isVeteran = true;
    }

    // used only for right click mounting shortcut
    @Nullable
    public static UnitAction getMountAction(LivingEntity passenger, LivingEntity vehicle) {
        if (!(passenger instanceof Unit) || !(vehicle instanceof Unit))
            return null;
        if (!((Unit) passenger).getOwnerName().equals(((Unit) vehicle).getOwnerName()))
            return null;
        if ((hudSelectedEntity instanceof PillagerUnit || hudSelectedEntity instanceof EvokerUnit) && vehicle instanceof RavagerUnit &&
            ResearchClient.hasResearch(ProductionItems.RESEARCH_RAVAGER_CAVALRY))
            return UnitAction.MOUNT_RAVAGER;
        if (hudSelectedEntity instanceof HeadhunterUnit && vehicle instanceof HoglinUnit &&
            ResearchClient.hasResearch(ProductionItems.RESEARCH_HOGLIN_CAVALRY))
            return UnitAction.MOUNT_HOGLIN;
        if (hudSelectedEntity instanceof Unit && hudSelectedEntity instanceof AbstractSkeleton && vehicle instanceof SpiderUnit &&
            ResearchClient.hasResearch(ProductionItems.RESEARCH_SPIDER_JOCKEYS))
            return UnitAction.MOUNT_SPIDER;
        return null;
    }

    public static List<LivingEntity> getMilitaryUnitsOnScreen() {
        ArrayList<Vec3> uvwpFull = MyMath.prepIsPointInsideRect3d(MC,
                0, 0, // top left
                0, MC.getWindow().getGuiScaledHeight(), // bottom left
                MC.getWindow().getGuiScaledWidth(), MC.getWindow().getGuiScaledHeight() // bottom right
        );

        ArrayList<LivingEntity> units = new ArrayList<>();
        for (LivingEntity entity : MiscUtil.getEntitiesWithinRange(CursorClientEvents.getCursorWorldPos(), 100, LivingEntity.class, MC.level)) {
            if (MyMath.isPointInsideRect3d(uvwpFull, entity.getBoundingBox().getCenter()) &&
                    entity.getId() != MC.player.getId() &&
                    !(entity instanceof WorkerUnit) &&
                    entity instanceof AttackerUnit &&
                    GarrisonableBuildingAddon.getGarrison((Unit) entity) == null &&
                    getPlayerToEntityRelationship(entity) == Relationship.OWNED
            )
                units.add(entity);
        }
        return units;
    }

    public static void syncUnitEatingFood(int unitId, int itemId) {
        for (LivingEntity entity : getAllUnits()) {
            if (unitId == entity.getId() && entity instanceof Unit unit) {
                unit.getItems().add(new ItemStack(BuiltInRegistries.ITEM.byId(itemId)));
                break;
            }
        }
    }

    public static void syncMobEffect(int entityId, int effectId, int amplifier, int duration) {
        for (LivingEntity entity : getAllUnits()) {
            MobEffect effect = MobEffect.byId(effectId);
            if (effect != null && entityId == entity.getId() && entity instanceof Unit) {
                if (duration > 0) {
                    entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
                } else if (entity.getEffect(effect) != null) {
                    entity.removeEffect(effect);
                }
            }
        }
    }

    /*
    @SubscribeEvent
    public static void onButtonPress2(ScreenEvent.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == GLFW.GLFW_KEY_P) {
            if (AFTER_SKY.equals(stage)) {
                stage = AFTER_SOLID_BLOCKS;
            } else if (AFTER_SOLID_BLOCKS.equals(stage)) {
                stage = AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS;
            } else if (AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS.equals(stage)) {
                stage = AFTER_CUTOUT_BLOCKS;
            } else if (AFTER_CUTOUT_BLOCKS.equals(stage)) {
                stage = AFTER_ENTITIES;
            } else if (AFTER_ENTITIES.equals(stage)) {
                stage = AFTER_BLOCK_ENTITIES;
            } else if (AFTER_BLOCK_ENTITIES.equals(stage)) {
                stage = AFTER_TRANSLUCENT_BLOCKS;
            } else if (AFTER_TRANSLUCENT_BLOCKS.equals(stage)) {
                stage = AFTER_TRIPWIRE_BLOCKS;
            } else if (AFTER_TRIPWIRE_BLOCKS.equals(stage)) {
                stage = AFTER_PARTICLES;
            } else if (AFTER_PARTICLES.equals(stage)) {
                stage = AFTER_WEATHER;
            } else if (AFTER_WEATHER.equals(stage)) {
                stage = AFTER_LEVEL;
            } else if (AFTER_LEVEL.equals(stage)) {
                stage = AFTER_SKY;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getGuiGraphics(), MC.font, new String[] {
            "stage: " + stage.toString(),
        });
    }
     */

    /*
    public static int yOffset = 0;
    public static int scale = 0;

    @SubscribeEvent
    public static void onButtonPress2(ScreenEvent.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT || evt.getKeyCode() == GLFW.GLFW_KEY_RIGHT) {
            int sign = evt.getKeyCode() == GLFW.GLFW_KEY_LEFT ? -1 : 1;
            yOffset += sign;
        }
        else if (evt.getKeyCode() == GLFW.GLFW_KEY_UP || evt.getKeyCode() == GLFW.GLFW_KEY_DOWN) {
            int sign = evt.getKeyCode() == GLFW.GLFW_KEY_UP ? -1 : 1;
            scale += sign;
        }
    }

    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getGuiGraphics(), MC.font, new String[] {
                "yOffset: " +  yOffset,
                "scale: " + scale,
        });
    }
     */

    /*
    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre evt) {
        if (Keybindings.altMod.isDown() && evt.getKeyCode() == GLFW.GLFW_KEY_SPACE && !getAllUnits().isEmpty()) {
            PacketHandler.INSTANCE.sendToServer(new UnitActionServerboundPacket(
                    "",
                    UnitAction.DEBUG1, 0, new int[]{0},
                    new BlockPos(0,0,0),
                    new BlockPos(0,0,0)
            ));
        }
        if (Keybindings.ctrlMod.isDown() && evt.getKeyCode() == GLFW.GLFW_KEY_SPACE && !getAllUnits().isEmpty()) {
            PacketHandler.INSTANCE.sendToServer(new UnitActionServerboundPacket(
                    "",
                    UnitAction.DEBUG2, 0, new int[]{0},
                    new BlockPos(0,0,0),
                    new BlockPos(0,0,0)
            ));
        }
    }
     */

    /*
    public static int pitch = 0;
    public static int yaw = 0;

    @SubscribeEvent
    public static void onButtonPress2(ScreenEvent.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT || evt.getKeyCode() == GLFW.GLFW_KEY_RIGHT) {
            int sign = evt.getKeyCode() == GLFW.GLFW_KEY_LEFT ? -1 : 1;
            yaw += sign;
        }
        else if (evt.getKeyCode() == GLFW.GLFW_KEY_UP || evt.getKeyCode() == GLFW.GLFW_KEY_DOWN) {
            int sign = evt.getKeyCode() == GLFW.GLFW_KEY_UP ? -1 : 1;
            pitch += sign;
        }
    }


    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getGuiGraphics(), MC.font, new String[] {
                "pitch: " +  pitch,
                "yaw: " + yaw,
        });
    }
     */
}
