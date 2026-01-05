package com.solegendary.reignofnether.unit;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import com.solegendary.reignofnether.building.buildings.placements.FarmPlacement;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.*;
import com.solegendary.reignofnether.util.LanguageUtil;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnitActionItem {
    private final String ownerName;
    private final UnitAction action;
    private final int unitId; // preselected unit (usually the target)
    private final int[] unitIds; // selected unit(s)
    private final BlockPos preselectedBlockPos;
    private final BlockPos selectedBuildingPos;

    public final UnitAction getAction() { return action; }
    public final int[] getUnitIds() { return unitIds; }
    public final BlockPos getPreselectedBlockPos() { return preselectedBlockPos; }

    public boolean equals(UnitActionItem unitActionItem) {
        if (unitActionItem == null)
            return false;
        return (
            ownerName == null && unitActionItem.ownerName == null || (ownerName != null && ownerName.equals(unitActionItem.ownerName)) &&
            action == null && unitActionItem.action == null || (action != null && action.equals(unitActionItem.action)) &&
            preselectedBlockPos == null && unitActionItem.preselectedBlockPos == null || (preselectedBlockPos != null && preselectedBlockPos.equals(unitActionItem.preselectedBlockPos)) &&
            selectedBuildingPos == null && unitActionItem.selectedBuildingPos == null || (selectedBuildingPos != null && selectedBuildingPos.equals(unitActionItem.selectedBuildingPos)) &&
            unitId == unitActionItem.unitId &&
            Arrays.equals(unitIds, unitActionItem.unitIds)
        );
    }

    private final List<UnitAction> nonAbilityActions = List.of(UnitAction.STOP,
        UnitAction.HOLD,
        UnitAction.GARRISON,
        UnitAction.UNGARRISON,
        UnitAction.MOVE,
        UnitAction.ATTACK_MOVE,
        UnitAction.ATTACK,
        UnitAction.ATTACK_BUILDING,
        UnitAction.FOLLOW,
        UnitAction.BUILD_REPAIR,
        UnitAction.FARM,
        UnitAction.RETURN_RESOURCES,
        UnitAction.RETURN_RESOURCES_TO_CLOSEST,
        UnitAction.DELETE,
        UnitAction.DISCARD
    );

    public UnitActionItem(
        String ownerName,
        UnitAction action,
        int unitId,
        int[] unitIds,
        BlockPos preselectedBlockPos,
        BlockPos selectedBuildingPos
    ) {
        this.ownerName = ownerName;
        this.action = action;
        this.unitId = unitId;
        this.unitIds = unitIds;
        this.preselectedBlockPos = preselectedBlockPos;
        this.selectedBuildingPos = selectedBuildingPos;
    }

    private boolean canAffordManaCost(Ability ability, Unit unit) {
        if (ability instanceof HeroAbility heroAbility && unit instanceof HeroUnit hero) {
            return heroAbility.manaCost <= hero.getMana();
        }
        return true;
    }

    private boolean isRedundantMove(Unit unit, BlockPos targetPos) {
        LivingEntity le = (LivingEntity) unit;
        MoveToTargetBlockGoal goal = unit.getMoveGoal();

        if (goal != null && !le.level().isClientSide()) {
            BlockPos bp = goal.getMoveTarget();
            if (bp != null) {
                double distToTarget = bp.distSqr(le.getOnPos());
                if (distToTarget > 400) {
                    double ignoreDist = Math.min(5, Math.sqrt(distToTarget) / 10);
                    return bp.distSqr(targetPos) < ignoreDist * ignoreDist;
                } else {
                    return bp.equals(targetPos);
                }
            }
        }
        return false;
    }

    // can be done server or clientside - but only serverside will have an effect on the world
    // clientside actions are purely for tracking data
    public void action(Level level) {
        Ability usedAbility = null;

        boolean isSandboxPlayer;
        if (level.isClientSide())
            isSandboxPlayer = SandboxClientEvents.isSandboxPlayer(this.ownerName);
        else
            isSandboxPlayer = SandboxServer.isSandboxPlayer(this.ownerName);

        // filter out unowned units and non-unit entities
        ArrayList<Unit> actionableUnits = new ArrayList<>();
        for (int id : unitIds) {
            Entity entity = level.getEntity(id);

            if (entity instanceof Unit unit) {
                boolean alliedControl;
                if (level.isClientSide())
                    alliedControl = AlliancesClient.canControlAlly(unit.getOwnerName());
                else
                    alliedControl = AlliancesServerEvents.canControlAlly(this.ownerName, unit.getOwnerName());

                if (unit.getOwnerName().equals(this.ownerName) || isSandboxPlayer || alliedControl) {
                    actionableUnits.add(unit);
                }
            }
        }

        ArrayList<Unit> formationUnits = new ArrayList<>();

        actionableUnitsLoop:
        for (Unit unit : actionableUnits) {

            if (((LivingEntity) unit).getEffect(MobEffectRegistrar.STUN.get()) != null) {
                Unit.fullResetBehaviours(unit);
                continue;
            } else if (((LivingEntity) unit).getEffect(MobEffectRegistrar.UNCONTROLLABLE.get()) != null) {
                continue;
            }
            // recalculating pathfinding can be expensive, so check if we actually need to first
            if (action == UnitAction.MOVE && isRedundantMove(unit, preselectedBlockPos)) {
                continue;
            }

            // have to do this before resetBehaviours so we can assign the correct resourceName first
            if (action == UnitAction.TOGGLE_GATHER_TARGET) {
                if (unit instanceof WorkerUnit workerUnit) {
                    GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                    ResourceName targetResourceName = goal.getTargetResourceName();
                    Unit.fullResetBehaviours(unit);
                    switch (targetResourceName) {
                        case NONE -> goal.setTargetResourceName(ResourceName.FOOD);
                        case FOOD -> goal.setTargetResourceName(ResourceName.WOOD);
                        case WOOD -> goal.setTargetResourceName(ResourceName.ORE);
                        case ORE -> goal.setTargetResourceName(ResourceName.NONE);
                    }
                }
            } else {
                // if we are issuing a redundant unit attack command then don't resetBehaviours or else the unit will
                // pause unnecessarily
                // also don't reset if the action is an ability and the ability wasn't found on this unit
                if ((
                    action != UnitAction.ATTACK || unit.getTargetGoal().getTarget() == null
                        || unit.getTargetGoal().getTarget().getId() != unitId
                )) {

                    boolean foundAbility = false;
                    boolean shouldResetBehaviours = true;
                    for (Ability ability : unit.getAbilities().get()) {
                        if (ability.action == action) {
                            foundAbility = true;
                            shouldResetBehaviours = ability.shouldResetBehaviours();
                            break;
                        }
                    }
                    //if (unit instanceof MagmaCubeUnit && unit.getMoveGoal().getMoveTarget() != null) {
                    //    shouldResetBehaviours = false;
                    //}
                    if (shouldResetBehaviours && (nonAbilityActions.contains(action) || foundAbility) && action != UnitAction.RETURN_RESOURCES_TO_CLOSEST) {
                        Unit.fullResetBehaviours(unit);
                    }
                }
            }
            switch (action) {
                case STOP -> {
                    Entity passenger = ((Entity) unit).getFirstPassenger();
                    if (passenger instanceof Unit unitPassenger) {
                        Unit.fullResetBehaviours(unitPassenger);
                    }
                }
                case HOLD -> {
                    unit.setHoldPosition(true);
                }
                case GARRISON -> {
                    if (unit.canGarrison()) {
                        unit.getGarrisonGoal().setBuildingTarget(preselectedBlockPos);
                    }
                }
                case UNGARRISON -> {
                    GarrisonableBuilding garr = GarrisonableBuilding.getGarrison(unit);
                    if (garr != null && garr.getExitPosition() != null ) {
                        BuildingPlacement building = (BuildingPlacement) garr;
                        BlockPos bp = garr.getExitPosition();
                        ((LivingEntity) unit).teleportTo(bp.getX() + 0.5f, bp.getY() + 0.5f, bp.getZ() + 0.5f);
                    }
                }
                case MOVE -> {
                    ResourceName resName = ResourceSources.getBlockResourceName(preselectedBlockPos, level);
                    BuildingPlacement buildingAtPos = BuildingUtils.findBuilding(((Entity) unit).level().isClientSide(),
                        preselectedBlockPos
                    );

                    if (unit instanceof WorkerUnit workerUnit && resName != ResourceName.NONE
                        && buildingAtPos == null) {
                        GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                        goal.setTargetResourceName(resName);
                        goal.setMoveTarget(preselectedBlockPos);
                        if (Unit.atMaxResources((Unit) workerUnit)) {
                            if (level.isClientSide()) {
                                HudClientEvents.showTemporaryMessage(LanguageUtil.getTranslation("hud.reignofnether.worker_inv_full"));
                            }
                            goal.saveAndReturnResources();
                        }
                    } else if (buildingAtPos instanceof PortalPlacement portal
                        && portal.getPortalType() == PortalPlacement.PortalType.TRANSPORT && unit.canUsePortal()) {
                        if (unit.getUsePortalGoal() instanceof FlyingUsePortalGoal flyingUsePortalGoal)
                            flyingUsePortalGoal.setBuildingTarget(preselectedBlockPos);
                        if (unit.getUsePortalGoal() instanceof UsePortalGoal usePortalGoal)
                            usePortalGoal.setBuildingTarget(preselectedBlockPos);
                    } else if (actionableUnits.size() == 1) {
                        unit.setMoveTarget(preselectedBlockPos);
                    } else {
                        formationUnits.add(unit);
                    }
                }
                case ATTACK_MOVE -> {
                    // if the unit can't actually attack just treat this as a move action
                    if (unit instanceof AttackerUnit attackerUnit) {
                        MiscUtil.addUnitCheckpoint(unit, preselectedBlockPos, false);
                        attackerUnit.setAttackMoveTarget(preselectedBlockPos);
                    } else {
                        unit.setMoveTarget(preselectedBlockPos);
                    }
                }
                case ATTACK -> {
                    // if the unit can't actually attack just treat this as a follow action
                    if (unit instanceof AttackerUnit attackerUnit) {
                        attackerUnit.setUnitAttackTargetForced((LivingEntity) level.getEntity(unitId));
                    } else {
                        LivingEntity livingEntity = (LivingEntity) level.getEntity(unitId);
                        if (livingEntity != null) {
                            MiscUtil.addUnitCheckpoint(unit, unitId, true);
                        }
                        unit.setFollowTarget(livingEntity);
                    }
                }
                case ATTACK_BUILDING -> {
                    // if the unit can't actually attack just treat this as a move action
                    if (unit instanceof AttackerUnit attackerUnit) {
                        attackerUnit.setAttackBuildingTarget(preselectedBlockPos);
                    } else {
                        unit.setMoveTarget(preselectedBlockPos);
                    }
                }
                case FOLLOW -> {
                    LivingEntity livingEntity = (LivingEntity) level.getEntity(unitId);
                    if (livingEntity != null) {
                        MiscUtil.addUnitCheckpoint(unit, unitId, true);
                    }
                    unit.setFollowTarget(livingEntity);
                }
                case BUILD_REPAIR -> {
                    // if the unit can't actually build/repair just treat this as a move action
                    if (unit instanceof WorkerUnit workerUnit) {
                        BuildingPlacement building = BuildingUtils.findBuilding(level.isClientSide(), preselectedBlockPos);
                        if (building != null) {
                            workerUnit.getBuildRepairGoal().setBuildingTarget(building);
                        }
                    } else {
                        unit.setMoveTarget(preselectedBlockPos);
                    }
                }
                case ENABLE_AUTOCAST_BUILD_REPAIR, DISABLE_AUTOCAST_BUILD_REPAIR -> {
                    // if the unit can't actually build/repair just treat this as a move action
                    if (unit instanceof WorkerUnit workerUnit) {
                        workerUnit.getBuildRepairGoal().autocastRepair =
                                action == UnitAction.ENABLE_AUTOCAST_BUILD_REPAIR;
                    }
                }
                case FARM -> {
                    if (unit instanceof WorkerUnit workerUnit) {
                        GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                        if (goal != null) {
                            goal.setTargetResourceName(ResourceName.FOOD);
                            goal.setMoveTarget(preselectedBlockPos);
                            BuildingPlacement building = BuildingUtils.findBuilding(level.isClientSide(), preselectedBlockPos);
                            if (building instanceof FarmPlacement) {
                                goal.setTargetFarm(building);
                                if (Unit.atMaxResources((Unit) workerUnit)) {
                                    if (level.isClientSide()) {
                                        HudClientEvents.showTemporaryMessage(LanguageUtil.getTranslation(
                                            "hud.reignofnether.worker_inv_full"));
                                    }
                                    goal.saveAndReturnResources();
                                }
                            }
                        }
                    }
                }
                case RETURN_RESOURCES -> {
                    if (unit instanceof WorkerUnit workerUnit) { // if we manually did this, ignore automated return
                        // to gather
                        GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                        if (goal != null) {
                            goal.saveData.delete();
                        }
                    }
                    ReturnResourcesGoal returnResourcesGoal = unit.getReturnResourcesGoal();
                    BuildingPlacement building = BuildingUtils.findBuilding(false, preselectedBlockPos);
                    if (returnResourcesGoal != null && building != null) {
                        returnResourcesGoal.setBuildingTarget(building);
                    }
                }
                case RETURN_RESOURCES_TO_CLOSEST -> { // drop resources off early and return to work
                    if (unit instanceof WorkerUnit workerUnit) {
                        GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                        if (goal != null) {
                            goal.saveAndReturnResources();
                        }
                    }
                }
                case DELETE -> {
                    ((LivingEntity) unit).kill();
                }
                case DISCARD -> {
                    if (unit instanceof ConvertableUnit cUnit) {
                        cUnit.setShouldDiscard(true);
                    }
                }
                // any other Ability not explicitly defined here
                default -> {
                    for (Ability ability : unit.getAbilities().get()) {
                        if (ability.action == action &&
                            (ability.isOffCooldown(unit) || ability.canBypassCooldown(unit)) &&
                            canAffordManaCost(ability, unit)
                        ) {
                            if (ability.canTargetEntities && this.unitId > 0) {
                                ability.use(level, unit, (LivingEntity) level.getEntity(unitId));
                                usedAbility = ability;
                                if (ability.oneClickOneUse) {
                                    break actionableUnitsLoop;
                                }
                            } else {
                                ability.use(level, unit, preselectedBlockPos);
                                usedAbility = ability;
                                if (ability.oneClickOneUse) {
                                    break actionableUnitsLoop;
                                }
                            }
                        } else if (ability.autocastEnableAction == action) {
                            ability.setAutocast(true, unit);
                        } else if (ability.autocastDisableAction == action) {
                            ability.setAutocast(false, unit);
                        }
                    }
                }
            }
        }

        // if we have multiple units performing the same MOVE action, calculate a spread of blockPoses for them to go to
        if (!formationUnits.isEmpty()) {
            List<Pair<LivingEntity, BlockPos>> formationPairs = UnitFormations.getMoveFormation(
                level, new ArrayList<>(formationUnits.stream().map(u -> ((LivingEntity) u)).toList()), preselectedBlockPos
            );
            for (Pair<LivingEntity, BlockPos> pair : formationPairs) {
                LivingEntity le = pair.getFirst();
                BlockPos targetPos = pair.getSecond();

                if (isRedundantMove((Unit) le, targetPos))
                    continue;

                if (le instanceof Unit unit)
                    unit.setMoveTarget(targetPos);
            }
        }

        if (level.isClientSide() && usedAbility != null && usedAbility.oneClickOneUse) {
            HudClientEvents.setLowestCdHudEntity();
        }

        BuildingPlacement actionableBuilding = null;
        if (!this.selectedBuildingPos.equals(new BlockPos(0, 0, 0))) {
            actionableBuilding = BuildingUtils.findBuilding(level.isClientSide(), this.selectedBuildingPos);
        }

        if (actionableBuilding != null && (
            actionableBuilding.ownerName.equals(ownerName) ||
            SandboxServer.isSandboxPlayer(ownerName) ||
            AlliancesServerEvents.canControlAlly(ownerName, actionableBuilding.ownerName))
        ) {
            for (Ability ability : actionableBuilding.getAbilities()) {
                if (ability.action == action && (ability.isOffCooldown(actionableBuilding) || ability.canBypassCooldown(actionableBuilding))) {
                    if (ability.canTargetEntities && this.unitId > 0) {
                        ability.use(level, actionableBuilding, (LivingEntity) level.getEntity(unitId));
                    } else {
                        ability.use(level, actionableBuilding, preselectedBlockPos);
                    }
                } else if (ability.autocastEnableAction == action) {
                    ability.setAutocast(true, actionableBuilding);
                } else if (ability.autocastDisableAction == action) {
                    ability.setAutocast(false, actionableBuilding);
                }
            }
        }

        ArrayList<PathfinderMob> actionableNonUnits = new ArrayList<>();
        for (int id : unitIds) {
            Entity entity = level.getEntity(id);
            if (entity instanceof PathfinderMob mob) {
                actionableNonUnits.add(mob);
            }
        }

        if ((level.isClientSide() && NonUnitClientEvents.canControlAllMobs()) ||
            (!level.isClientSide() && NonUnitServerEvents.canControlAllMobs(level, ownerName))) {

            for (PathfinderMob mob : actionableNonUnits) {
                if (mob instanceof Unit)
                    continue;

                mob.getNavigation().stop();
                mob.setTarget(null);

                if (!level.isClientSide()) {
                    synchronized (NonUnitServerEvents.nonUnitMoveTargets) {
                        NonUnitServerEvents.nonUnitMoveTargets.removeIf(p -> p.getFirst() == mob);
                    }
                }

                if (List.of(UnitAction.MOVE, UnitAction.FOLLOW, UnitAction.ATTACK_MOVE).contains(action)) {
                    BlockPos bp = preselectedBlockPos;
                    Path path = mob.getNavigation().createPath(bp.getX(), bp.getY(), bp.getZ(), 0);
                    mob.getNavigation().moveTo(path, 1);
                    if (!level.isClientSide()) {
                        synchronized (NonUnitServerEvents.nonUnitMoveTargets) {
                            NonUnitServerEvents.nonUnitMoveTargets.add(new Pair<>(mob, preselectedBlockPos));
                        }
                    }
                } else if (action == UnitAction.ATTACK) {
                    if (level.getEntity(unitId) instanceof LivingEntity le) {
                        mob.setTarget(le);
                    }
                }
                if (!level.isClientSide()) {
                    if (action != UnitAction.ATTACK_MOVE) {
                        NonUnitServerEvents.attackSuppressedNonUnits.add(mob);
                    }
                    NonUnitServerEvents.moveSuppressedNonUnits.add(mob);
                } else {
                    NonUnitClientEvents.isMoveCheckpointGreen = action != UnitAction.ATTACK_MOVE;
                }
            }
        }
    }
}