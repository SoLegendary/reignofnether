package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.BridgePlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.nether.NetherBlocks;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.time.NightUtils;
import com.solegendary.reignofnether.tps.TPSClientEvents;
import com.solegendary.reignofnether.unit.*;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import com.solegendary.reignofnether.util.Faction;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

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

    static float HEAL_PER_NUTRITION = 2.5f;

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
    Abilities getAbilities();
    default List<Button> getAbilityButtons() {
        return getAbilities().getButtons(this);
    }
    List<ItemStack> getItems();
    int getMaxResources();

    public default boolean isEatingFood() { return getEatingTicksLeft() > 0; };
    public default boolean isHoldingEdibleFood() {
        for (ItemStack itemStack : getItems())
            if (ResourceSources.isPreparedFood(itemStack.getItem()))
                return true;
        return false;
    };
    public default Item getFoodBeingEaten() {
        for (ItemStack itemStack : getItems())
            if (ResourceSources.isPreparedFood(itemStack.getItem()))
                return itemStack.getItem();
        return Items.AIR;
    }
    public void setEatingTicksLeft(int amount);
    public int getEatingTicksLeft();

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

    public float getMovementSpeed();
    public float getUnitMaxHealth();
    public ResourceCost getCost();

    LivingEntity getFollowTarget();
    boolean getHoldPosition();
    void setHoldPosition(boolean holdPosition);

    String getOwnerName();
    void setOwnerName(String name);

    // SOURCE: armour attribute and armour items
    default float getUnitPhysicalArmorPercentage() {
        Mob mob = (Mob) this;
        return 1 - CombatRules.getDamageAfterAbsorb(1, (float)mob.getArmorValue(), (float)mob.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    // SOURCE: inherent unit stats and abilities
    default float getUnitRangedArmorPercentage() {
        return 0;
    }

    // SOURCE: inherent unit stats and vanilla mechanics (like resistance)
    default float getUnitMagicArmorPercentage() {
        Mob mob = (Mob) this;
        return 1 - mob.getDamageAfterMagicAbsorb(mob.damageSources().magic(), 1);
    }

    // SOURCE: resistance mob effect
    default float getUnitResistPercentage() {
        Mob mob = (Mob) this;
        MobEffectInstance mei = mob.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (mei != null) {
            return (float) (0.2 * (mei.getAmplifier() + 1));
        } else {
            return 0;
        }
    }

    public static void tick(Unit unit) {
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
                if (((Entity) unit).level().isClientSide())
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
            checkAndPickupEdibleFood(unit);
            checkAndPickupResources(unit);
            checkAndPickupEquipment(unit);

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
                    (MiscUtil.isOnNetherTerrain(le) || unit instanceof GhastUnit)) {
                le.heal(1);
            }
        }

        if (le.isInWater() && // stuck in bridge
                BuildingUtils.findBuilding(le.level().isClientSide(), le.getOnPos().above()) instanceof BridgePlacement) {
            le.setDeltaMovement(0, 0.2, 0);
        }

        if (!le.level().getWorldBorder().isWithinBounds(le.getOnPos()))
            le.kill();

        if (unitMob.tickCount % 50 == 0)
            checkAndRetreatToAnchor(unit);

        if (unit.getSunlightEffect() == SunlightEffect.SLOWNESS_II ||
            unit.getSunlightEffect() == SunlightEffect.SLOWNESS_I) {
            // apply slowness during daytime for a short time repeatedly
            if (unitMob.tickCount % 10 == 0 && !unitMob.level().isClientSide() && unitMob.level().isDay() &&
                    !NightUtils.isInRangeOfNightSource(unitMob.getEyePosition(), false) &&
                    !ResearchServerEvents.playerHasCheat(unit.getOwnerName(), "slipslopslap"))
                unitMob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15,
                        unit.getSunlightEffect() == SunlightEffect.SLOWNESS_I ? 0 : 1
                ));
        }

        if (unitMob.tickCount % 20 == 0) {
            if (unitMob.hasEffect(MobEffectRegistrar.UNCONTROLLABLE.get())) {
                addParticlesAroundSelf(unit, ParticleTypes.ANGRY_VILLAGER);
            }
        }

        if (unit.isEatingFood()) {
            unit.setEatingTicksLeft(unit.getEatingTicksLeft() - 1);
            if (!unit.isEatingFood()) {
                for (ItemStack itemStack : unit.getItems()) {
                    if (ResourceSources.isPreparedFood(itemStack.getItem())) {
                        unitMob.level().playSound(null, unitMob.getX(), unitMob.getY(), unitMob.getZ(),
                                SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F,
                                unitMob.getRandom().nextFloat() * 0.1F + 0.9F
                        );
                        int nutrition = itemStack.getItem().getFoodProperties(itemStack, (LivingEntity) unit).getNutrition();
                        if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                            unitMob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 999999, 5));
                            unitMob.setAbsorptionAmount(24);
                        } else {
                            unitMob.heal(nutrition * HEAL_PER_NUTRITION);
                        }
                        itemStack.setCount(itemStack.getCount() - 1);
                        break;
                    }
                }
            } else if (unit.getEatingTicksLeft() % 4 == 0) {
                unitMob.level().playSound(null, unitMob.getX(), unitMob.getY(), unitMob.getZ(),
                        SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.5F,
                        unitMob.getRandom().nextFloat() * 0.1F + 0.9F
                );
            }
        } else {
            for (ItemStack itemStack : unit.getItems()) {
                if (ResourceSources.isPreparedFood(itemStack.getItem())) {
                    unit.setEatingTicksLeft(40);
                    break;
                }
            }
        }
        if (unitMob.hasEffect(MobEffects.ABSORPTION) && unitMob.getAbsorptionAmount() <= 0)
            unitMob.removeEffect(MobEffects.ABSORPTION);

        if (unitMob.tickCount % 10 == 0 &&
            unit.getFaction() == Faction.PIGLINS &&
            MiscUtil.isOnNetherTerrain(unitMob)) {
            unitMob.addEffect(new MobEffectInstance(MobEffectRegistrar.MINOR_MOVEMENT_SPEED.get(), 15, 1, true, false));
        }
    }

    private static void checkAndPickupResources(Unit unit) {
        Mob unitMob = (Mob) unit;
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
    }

    private static void checkAndPickupEquipment(Unit unit) {
        Mob unitMob = (Mob) unit;
        for (ItemEntity itementity : unitMob.level().getEntitiesOfClass(ItemEntity.class, unitMob.getBoundingBox().inflate(1, 0, 1))) {

            Relationship rl = UnitServerEvents.getUnitToEntityRelationship(unit, itementity);
            ItemStack itemstack = itementity.getItem();
            if (unit.canPickUpEquipment(itemstack) && !itementity.isRemoved() &&
                !itemstack.isEmpty() && !itementity.hasPickUpDelay() && unitMob.isAlive() &&
                (rl != Relationship.HOSTILE || itementity.tickCount > 100)) {

                unitMob.onItemPickup(itementity);
                unitMob.take(itementity, 1);
                unit.onPickupEquipment(itemstack);
                itementity.discard();
                break;
            }
        }
    }

    default boolean canPickUpEquipment(ItemStack itemStack) { return false; }

    default void onPickupEquipment(ItemStack itemStack) { }

    static int HOSTILE_FOOD_DELAY_TICKS = 200;

    private static void checkAndPickupEdibleFood(Unit unit) {
        Mob unitMob = (Mob) unit;
        if (!unit.isHoldingEdibleFood()) {
            for (ItemEntity itementity : unitMob.level().getEntitiesOfClass(ItemEntity.class, unitMob.getBoundingBox().inflate(1, 0, 1))) {

                ItemStack itemstack = itementity.getItem();
                if (itemstack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                    if (unitMob.getAbsorptionAmount() > 0)
                        continue;
                } else if (unitMob.getHealth() >= unitMob.getMaxHealth()) {
                    continue;
                }
                Relationship rl = UnitServerEvents.getUnitToEntityRelationship(unit, itementity);
                if (!itementity.isRemoved() && !itemstack.isEmpty() && !itementity.hasPickUpDelay() && unitMob.isAlive() && !unit.getOwnerName().isEmpty() &&
                    (rl != Relationship.HOSTILE || itementity.tickCount > HOSTILE_FOOD_DELAY_TICKS) && ResourceSources.isPreparedFood(itemstack.getItem())) {
                    if (ResourceSources.isPreparedFood(itemstack.getItem()) &&
                            (unitMob.getHealth() < unitMob.getMaxHealth() || itemstack.getItem() == Items.ENCHANTED_GOLDEN_APPLE)) {
                        unitMob.onItemPickup(itementity);
                        unitMob.take(itementity, 1);
                        unit.getItems().add(new ItemStack(itemstack.getItem(), 1));
                        UnitAnimationClientboundPacket.sendEatFoodPacket(unitMob, BuiltInRegistries.ITEM.getId(itemstack.getItem()));
                        itemstack.setCount(itemstack.getCount() - 1);
                        if (itemstack.getCount() <= 0)
                            itementity.discard();
                        break;
                    }
                }
            }
        }
    }

    // call from addAdditionalSaveData
    public default void addUnitSaveData(@NotNull CompoundTag pCompound) {
        pCompound.putString("ownerName", getOwnerName());
        if (getAnchor() != null) {
            pCompound.putInt("anchorPosX", getAnchor().getX());
            pCompound.putInt("anchorPosY", getAnchor().getY());
            pCompound.putInt("anchorPosZ", getAnchor().getZ());
        }
        if (this instanceof HeroUnit heroUnit)
            heroUnit.addHeroUnitSaveData(pCompound);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack itemStack = ((LivingEntity) this).getItemBySlot(slot);
            if (itemStack.getItem() != Items.AIR)
                pCompound.put(slot.name() + "Item", itemStack.serializeNBT());
        }
    }

    // call from readAdditionalSaveData
    public default void readUnitSaveData(@NotNull CompoundTag pCompound) {
        setOwnerName(pCompound.getString("ownerName"));
        BlockPos anchorPos = new BlockPos(
            pCompound.getInt("anchorPosX"),
            pCompound.getInt("anchorPosY"),
            pCompound.getInt("anchorPosZ")
        );
        if (!anchorPos.equals(new BlockPos(0,0,0))) {
            setAnchor(anchorPos);
        }
        if (this instanceof HeroUnit heroUnit)
            heroUnit.readHeroUnitSaveData(pCompound);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String keyName = slot.name() + "Item";
            if (pCompound.contains(keyName)) {
                CompoundTag itemNbt = (CompoundTag) pCompound.get(keyName);
                if (itemNbt != null) {
                    ((LivingEntity) this).setItemSlot(slot, ItemStack.of(itemNbt));
                }
            }
        }
    }

    public enum SunlightEffect {
        NONE,
        SLOWNESS_II,
        SLOWNESS_I,
        FIRE
    }

    public default SunlightEffect getSunlightEffect() {
        return SunlightEffect.NONE;
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
        for (Ability ability : unit.getAbilities().get())
            if (ability.action.equals(abilityAction))
                return ability;
        return null;
    }

    default boolean isIdle() {
        boolean idleAttacker = true;
        boolean idleBuildingAttacker = true;
        if (this instanceof AttackerUnit attackerUnit) {
            idleAttacker = attackerUnit.getAttackMoveTarget() == null &&
                    !((Unit) attackerUnit).hasLivingTarget() &&
                    !AttackerUnit.isAttackingBuilding(attackerUnit);
        }
        boolean idleWorker = true;
        if (this instanceof WorkerUnit)
            idleWorker = WorkerUnit.isIdle((WorkerUnit) this);

        // some larger mobs like bears get stuck near their movetarget so nav won't be done but it also won't be null
        boolean stationaryNearMoveTarget = false;
        if (this.getMoveGoal().getMoveTarget() != null) {
            double distToMoveTarget = ((LivingEntity) this).distanceToSqr(this.getMoveGoal().getMoveTarget().getCenter());
            boolean stationary = ((Mob) this).getDeltaMovement().x == 0 || ((Mob) this).getDeltaMovement().z == 0;
            stationaryNearMoveTarget = stationary && distToMoveTarget < 4;
        }
        return (this.getMoveGoal().getMoveTarget() == null || stationaryNearMoveTarget) &&
                this.getFollowTarget() == null &&
                idleAttacker &&
                idleWorker;
    }

    static Random RANDOM = new Random();

    public static void addParticlesAroundSelf(Unit unit, ParticleOptions pParticleOption) {
        for(int i = 0; i < 5; ++i) {
            double d0 = RANDOM.nextGaussian() * 0.02;
            double d1 = RANDOM.nextGaussian() * 0.02;
            double d2 = RANDOM.nextGaussian() * 0.02;
            Entity entity = (Entity) unit;

            if (!entity.level().isClientSide) {
                ((ServerLevel) entity.level()).sendParticles(pParticleOption,
                        entity.getRandomX(1.0),
                        entity.getRandomY() + 1.0,
                        entity.getRandomZ(1.0),
                        1, d0, d1, d2, 0
                );
            }
        }
    }

    void updateAbilityButtons();

    default boolean isCasting() {
        for (Ability ability : getAbilities().get())
            if (ability.isCasting(this))
                return true;
        return false;
    }

    public default List<FormattedCharSequence> getAttackDamageStatTooltip() {
        return List.of(fcs(I18n.get("unitstats.reignofnether.attack_damage"), true));
    }

    public default boolean hasBonusDamage() {
        return false;
    }

    public default boolean hasBonusAttackSpeed() {
        return false;
    }

    public default List<FormattedCharSequence> getAttackSpeedStatTooltip() {
        return List.of(fcs(I18n.get("unitstats.reignofnether.attack_speed"), true));
    }

    public default List<FormattedCharSequence> getRangeStatTooltip() {
        return List.of(fcs(I18n.get("unitstats.reignofnether.range"), true));
    }

    public default List<FormattedCharSequence> getArmourStatTooltip() {
        ArrayList<FormattedCharSequence> fcsList = new ArrayList<>();
        fcsList.add(fcs(I18n.get("unitstats.reignofnether.armour"), true));
        if (getUnitPhysicalArmorPercentage() > 0) {
            fcsList.add(fcs(I18n.get("unitstats.reignofnether.armour_melee_and_ranged", (int) (getUnitPhysicalArmorPercentage() * 100)), false));
        }
        if (getUnitRangedArmorPercentage() > 0) {
            fcsList.add(fcs(I18n.get("unitstats.reignofnether.armour_ranged", (int) (getUnitRangedArmorPercentage() * 100)), false));
        }
        if (getUnitResistPercentage() > 0) {
            fcsList.add(fcs(I18n.get("unitstats.reignofnether.armour_all", (int) (getUnitResistPercentage() * 100)), false));
        }
        else if (getUnitMagicArmorPercentage() > 0) {
            fcsList.add(fcs(I18n.get("unitstats.reignofnether.armour_magic", (int) (getUnitMagicArmorPercentage() * 100)), false));
        }
        return fcsList;
    }

    public default List<FormattedCharSequence> getMovementSpeedStatTooltip() {
        return List.of(fcs(I18n.get("unitstats.reignofnether.movement_speed"), true));
    }

    public default List<FormattedCharSequence> getStatTooltip(UnitStatType unitStatType) {
        return switch (unitStatType) {
            case ATTACK_DAMAGE -> getAttackDamageStatTooltip();
            case ATTACK_SPEED -> getAttackSpeedStatTooltip();
            case RANGE -> getRangeStatTooltip();
            case ARMOUR -> getArmourStatTooltip();
            case MOVEMENT_SPEED -> getMovementSpeedStatTooltip();
        };
    }

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