package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.CastFangsCircle;
import com.solegendary.reignofnether.ability.abilities.CastFangsLine;
import com.solegendary.reignofnether.ability.abilities.CastSummonVexes;
import com.solegendary.reignofnether.ability.abilities.PromoteIllager;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EvokerUnit extends Evoker implements Unit {
    // region
    private final ArrayList<BlockPos> checkpoints = new ArrayList<>();
    private int checkpointTicksLeft = UnitClientEvents.CHECKPOINT_TICKS_MAX;
    public ArrayList<BlockPos> getCheckpoints() { return checkpoints; };
    public int getCheckpointTicksLeft() { return checkpointTicksLeft; }
    public void setCheckpointTicksLeft(int ticks) { checkpointTicksLeft = ticks; }
    private boolean isCheckpointGreen = true;
    public boolean isCheckpointGreen() { return isCheckpointGreen; };
    public void setIsCheckpointGreen(boolean green) { isCheckpointGreen = green; };
    private int entityCheckpointId = -1;
    public int getEntityCheckpointId() { return entityCheckpointId; };
    public void setEntityCheckpointId(int id) { entityCheckpointId = id; };

    GarrisonGoal garrisonGoal;
    public GarrisonGoal getGarrisonGoal() { return garrisonGoal; }
    public boolean canGarrison() { return true; }

    public Faction getFaction() {return Faction.VILLAGERS;}
    public List<AbilityButton> getAbilityButtons() {return abilityButtons;}
    public List<Ability> getAbilities() {return abilities;}
    public List<ItemStack> getItems() {return items;}
    public MoveToTargetBlockGoal getMoveGoal() {return moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public ReturnResourcesGoal getReturnResourcesGoal() {return returnResourcesGoal;}
    public int getMaxResources() {return maxResources;}

    private MoveToTargetBlockGoal moveGoal;
    private SelectedTargetGoal<? extends LivingEntity> targetGoal;
    public BuildRepairGoal buildRepairGoal;
    public GatherResourcesGoal gatherResourcesGoal;
    private ReturnResourcesGoal returnResourcesGoal;

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
            SynchedEntityData.defineId(EvokerUnit.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
    }

    // combat stats
    public float getMovementSpeed() {return movementSpeed;}
    public float getUnitMaxHealth() {return maxHealth;}
    public float getUnitArmorValue() {return armorValue;}
    public int getPopCost() {return popCost;}

    public void setFollowTarget(@Nullable LivingEntity target) { this.followTarget = target; }

    // endregion

    private CastFangsLineGoal castFangsLineGoal;
    public CastFangsLineGoal getCastFangsLineGoal() {
        return castFangsLineGoal;
    }
    private CastFangsCircleGoal castFangsCircleGoal;
    public CastFangsCircleGoal getCastFangsCircleGoal() {
        return castFangsCircleGoal;
    }
    private CastSummonVexesGoal castSummonVexesGoal;
    public CastSummonVexesGoal getCastSummonVexesGoal() {
        return castSummonVexesGoal;
    }

    public static final int FANGS_RANGE = 10;
    public static final float FANGS_DAMAGE = 6f; // can sometimes be doubled or tripled due to overlapping fang hitboxes
    public static final int FANGS_CHANNEL_TICKS = 1 * ResourceCost.TICKS_PER_SECOND;

    final static public float maxHealth = 40.0f;
    final static public float armorValue = 0.0f;
    final static public float movementSpeed = 0.25f;
    final static public int popCost = ResourceCosts.EVOKER.population;
    public int maxResources = 100;

    private final List<AbilityButton> abilityButtons = new ArrayList<>();
    private final List<Ability> abilities = new ArrayList<>();
    private final List<ItemStack> items = new ArrayList<>();

    public EvokerUnit(EntityType<? extends Evoker> entityType, Level level) {
        super(entityType, level);

        CastFangsLine ab1 = new CastFangsLine(this);
        CastFangsCircle ab2 = new CastFangsCircle(this);
        CastSummonVexes ab3 = new CastSummonVexes(this);
        this.abilities.add(ab1);
        this.abilities.add(ab2);
        this.abilities.add(ab3);

        if (level.isClientSide()) {
            this.abilityButtons.add(ab1.getButton(Keybindings.keyQ));
            this.abilityButtons.add(ab2.getButton(Keybindings.keyW));
            this.abilityButtons.add(ab3.getButton(Keybindings.keyE));
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, EvokerUnit.movementSpeed)
                .add(Attributes.MAX_HEALTH, EvokerUnit.maxHealth)
                .add(Attributes.ARMOR, EvokerUnit.armorValue);
    }

    @Override
    public void resetBehaviours() {
        this.castFangsLineGoal.stop();
        this.castFangsCircleGoal.stop();
        this.castSummonVexesGoal.stop();
    }

    public void tick() {
        this.setCanPickUpLoot(true);
        super.tick();
        Unit.tick(this);
        this.castFangsLineGoal.tick();
        this.castFangsCircleGoal.tick();
        this.castSummonVexesGoal.tick();
        PromoteIllager.checkAndApplyBuff(this);

        // vexes will inherit this target
        if (this.getTarget() == null && !this.level.isClientSide()) {
            Mob target = MiscUtil.findClosestAttackableEnemy(this, 10, (ServerLevel) level);
            if (target != null)
                this.setTarget(target);
        }
    }

    public void initialiseGoals() {
        this.moveGoal = new MoveToTargetBlockGoal(this, false, 1.0f, 0);
        this.targetGoal = new SelectedTargetGoal<>(this, true, true);
        this.garrisonGoal = new GarrisonGoal(this, 1.0f);
        this.returnResourcesGoal = new ReturnResourcesGoal(this, 1.0f);
        this.castFangsLineGoal = new CastFangsLineGoal(this, FANGS_CHANNEL_TICKS, FANGS_RANGE, this::createEvokerFangsLine);
        this.castFangsCircleGoal = new CastFangsCircleGoal(this);
        this.castSummonVexesGoal = new CastSummonVexesGoal(this);
    }

    @Override
    protected void registerGoals() {
        initialiseGoals();

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, returnResourcesGoal);
        this.goalSelector.addGoal(2, garrisonGoal);
        this.targetSelector.addGoal(2, targetGoal);
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    // controls whether the evoker's arms are up or not
    @Override
    public boolean isCastingSpell() {
        if (this.getCastFangsLineGoal() != null && this.getCastFangsLineGoal().isCasting())
            return true;
        if (this.getCastFangsCircleGoal() != null && this.getCastFangsCircleGoal().isCasting())
            return true;
        if (this.getCastSummonVexesGoal() != null && this.getCastSummonVexesGoal().isCasting())
            return true;
        return false;
    }

    // based on Evoker.EvokerAttackSpellGoal.performSpellCasting
    public void createEvokerFangsLine(BlockPos targetPos) {
        double d0 = Math.min(targetPos.getY(), this.getY());
        double d1 = Math.max(targetPos.getY(), this.getY()) + 1.0;
        float f = (float)Mth.atan2(targetPos.getZ() - this.getZ(), targetPos.getX() - this.getX());
        int k;
        for(k = 0; k < FANGS_RANGE; ++k) {
            double d2 = 1.25 * (double)(k + 1);
            createEvokerFang(this.getX() + (double)Mth.cos(f) * d2, this.getZ() + (double)Mth.sin(f) * d2, d0, d1, f, k);
        }
    }

    // based on Evoker.EvokerAttackSpellGoal.performSpellCasting
    public void createEvokerFangsCircle() {
        int k;
        float f2;
        for(k = 0; k < 5; ++k) {
            f2 = (float)k * (float) Math.PI * 0.4F;
            createEvokerFang(this.getX() + (double)Mth.cos(f2) * 1.5, this.getZ() + (double)Mth.sin(f2) * 1.5, this.getY(), this.getY() + 1, f2, 0);
        }
        for(k = 0; k < 8; ++k) {
            f2 = (float)k * (float) Math.PI * 2.0F / 8.0F + 1.2566371F;
            createEvokerFang(this.getX() + (double)Mth.cos(f2) * 2.5, this.getZ() + (double)Mth.sin(f2) * 2.5, this.getY(), this.getY() + 1, f2, 3);
        }
    }

    // based on Evoker.EvokerAttackSpellGoal.createSpellEntity
    private void createEvokerFang(double pX, double pZ, double pMinY, double pMaxY, float pYRot, int pWarmupDelay) {
        BlockPos blockpos = new BlockPos(pX, pMaxY, pZ);
        boolean flag = false;
        double d0 = 0.0;

        do {
            BlockPos blockpos1 = blockpos.below();
            BlockState blockstate = this.level.getBlockState(blockpos1);
            if (blockstate.isFaceSturdy(this.level, blockpos1, Direction.UP)) {
                if (!this.level.isEmptyBlock(blockpos)) {
                    BlockState blockstate1 = this.level.getBlockState(blockpos);
                    VoxelShape voxelshape = blockstate1.getCollisionShape(this.level, blockpos);
                    if (!voxelshape.isEmpty())
                        d0 = voxelshape.max(Direction.Axis.Y);
                }
                flag = true;
                break;
            }
            blockpos = blockpos.below();
        } while(blockpos.getY() >= Mth.floor(pMinY) - 1);

        if (flag)
            this.level.addFreshEntity(new EvokerFangs(this.level, pX, (double)blockpos.getY() + d0, pZ, pYRot, pWarmupDelay, this));
    }

    public void summonVexes() {
        if (this.level.isClientSide())
            return;

        for(int i = 0; i < 3; ++i) {
            BlockPos blockpos = this.blockPosition().offset(-2 + this.random.nextInt(5), 1, -2 + this.random.nextInt(5));
            Vex vex = EntityType.VEX.create(this.level);
            if (vex != null) {
                vex.moveTo(blockpos, 0.0F, 0.0F);
                vex.finalizeSpawn((ServerLevel) this.level, this.level.getCurrentDifficultyAt(blockpos), MobSpawnType.MOB_SUMMONED, null, null);
                vex.setOwner(this);
                vex.setBoundOrigin(blockpos);
                vex.setLimitedLife(CastSummonVexes.VEX_DURATION_SECONDS * ResourceCost.TICKS_PER_SECOND);
                ((ServerLevel) this.level).addFreshEntityWithPassengers(vex);
            }
        }
    }
}
