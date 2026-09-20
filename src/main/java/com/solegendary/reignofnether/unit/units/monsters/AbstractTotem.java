package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.heroAbilities.enchanter.MarchOfProgress;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.blocks.RangeIndicator;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractTotem extends Mob implements Unit, RangeIndicator {
    public static final Abilities ABILITIES = new Abilities();

    //region
    @Override
    public void updateAbilityButtons() { }
    Object2ObjectArrayMap<Ability, Float> cooldowns = Unit.createCooldownMap();
    Object2ObjectArrayMap<Ability, Integer> charges = new Object2ObjectArrayMap<>();
    @Override public Object2ObjectArrayMap<Ability, Float> getCooldowns() { return cooldowns; }
    @Override public boolean hasAutocast(Ability ability) { return false; }
    @Override public void setAutocast(Ability autocast) {  }
    @Override public Object2ObjectArrayMap<Ability, Integer> getCharges() { return charges; }

    public void setEatingTicksLeft(int amount) {  }
    public int getEatingTicksLeft() { return 0; }
    public void setAnchor(BlockPos bp) {  }
    public BlockPos getAnchor() { return null; }

    private final ArrayList<Checkpoint> checkpoints = new ArrayList<>();
    public ArrayList<Checkpoint> getCheckpoints() { return checkpoints; };

    public GarrisonGoal getGarrisonGoal() { return null; }
    public boolean canGarrison() { return getGarrisonGoal() != null; }

    public UsePortalGoal getUsePortalGoal() { return null; }
    public boolean canUsePortal() { return getUsePortalGoal() != null; }

    public Faction getFaction() {return Faction.NEUTRAL;}
    public Abilities getAbilities() {return abilities;}
    public List<ItemStack> getItems() {return List.of();}
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return null;}
    public int getMaxResources() {return 0;}

    private MoveToTargetBlockGoal moveGoal;
    private SelectedTargetGoal<? extends LivingEntity> targetGoal;

    public LivingEntity getFollowTarget() { return null; }
    public boolean getHoldPosition() { return false; }
    public void setHoldPosition(boolean holdPosition) {  }

    // which player owns this unit? this format ensures its synched to client without having to use packets
    public String getOwnerName() { return this.entityData.get(ownerDataAccessor); }
    public void setOwnerName(String name) { this.entityData.set(ownerDataAccessor, name); }
    public static final EntityDataAccessor<String> ownerDataAccessor =
            SynchedEntityData.defineId(AbstractTotem.class, EntityDataSerializers.STRING);

    // which scenario role does this unit use?
    public int getScenarioRoleIndex() { return this.entityData.get(scenarioRoleDataAccessor); }
    public void setScenarioRoleIndex(int index) { this.entityData.set(scenarioRoleDataAccessor, index); }
    public static final EntityDataAccessor<Integer> scenarioRoleDataAccessor =
            SynchedEntityData.defineId(AbstractTotem.class, EntityDataSerializers.INT);

    public String getOnDeathCommand() { return this.entityData.get(onDeathCommandDataAccessor); }
    public void setOnDeathCommand(String command) { this.entityData.set(onDeathCommandDataAccessor, command); }
    public static final EntityDataAccessor<String> onDeathCommandDataAccessor =
            SynchedEntityData.defineId(AbstractTotem.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
        this.entityData.define(scenarioRoleDataAccessor, -1);
        this.entityData.define(onDeathCommandDataAccessor, "");
    }

    public ResourceCost getCost() {return ResourceCost.Unit(0,0,0,0,0);}

    public void setFollowTarget(@Nullable LivingEntity target) {  }

    private Set<BlockPos> highlightBps = new HashSet<>();

    @Override public Set<BlockPos> getHighlightBps() { return highlightBps; }
    @Override public void setHighlightBps(Set<BlockPos> bps) { highlightBps = bps; }
    @Override public boolean showOnlyWhenSelected() {
        return false;
    }

    @Override
    public void updateHighlightBps(Level level) {
        RangeIndicator.super.updateHighlightBps(level());
        if (level().isClientSide()) {
            this.highlightBps.addAll(MiscUtil.getRangeIndicatorCircleBlocks(blockPosition(),
                    (int) AURA_RANGE - 1,
                    level()
            ));
        }
    }

    // endregion

    final static public float MAX_HEALTH = 20.0f;
    final static public float AURA_RANGE = 10.0f;

    final static public float rangedDamageResist = 0.5f;
    final static public float magicDamageResist = 0.5f;

    final protected HashMap<MobEffect, Integer> auraEffects = new HashMap<>();

    private Abilities abilities = ABILITIES.clone();

    public AbstractTotem(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        updateAbilityButtons();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Unit.createDefaultAttributes()
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.MAX_HEALTH, AbstractTotem.MAX_HEALTH)
                .add(Attributes.FOLLOW_RANGE, 0)
                .add(Attributes.ARMOR, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 9999.0f)
                .add(AttributeRegistrar.RANGED_DAMAGE_RESIST.get(), rangedDamageResist)
                .add(AttributeRegistrar.MAGIC_DAMAGE_RESIST.get(), magicDamageResist);

    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public void tick() {
        this.setCanPickUpLoot(false);
        super.tick();
        Unit.tick(this);

        if (level().isClientSide && highlightBps.isEmpty()) {
            updateHighlightBps(level());
        }

        if (!level().isClientSide && tickCount % 20 == 0) {
            SoundClientboundPacket.playSoundAtPos(SoundAction.BEACON_AMBIENT, blockPosition(), 1.5f);
            for (Mob mob : MiscUtil.getEntitiesWithinRange(position(), AURA_RANGE, Mob.class, level())) {
                if (mob instanceof Unit unit && AlliancesServerEvents.isAlliedOrOwned(unit.getOwnerName(), getOwnerName()) && !(unit instanceof AbstractTotem)) {
                    for (MobEffect mobEffect : auraEffects.keySet())
                        mob.addEffect(new MobEffectInstance(mobEffect, 30, auraEffects.get(mobEffect), false, true));
                }
            }
        }
    }

    @Override
    public void remove(@NotNull RemovalReason pReason) {
        if (this.level() instanceof ServerLevel serverLevel) {
            String command = this.getOnDeathCommand();
            if (command != null && !command.isEmpty()) {
                CommandSourceStack source;
                source = serverLevel.getServer()
                        .createCommandSourceStack()
                        .withEntity(this)
                        .withPosition(this.position())
                        .withLevel(serverLevel)
                        .withPermission(2);
                serverLevel.getServer().getCommands().performPrefixedCommand(source, command);
            }
        }
        super.remove(pReason);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        this.addUnitSaveData(pCompound);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.readUnitSaveData(pCompound);
    }

    public void initialiseGoals() {
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();
        this.goalSelector.addGoal(1, new FloatGoal(this));
    }

    @Override
    public AABB getInflatedSelectionBox() {
        AABB aabb = this.getBoundingBox().inflate(0.05f, 0, 0.05f);
        aabb.setMaxY(aabb.maxY + 0.4f);
        return aabb;
    }
}
