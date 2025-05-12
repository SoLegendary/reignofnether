package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.BridgePlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.nether.NetherBlocks;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.time.NightUtils;
import com.solegendary.reignofnether.tps.TPSClientEvents;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import com.solegendary.reignofnether.util.Faction;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Defines method bodies for Units
// workaround for trying to have units inherit from both their base vanilla Mob class and a Unit class
// Note that we can't write any default methods if they need to use Unit fields without a getter/setter
// (including getters/setters themselves)

public interface Unit {

    int ANCHOR_RETREAT_RANGE = 30;

    int PIGLIN_HEALING_TICKS = 8 * ResourceCost.TICKS_PER_SECOND;
    int MONSTER_HEALING_TICKS = 12 * ResourceCost.TICKS_PER_SECOND;

    // used for increasing pathfinding calculation range, default is 16 for most mobs
    int FOLLOW_RANGE_IMPROVED = 64;
    int FOLLOW_RANGE = 16;

    static Object2ObjectArrayMap<Ability, Float> createCooldownMap() {
        Object2ObjectArrayMap<Ability, Float> map = new Object2ObjectArrayMap<>();
        map.defaultReturnValue(0F);
        return map;
    }

    // position that neutral units run back to when past leash range
    void setAnchor(BlockPos bp);
    BlockPos getAnchor();

    static int getFollowRange() {
        return UnitServerEvents.improvedPathfinding ? FOLLOW_RANGE_IMPROVED : FOLLOW_RANGE;
    }

    // list of positions to draw lines between to indicate unit intents - will fade over time unless shift is held
    ArrayList<Checkpoint> getCheckpoints();

    GarrisonGoal getGarrisonGoal();
    boolean canGarrison();

    MoveToTargetBlockGoal getUsePortalGoal();
    boolean canUsePortal();

    Faction getFaction();
    List<AbilityButton> getAbilityButtons();
    List<Ability> getAbilities();
    List<ItemStack> getItems();
    int getMaxResources();

    List<Keybinding> ABILITY_KEYBINDS = List.of(
            Keybindings.keyQ,
            Keybindings.keyW,
            Keybindings.keyE,
            Keybindings.keyR,
            Keybindings.keyT,
            Keybindings.keyY
    );

    // note that attackGoal is specific to unit types
    MoveToTargetBlockGoal getMoveGoal();
    SelectedTargetGoal<?> getTargetGoal();
    ReturnResourcesGoal getReturnResourcesGoal();

    float getMovementSpeed();
    float getUnitMaxHealth();
    float getUnitArmorValue();
    ResourceCost getCost();

    LivingEntity getFollowTarget();
    boolean getHoldPosition();
    void setHoldPosition(boolean holdPosition);

    String getOwnerName();
    void setOwnerName(String name);

    static void tick(Unit unit) {
        Mob unitMob = (Mob) unit;
        if (!unitMob.level().isClientSide() && unitMob.level() instanceof ServerLevel serverLevel) {
            ServerChunkCache chunkProvider = serverLevel.getChunkSource();

            BlockPos unitPos = unitMob.blockPosition();
            ChunkPos currentChunkPos = new ChunkPos(unitPos);

            // Load a 2-chunk radius around the unit
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    ChunkPos chunkPos = new ChunkPos(currentChunkPos.x + dx, currentChunkPos.z + dz);
                    chunkProvider.addRegionTicket(TicketType.FORCED, chunkPos, 2, chunkPos);
                }
            }
        }
        for (Map.Entry<Ability, Float> cooldownEntry : unit.getCooldowns().entrySet()) {
            Ability ability = cooldownEntry.getKey();
            float cooldown = cooldownEntry.getValue();
            if (cooldown > 0 || unit.getCharges(ability) < ability.maxCharges) {
                if (unit.level().isClientSide())
                    unit.getCooldowns().put(ability, (float) (cooldown - (TPSClientEvents.getCappedTPS() / 20D)));
                else
                    unit.getCooldowns().put(ability, cooldown - 1);

                if (cooldown <= 0 && ability.usesCharges() && unit.getCharges(ability) < ability.maxCharges) {
                    unit.setCharges(ability, unit.getCharges(ability) + 1);
                    if (unit.getCharges(ability) < ability.maxCharges)
                        unit.getCooldowns().put(ability, ability.cooldownMax);
                    if (unit.getCharges(ability) > ability.maxCharges)
                        unit.setCharges(ability, ability.maxCharges);
                }
            }
        }

        // ------------- CHECKPOINT LOGIC ------------- //
        if (unitMob.level().isClientSide()) {

            unit.getCheckpoints().removeIf(c -> c.isForEntity() && !c.entity.isAlive() || c.ticksLeft <= 0);

            for (Checkpoint cp : unit.getCheckpoints()) {
                cp.tick();
                boolean buildingIsDone = false;
                if (unit instanceof WorkerUnit workerUnit && !cp.isForEntity()) {
                    if (cp.placement != null && cp.placement.isBuilt && cp.placement.getHealth() >= cp.placement.getMaxHealth())
                        buildingIsDone = true;
                }
                if (((Mob) unit).getOnPos().distToCenterSqr(cp.getPos()) < 4f || buildingIsDone)
                    cp.startFading();
            }
        } else {
            int totalRes = Resources.getTotalResourcesFromItems(unit.getItems()).getTotalValue();
            if (unitMob.canPickUpLoot()) {
                for (ItemEntity itementity : unitMob.level().getEntitiesOfClass(ItemEntity.class, unitMob.getBoundingBox().inflate(1, 0, 1))) {
                    if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && unitMob.isAlive()) {

                        if (!Unit.atMaxResources(unit)) {
                            ItemStack itemstack = itementity.getItem();
                            ResourceSource resBlock = ResourceSources.getFromItem(itemstack.getItem());
                            if (resBlock != null) {
                                while (!Unit.atMaxResources(unit) && itemstack.getCount() > 0) {
                                    unitMob.onItemPickup(itementity);
                                    unitMob.take(itementity, 1);
                                    unit.getItems().add(new ItemStack(itemstack.getItem(), 1));
                                    itemstack.setCount(itemstack.getCount() - 1);
                                }
                                if (itemstack.getCount() <= 0)
                                    itementity.discard();

                                UnitSyncClientboundPacket.sendSyncResourcesPacket(unit);
                            }
                            if (Unit.atThresholdResources(unit) && unit instanceof WorkerUnit workerUnit) {
                                GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                                if (goal != null && goal.getTargetResourceName() != ResourceName.NONE)
                                    goal.saveAndReturnResources();
                            }
                        }
                    }
                }
            }

            // sync target variables between goals and Mob
            if (unit.getTargetGoal().getTarget() == null || !unit.getTargetGoal().getTarget().isAlive() ||
                    unitMob.getTarget() == null || !unitMob.getTarget().isAlive()) {
                unitMob.setTarget(null);
                unit.getTargetGoal().setTarget(null);
            }

            // no iframes after being damaged so multiple units can attack at once
            unitMob.invulnerableTime = 0;

            // enact target-following, and stop followTarget being reset
            if (unit.getFollowTarget() != null && unitMob.tickCount % 20 == 0)
                unit.setMoveTarget(unit.getFollowTarget().blockPosition());

            // remove fire from piglin units if they have research
            boolean hasImmunityResearch = ResearchServerEvents.playerHasResearch(unit.getOwnerName(), ProductionItems.RESEARCH_FIRE_RESISTANCE);
            if (hasImmunityResearch && unit.getFaction() == Faction.PIGLINS)
                unitMob.setRemainingFireTicks(0);
        }


        // slow regen for monster and piglin units
        LivingEntity le = (LivingEntity) unit;

        if (!le.level().isClientSide()) {
            if (unit.getFaction() == Faction.MONSTERS &&
                    le.tickCount % MONSTER_HEALING_TICKS == 0 &&
                    (!le.level().isDay())) {
                le.heal(1);
            } else if (unit.getFaction() == Faction.MONSTERS &&
                    (le.tickCount + MONSTER_HEALING_TICKS / 2) % MONSTER_HEALING_TICKS == 0 &&
                    (NightUtils.isInRangeOfNightSource(le.position(), le.level().isClientSide()))) {
                le.heal(1);
            } else if (unit.getFaction() == Faction.PIGLINS &&
                    le.tickCount % PIGLIN_HEALING_TICKS == 0 &&
                    !(unit instanceof Slime) &&
                    ((le.getVehicle() != null && NetherBlocks.isNetherBlock(le.level(), le.getVehicle().getOnPos())) ||
                    NetherBlocks.isNetherBlock(le.level(), le.getOnPos()) ||
                    unit instanceof GhastUnit)) {
                le.heal(1);
            }
        }

        if (le.isInWater() && // stuck in bridge
                BuildingUtils.findBuilding(le.level().isClientSide(), le.getOnPos().above()) instanceof BridgePlacement) {
            le.setDeltaMovement(0, 0.2, 0);
        }

        if (!le.level().getWorldBorder().isWithinBounds(le.getOnPos()))
            le.kill();

        if (unitMob.tickCount % 20 == 0)
            checkAndRetreatToAnchor(unit);
    }

    static boolean hasAnchor(Unit unit) {
        return unit.getAnchor() != null && !unit.getAnchor().equals(new BlockPos(0,0,0));
    }

    private static void checkAndRetreatToAnchor(Unit unit) {
        LivingEntity le = (LivingEntity) unit;
        if (!hasAnchor(unit) || le.level().isClientSide())
            return;

        if ((unit.isIdle() || le.distanceToSqr(Vec3.atCenterOf(unit.getAnchor())) > ANCHOR_RETREAT_RANGE * ANCHOR_RETREAT_RANGE) &&
            !le.getOnPos().equals(unit.getAnchor())) {
            fullResetBehaviours(unit);
            unit.getMoveGoal().setMoveTarget(unit.getAnchor());
        }
    }

    private static int getThresholdResources(Unit unit) {
        boolean hasCarryBags;
        if (((LivingEntity) unit).level().isClientSide())
            hasCarryBags = ResearchClient.hasResearch(ProductionItems.RESEARCH_RESOURCE_CAPACITY);
        else
            hasCarryBags = ResearchServerEvents.playerHasResearch(unit.getOwnerName(), ProductionItems.RESEARCH_RESOURCE_CAPACITY);
        return hasCarryBags ? 100 : 50;
    }

    static boolean atMaxResources(Unit unit) {
        return Resources.getTotalResourcesFromItems(unit.getItems()).getTotalValue() >= unit.getMaxResources();
    }

    static boolean atThresholdResources(Unit unit) {
        return Resources.getTotalResourcesFromItems(unit.getItems()).getTotalValue() >= getThresholdResources(unit);
    }

    default boolean hasLivingTarget() {
        Mob unitMob = (Mob) this;
        return unitMob.getTarget() != null && unitMob.getTarget().isAlive();
    }

    static void fullResetBehaviours(Unit unit) {
        if (((Entity) unit).level().isClientSide() && !Keybindings.shiftMod.isDown())
            unit.getCheckpoints().clear();
        unit.resetBehaviours();
        Unit.resetBehaviours(unit);
        if (unit instanceof WorkerUnit workerUnit) {
            WorkerUnit.resetBehaviours(workerUnit);
        }
        if (unit instanceof AttackerUnit attackerUnit) {
            AttackerUnit.resetBehaviours(attackerUnit);
        }
    }

    static void resetBehaviours(Unit unit) {
        unit.getTargetGoal().setTarget(null);
        unit.getMoveGoal().stopMoving();
        if (unit.getReturnResourcesGoal() != null)
            unit.getReturnResourcesGoal().stopReturning();
        unit.setFollowTarget(null);
        unit.setHoldPosition(false);
        if (unit.canGarrison())
            unit.getGarrisonGoal().stopGarrisoning();
        if (unit.canUsePortal()) {
            if (unit.getUsePortalGoal() instanceof FlyingUsePortalGoal flyingUsePortalGoal)
                flyingUsePortalGoal.stopUsingPortal();
            if (unit.getUsePortalGoal() instanceof UsePortalGoal usePortalGoal)
                usePortalGoal.stopUsingPortal();
        }
    }

    // can be overridden in the Unit's class to do additional logic on a reset
    default void resetBehaviours() { }

    // this setter sets a Unit field and so can't be defaulted
    // move to a block ignoring all else until reaching it
    default void setMoveTarget(@Nullable BlockPos bp) {
        this.getMoveGoal().setMoveTarget(bp);
    }

    // continuously move to a target until told to do something else
    void setFollowTarget(@Nullable LivingEntity target);

    void initialiseGoals();

    // weapons aren't provided automatically when spawned by custom code
    // also recalculate stats based on upgrades
    default void setupEquipmentAndUpgradesServer() { }

    // equipment only needs to be done serverside, but mod-specific fields need to be done clientside too
    default void setupEquipmentAndUpgradesClient() { }

    static float getSpeedModifier(Unit unit) {
        if (unit instanceof BruteUnit brute && brute.isHoldingUpShield) {
            return 0.5f;
        }
        return 1.0f;
    }

    static Ability getAbility(Unit unit, UnitAction abilityAction) {
        for (Ability ability : unit.getAbilities())
            if (ability.action.equals(abilityAction))
                return ability;
        return null;
    }

    default boolean isIdle() {
        boolean idleAttacker = true;
        if (this instanceof AttackerUnit attackerUnit)
            idleAttacker = attackerUnit.getAttackMoveTarget() == null &&
                            !((Unit) attackerUnit).hasLivingTarget();
        boolean idleWorker = true;
        if (this instanceof WorkerUnit)
            idleWorker = WorkerUnit.isIdle((WorkerUnit) this);

        return this.getMoveGoal().getMoveTarget() == null &&
                this.getFollowTarget() == null &&
                idleAttacker &&
                idleWorker;
    }

    void updateAbilityButtons();
    Level level();

    default void setCooldown(Ability abilityClass, float cooldown) {
        getCooldowns().put(abilityClass, cooldown);
    }

    default float getCooldown(Ability abilityClass) {
        return getCooldowns().get(abilityClass);
    }

    Object2ObjectArrayMap<Ability,Float> getCooldowns();

    boolean hasAutocast(Ability ability);
    void setAutocast(Ability ability);
    default void setCharges(Ability abilityClass, int cooldown) {
        getCharges().put(abilityClass, cooldown);
    }

    default int getCharges(Ability ability) {
        if (!getCharges().containsKey(ability))
            getCharges().put(ability, ability.maxCharges);
        return getCharges().get(ability);
    }
    Object2ObjectArrayMap<Ability,Integer> getCharges();
}
