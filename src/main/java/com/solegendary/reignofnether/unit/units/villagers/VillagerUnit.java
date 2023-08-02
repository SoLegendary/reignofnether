package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.shared.Stockpile;
import com.solegendary.reignofnether.building.buildings.villagers.*;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServer;
import com.solegendary.reignofnether.research.researchItems.ResearchResourceCapacity;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.ArmSwingingUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.unit.units.modelling.VillagerUnitModel;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class VillagerUnit extends Vindicator implements Unit, WorkerUnit, ArmSwingingUnit {
    // region
    public Faction getFaction() {return Faction.VILLAGERS;}
    public List<AbilityButton> getAbilityButtons() {return abilityButtons;};
    public List<Ability> getAbilities() {return abilities;}
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public BuildRepairGoal getBuildRepairGoal() {return buildRepairGoal;}
    public GatherResourcesGoal getGatherResourceGoal() {return gatherResourcesGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}

    public MoveToTargetBlockGoal moveGoal;
    public SelectedTargetGoal<? extends LivingEntity> targetGoal;
    public BuildRepairGoal buildRepairGoal;
    public GatherResourcesGoal gatherResourcesGoal;
    public ReturnResourcesGoal returnResourcesGoal;

    public LivingEntity getFollowTarget() { return followTarget; }
    public boolean getHoldPosition() { return holdPosition; }
    public void setHoldPosition(boolean holdPosition) { this.holdPosition = holdPosition; }

    // if true causes moveGoal and attackGoal to work together to allow attack moving
    // moves to a block but will chase/attack nearby monsters in range up to a certain distance away
    private LivingEntity followTarget = null; // if nonnull, continuously moves to the target
    private boolean holdPosition = false;

    // which player owns this unit? this format ensures its synched to client without having to use packets
    public String getOwnerName() { return this.entityData.get(ownerDataAccessor); }
    public void setOwnerName(String name) { this.entityData.set(ownerDataAccessor, name); }
    public static final EntityDataAccessor<String> ownerDataAccessor =
            SynchedEntityData.defineId(VillagerUnit.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
    }

    // combat stats
    public float getMovementSpeed() {return movementSpeed;}
    public float getUnitMaxHealth() {return maxHealth;}
    public float getUnitArmorValue() {return armorValue;}
    public float getSightRange() {return sightRange;}
    public int getPopCost() {return popCost;}

    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    public BlockState getReplantBlockState() {
        return Blocks.WHEAT.defaultBlockState();
    }

    final static public float maxHealth = 20.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.25f;
    final static public float sightRange = 10f;
    final static public int popCost = ResourceCosts.VILLAGER.population;
    public int maxResources = 100;

    private final List<AbilityButton> abilityButtons = new ArrayList<>();
    private final List<Ability> abilities = new ArrayList<>();
    private final List<ItemStack> items = new ArrayList<>();

    private boolean isSwingingArmOnce = false;
    private int swingTime = 0;

    public int getSwingTime() {
        return swingTime;
    }

    public void setSwingTime(int time) {
        this.swingTime = time;
    }

    public boolean isSwingingArmOnce() {
        return isSwingingArmOnce;
    }

    public void setSwingingArmOnce(boolean swing) {
        isSwingingArmOnce = swing;
    }

    public boolean isSwingingArmRepeatedly() {
        return (this.getGatherResourceGoal().isGathering() || this.getBuildRepairGoal().isBuilding());
    }

    public VillagerUnit(EntityType<? extends Vindicator> entityType, Level level) {
        super(entityType, level);

        if (level.isClientSide()) {
            AbilityButton townCentreButton = TownCentre.getBuildButton(Keybindings.keyQ);
            townCentreButton.isEnabled = () -> !BuildingUtils.doesPlayerOwnTownCentre(getOwnerName());
            this.abilityButtons.add(townCentreButton);
            this.abilityButtons.add(Stockpile.getBuildButton(Keybindings.keyW));
            this.abilityButtons.add(VillagerHouse.getBuildButton(Keybindings.keyE));
            this.abilityButtons.add(WheatFarm.getBuildButton(Keybindings.keyR));
            this.abilityButtons.add(Barracks.getBuildButton(Keybindings.keyT));
            this.abilityButtons.add(Blacksmith.getBuildButton(Keybindings.keyY));
            this.abilityButtons.add(ArcaneTower.getBuildButton(Keybindings.keyU));
            this.abilityButtons.add(Library.getBuildButton(Keybindings.keyI));
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, VillagerUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, VillagerUnit.maxHealth)
                .add(Attributes.ARMOR, VillagerUnit.armorValue);
    }

    public VillagerUnitModel.ArmPose getVillagerUnitArmPose() {
        if (this.buildRepairGoal != null && this.buildRepairGoal.isBuilding())
            return VillagerUnitModel.ArmPose.BUILDING;
        else if (this.gatherResourcesGoal != null && this.gatherResourcesGoal.isGathering())
            return VillagerUnitModel.ArmPose.GATHERING;
        return VillagerUnitModel.ArmPose.CROSSED;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource p_34103_) {
        return SoundEvents.VILLAGER_HURT;
    }


    public void tick() {
        this.setCanPickUpLoot(true);

        super.tick();
        Unit.tick(this);
        WorkerUnit.tick(this);
    }

    public void initialiseGoals() {
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 1.0f, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.buildRepairGoal = new BuildRepairGoal(this, 1.0f);
        this.gatherResourcesGoal = new GatherResourcesGoal(this, 1.0f);
        this.returnResourcesGoal = new ReturnResourcesGoal(this, 1.0f);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, buildRepairGoal);
        this.goalSelector.addGoal(2, gatherResourcesGoal);
        this.goalSelector.addGoal(2, returnResourcesGoal);
        this.targetSelector.addGoal(2, targetGoal);
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void setupEquipmentAndUpgradesClient() {
        if (ResearchClient.hasResearch(ResearchResourceCapacity.itemName))
            this.maxResources = 150;
    }

    @Override
    public void setupEquipmentAndUpgradesServer() {
        if (ResearchServer.playerHasResearch(this.getOwnerName(), ResearchResourceCapacity.itemName))
            this.maxResources = 150;
    }
}
