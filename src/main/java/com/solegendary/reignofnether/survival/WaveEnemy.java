package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.*;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import com.solegendary.reignofnether.unit.units.monsters.WardenUnit;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import com.solegendary.reignofnether.unit.units.piglins.WitherSkeletonUnit;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WaveEnemy {

    private static final int PERIODIC_COMMAND_INTERVAL = 40;
    private static final int IDLE_COMMAND_INTERVAL = 100;

    public final Unit unit;
    private long idleTicks = 0;
    private long ticks = 0;

    private BlockPos lastOnPos;

    public WaveEnemy(Unit unit) {
        this.unit = unit;
        this.lastOnPos = getEntity().getOnPos();
    }

    public LivingEntity getEntity() {
        return ((LivingEntity) unit);
    }

    public void tick(long ticksToAdd) {
        if (getEntity().isPassenger())
            return;

        ticks += ticksToAdd;

        boolean isAttacking = unit.getTargetGoal().getTarget() != null;
        if (!isAttacking &&
            unit instanceof AttackerUnit aUnit &&
            aUnit.getAttackBuildingGoal() instanceof MeleeAttackBuildingGoal mabg &&
            mabg.isAttacking())
            isAttacking = true;

        BlockPos onPos = getEntity().getOnPos();
        if (onPos.equals(lastOnPos) && !isAttacking)
            idleTicks += ticksToAdd;
        else
            idleTicks = 0;

        lastOnPos = onPos;

        if (ticks > 0 && ticks == ticksToAdd * 10)
            startingCommand();

        if (ticks > 0 && ticks % PERIODIC_COMMAND_INTERVAL == 0)
            periodicCommand();

        if (idleTicks > 0 && idleTicks % IDLE_COMMAND_INTERVAL == 0)
            idleCommand();

        if (unit instanceof CreeperUnit creeperUnit) {
            BuildingPlacement nearestBuilding = getNearestAttackableBuilding();
            if (nearestBuilding != null) {
                BlockPos bpTarget = nearestBuilding.getClosestGroundPos(((Entity) unit).getOnPos(), 1);
                if (creeperUnit.distanceToSqr(Vec3.atCenterOf(bpTarget)) < 4)
                    creeperUnit.startToExplode();
            }
        }
        if (ticks > 0 && ticks % 20 == 0 && getEntity().isInWater() && idleTicks > 100) {
            if (getEntity().level().getBlockState(getEntity().getOnPos()).getFluidState().is(FluidTags.WATER)) {
                getEntity().level().setBlockAndUpdate(getEntity().getOnPos(), Blocks.FROSTED_ICE.defaultBlockState());
                getEntity().moveTo(getEntity().getX(), getEntity().getY() + 0.5f, getEntity().getZ());
            }
        }
    }

    // done shortly after spawn
    public void startingCommand() {
        if (unit instanceof SpiderUnit spiderUnit &&
            ResearchServerEvents.playerHasResearch(spiderUnit.getOwnerName(), ProductionItems.RESEARCH_SPIDER_WEBS) &&
            spiderUnit.getWebAbility() != null) {
            spiderUnit.getWebAbility().setAutocast(true, spiderUnit);
        }
        attackMoveNearestBuilding();
    }

    // done every X ticks
    public void periodicCommand() {
        if (unit instanceof RavagerUnit ravagerUnit) {
            for (Ability ability : ravagerUnit.getAbilities().get()) {
                if (ability instanceof Roar roar && roar.isOffCooldown(unit) &&
                        ravagerUnit.getHealth() < ravagerUnit.getMaxHealth() / 2) {
                    Unit.fullResetBehaviours(ravagerUnit);
                    roar.use(getEntity().level(), unit, (BlockPos) null);
                }
            }
        }
        if (unit instanceof WardenUnit wardenUnit) {
            LivingEntity target = getNearestAttackableUnit();
            for (Ability ability : wardenUnit.getAbilities().get()) {
                if (ability instanceof SonicBoom boom && boom.isOffCooldown(unit) && target != null &&
                        wardenUnit.distanceToSqr(target) <= (boom.range * boom.range)) {
                    Unit.fullResetBehaviours(wardenUnit);
                    boom.use(getEntity().level(), unit, target);
                }
            }
        }
        if (unit instanceof EvokerUnit evokerUnit) {
            LivingEntity target = getNearestAttackableUnit();
            for (Ability ability : evokerUnit.getAbilities().get()) {
                if (ability instanceof CastSummonVexes summon && summon.isOffCooldown(unit) && target != null &&
                        evokerUnit.distanceToSqr(target) <= (evokerUnit.getAttackRange() * evokerUnit.getAttackRange())) {
                    Unit.fullResetBehaviours(evokerUnit);
                    summon.use(getEntity().level(), unit, target);
                }
            }
        }
        if (unit instanceof BruteUnit bruteUnit) {
            for (Ability ability : bruteUnit.getAbilities().get()) {
                if (ability instanceof ToggleShield shield &&
                    ResearchServerEvents.playerHasResearch(bruteUnit.getOwnerName(), ProductionItems.RESEARCH_BRUTE_SHIELDS)) {

                    boolean shouldRaiseShield =
                            (bruteUnit.getTarget() instanceof RangedAttackerUnit rTarget && bruteUnit.distanceToSqr((Entity) rTarget) <= 36) ||
                            (bruteUnit.getAttackBuildingGoal() instanceof MeleeAttackBuildingGoal mabg && mabg.isAttacking());

                    if ((!bruteUnit.isHoldingUpShield && shouldRaiseShield) ||
                        ((bruteUnit.isHoldingUpShield && !shouldRaiseShield))) {
                        shield.use(getEntity().level(), unit, (BlockPos) null);
                    }
                }
            }
        }
        if (unit instanceof WitherSkeletonUnit wsUnit) {
            LivingEntity target = wsUnit.getTarget();
            for (Ability ability : wsUnit.getAbilities().get()) {
                LivingEntity nearestAlly = getNearestNonWitherAllyUnit();
                if (ability instanceof WitherCloud cloud && cloud.isOffCooldown(unit) && target != null &&
                    wsUnit.distanceToSqr(target) <= 16 && (nearestAlly == null || wsUnit.distanceToSqr(nearestAlly) > 16)) {
                    cloud.use(getEntity().level(), unit, (BlockPos) null);
                }
            }
        }
    }

    // done if the unit didn't change position in X ticks
    public void idleCommand() {
        if (unit instanceof CreeperUnit ||
            (unit instanceof AttackerUnit aUnit && aUnit.canAttackBuildings()))
            attackMoveNearestBuilding();
        else
            attackMoveNearestUnit();
    }

    // done when attacked
    public void retaliateCommand() { }

    private BuildingPlacement getNearestAttackableBuilding() {
        List<BuildingPlacement> buildings = new ArrayList<>();
        for (BuildingPlacement buildingPlacement : BuildingServerEvents.getBuildings()) {
            if (!SurvivalServerEvents.ENEMY_OWNER_NAME.equals(buildingPlacement.ownerName) && !buildingPlacement.ownerName.isBlank() && !buildingPlacement.getBuilding().invulnerable) {
                buildings.add(buildingPlacement);
            }
        }
        buildings.sort(Comparator.comparing(b -> b.centrePos.distToCenterSqr(((Entity) unit).position())));

        if (!buildings.isEmpty())
            return buildings.get(0);

        return null;
    }

    private LivingEntity getNearestAttackableUnit() {
        List<LivingEntity> entities = new ArrayList<>();
        for (LivingEntity livingEntity : UnitServerEvents.getAllUnits()) {
            if (livingEntity instanceof Unit u && !SurvivalServerEvents.ENEMY_OWNER_NAME.equals(u.getOwnerName()) && !u.getOwnerName().isBlank()) {
                entities.add(livingEntity);
            }
        }
        entities.sort(Comparator.comparing(le -> le.position().distanceToSqr(((Entity) unit).position())));

        BlockPos targetBp = null;
        if (!entities.isEmpty())
            return entities.get(0);

        return null;
    }

    private LivingEntity getNearestAttackableWorkerUnit() {
        List<LivingEntity> entities = new ArrayList<>();
        for (LivingEntity livingEntity : UnitServerEvents.getAllUnits()) {
            if (livingEntity instanceof Unit u && livingEntity instanceof WorkerUnit &&
                !SurvivalServerEvents.ENEMY_OWNER_NAME.equals(u.getOwnerName()) && !u.getOwnerName().isBlank()) {
                entities.add(livingEntity);
            }
        }
        entities.sort(Comparator.comparing(le -> le.position().distanceToSqr(((Entity) unit).position())));

        if (!entities.isEmpty())
            return entities.get(0);

        return null;
    }

    private LivingEntity getNearestNonWitherAllyUnit() {
        List<LivingEntity> entities = new ArrayList<>();
        for (LivingEntity livingEntity : UnitServerEvents.getAllUnits()) {
            if (livingEntity instanceof Unit u && !(u instanceof WitherSkeletonUnit) &&
                SurvivalServerEvents.ENEMY_OWNER_NAME.equals(u.getOwnerName()) && !u.getOwnerName().isBlank()) {
                entities.add(livingEntity);
            }
        }
        entities.sort(Comparator.comparing(le -> le.position().distanceToSqr(((Entity) unit).position())));

        if (!entities.isEmpty())
            return entities.get(0);

        return null;
    }

    private void attackMoveNearestBuilding() {
        unit.resetBehaviours();

        Entity entity = (Entity) unit;
        BuildingPlacement nearestBuilding = getNearestAttackableBuilding();

        BlockPos targetBp = null;
        if (nearestBuilding != null)
            targetBp = nearestBuilding.getClosestGroundPos(((Entity) unit).getOnPos(), 1);

        if (targetBp != null) {
            if (unit instanceof AttackerUnit)
                UnitServerEvents.addActionItem(unit.getOwnerName(), UnitAction.ATTACK_MOVE, -1,
                        new int[]{entity.getId()},  targetBp, new BlockPos(0,0,0));
            else
                UnitServerEvents.addActionItem(unit.getOwnerName(), UnitAction.MOVE, -1,
                        new int[]{entity.getId()},  targetBp, new BlockPos(0,0,0));
        }
    }

    private void attackMoveRandomBuilding() {
        unit.resetBehaviours();

        ArrayList<BuildingPlacement> buildings = BuildingServerEvents.getBuildings();
        Collections.shuffle(buildings);

        List<BuildingPlacement> playerBuildings = new ArrayList<>();
        for (BuildingPlacement b : buildings) {
            if (!SurvivalServerEvents.ENEMY_OWNER_NAME.equals(b.ownerName) && !b.ownerName.isBlank()) {
                playerBuildings.add(b);
            }
        }

        BlockPos targetBp = null;
        if (!playerBuildings.isEmpty())
            targetBp = buildings.get(0).getClosestGroundPos(((Entity) unit).getOnPos(), 1);


        if (targetBp != null) {
            if (unit instanceof AttackerUnit)
                UnitServerEvents.addActionItem(unit.getOwnerName(), UnitAction.ATTACK_MOVE, -1,
                        new int[]{((Entity) unit).getId()},  targetBp, new BlockPos(0,0,0));
            else
                UnitServerEvents.addActionItem(unit.getOwnerName(), UnitAction.MOVE, -1,
                        new int[]{((Entity) unit).getId()},  targetBp, new BlockPos(0,0,0));
        }
    }

    private void attackMoveNearestUnit() {
        unit.resetBehaviours();
        Entity entity = (Entity) unit;

        LivingEntity nearestUnit = getNearestAttackableUnit();

        if (unit instanceof SpiderUnit) {
            LivingEntity nearestWorker = getNearestAttackableWorkerUnit();
            if (nearestWorker != null)
                nearestUnit = nearestWorker;
        }
        BlockPos targetBp = null;
        if (nearestUnit != null)
            targetBp = nearestUnit.getOnPos();

        if (targetBp != null) {
            if (unit instanceof AttackerUnit)
                UnitServerEvents.addActionItem(unit.getOwnerName(), UnitAction.ATTACK_MOVE, -1,
                        new int[]{((Entity) unit).getId()},  targetBp, new BlockPos(0,0,0));
            else
                UnitServerEvents.addActionItem(unit.getOwnerName(), UnitAction.MOVE, -1,
                        new int[]{((Entity) unit).getId()},  targetBp, new BlockPos(0,0,0));
        }
    }
}
