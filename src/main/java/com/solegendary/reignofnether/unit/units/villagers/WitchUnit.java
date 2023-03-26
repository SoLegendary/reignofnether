package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.Ability;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.abilities.ThrowHarmingPotion;
import com.solegendary.reignofnether.unit.abilities.ThrowHealingPotion;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class WitchUnit extends Witch implements Unit {
    // region
    public Faction getFaction() {return Faction.VILLAGERS;}
    public List<AbilityButton> getAbilityButtons() {return abilityButtons;};
    public List<Ability> getAbilities() {return abilities;}
    public List<ItemStack> getItems() {return items;};
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
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
            SynchedEntityData.defineId(WitchUnit.class, EntityDataSerializers.STRING);

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

    final static public float maxHealth = 15.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.25f;
    final static public float sightRange = 10f;
    final static public int popCost = ResourceCosts.Witch.POPULATION;
    public int maxResources = 100;

    private final List<AbilityButton> abilityButtons = new ArrayList<>();
    private final List<Ability> abilities = new ArrayList<>();
    private final List<ItemStack> items = new ArrayList<>();

    public WitchUnit(EntityType<? extends Witch> entityType, Level level) {
        super(entityType, level);

        this.abilities.add(new ThrowHarmingPotion(this));
        this.abilities.add(new ThrowHealingPotion(this));

        if (level.isClientSide())
            for (Ability ability : this.abilities)
                this.abilityButtons.add(ability.getButton());
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public void throwPotion(LivingEntity targetEntity, Potion potion) {
        Vec3 dMovement = targetEntity.getDeltaMovement();
        double x = targetEntity.getX() + dMovement.x - this.getX();
        double y = targetEntity.getEyeY() - 1.1d - this.getY();
        double z = targetEntity.getZ() + dMovement.z - this.getZ();
        this.throwPotion(new Vec3(x, y, z), potion);
    }

    public void throwPotion(Vec3 targetBp, Potion potion) {
        double x = targetBp.x;
        double y = targetBp.y;
        double z = targetBp.z;
        double range = Math.sqrt(x * x + z * z);
        ThrownPotion thrownPotion = new ThrownPotion(this.level, this);
        thrownPotion.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), potion));
        thrownPotion.setXRot(thrownPotion.getXRot() - -20.0F);
        thrownPotion.shoot(x, y + range * 0.2, z, 0.75F, 8.0F);
        if (!this.isSilent()) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_THROW, this.getSoundSource(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
        }
        this.level.addFreshEntity(thrownPotion);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, WitchUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, WitchUnit.maxHealth)
                .add(Attributes.ARMOR, WitchUnit.armorValue);
    }

    public void tick() {
        this.setCanPickUpLoot(true);
        super.tick();
        Unit.tick(this);
    }

    public void initialiseGoals() {
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 1.0f, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.returnResourcesGoal = new ReturnResourcesGoal(this, 1.0f);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, returnResourcesGoal);
        this.targetSelector.addGoal(2, targetGoal);
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }
}
