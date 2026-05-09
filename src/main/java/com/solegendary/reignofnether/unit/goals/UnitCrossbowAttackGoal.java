package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.Random;

import static com.solegendary.reignofnether.unit.goals.UnitCrossbowAttackGoal.CrossbowState.*;
// - has an attack cooldown parameter in the constructor
// - has no pathfinding delay
// - stops when the target is dead

public class UnitCrossbowAttackGoal<T extends Monster & RangedAttackMob & CrossbowAttackMob> extends Goal {
    enum CrossbowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK;
    }

    private final Random random = new Random();

    private final T mob;
    private UnitCrossbowAttackGoal.CrossbowState crossbowState = UNCHARGED;
    private int seeTime;
    private int attackCooldown;
    private int attackCooldownMax;
    private int windupTime = random.nextInt(0,6);

    private static final int GARRISON_BONUS_RANGE_TO_GHASTS = 10;

    public UnitCrossbowAttackGoal(T mob, int attackCooldown) {
        this.mob = mob;
        this.attackCooldownMax = attackCooldown;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public float getAttackCooldown() {
        return attackCooldown;
    }

    public CrossbowState getCrossbowState() {
        return crossbowState;
    }

    public void setToMaxAttackCooldown() {
        this.attackCooldown = this.attackCooldownMax;
    }

    public boolean canUse() {
        return this.isValidTarget() && this.isHoldingCrossbow();
    }

    private boolean isHoldingCrossbow() {
        return this.mob.isHolding(is -> is.getItem() instanceof CrossbowItem);
    }

    public boolean canContinueToUse() {
        if (!isValidTarget() || !this.isHoldingCrossbow())
            return false;
        if (!this.canUse() && this.mob.getNavigation().isDone())
            return false;

        return true;
    }

    private boolean isValidTarget() {
        return (this.mob.getTarget() != null && this.mob.getTarget().isAlive()) ||
                getBuildingTarget() != null ||
                getGroundTarget() != null;
    }

    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.mob.setTarget(null);
        this.seeTime = 0;
        this.attackCooldown = attackCooldownMax;
        windupTime = random.nextInt(0,6);
    }

    private BuildingPlacement getBuildingTarget() {
        if (this.mob instanceof PillagerUnit pUnit &&
                pUnit.getAttackBuildingGoal() instanceof RangedAttackBuildingGoal<?> rabg) {
            return rabg.getBuildingTarget();
        }
        return null;
    }

    private BlockPos getGroundTarget() {
        if (this.mob instanceof PillagerUnit pUnit &&
                pUnit.getRangedAttackGroundGoal() != null) {
            return pUnit.getRangedAttackGroundGoal().getGroundTarget();
        }
        return null;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tickChargeCrossbow() {
        ItemStack itemstack = this.mob.getItemBySlot(EquipmentSlot.MAINHAND);
        if (this.crossbowState == UNCHARGED) {
            int ticks = CrossbowItem.getChargeDuration(itemstack);
            this.mob.addEffect(new MobEffectInstance(MobEffectRegistrar.MINOR_MOVEMENT_SLOWDOWN.get(), ticks, 3, true, false));
            this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof CrossbowItem));
            this.crossbowState = CHARGING;
            this.mob.setChargingCrossbow(true);
        }
        else if (this.crossbowState == CHARGING) {
            int i = this.mob.getTicksUsingItem();
            if (i >= CrossbowItem.getChargeDuration(itemstack) + windupTime) {
                this.mob.releaseUsingItem();
                this.crossbowState = CHARGED;
                this.attackCooldown = attackCooldownMax;
                this.mob.setChargingCrossbow(false);
                windupTime = random.nextInt(0,6);
            }
        }
    }

    public void tick() {
        LivingEntity target = this.mob.getTarget();
        BuildingPlacement buildTarget = getBuildingTarget();
        BlockPos groundTarget = buildTarget != null ? buildTarget.centrePos : getGroundTarget();

        if ((target != null && target.isAlive()) || groundTarget != null) {

            BuildingPlacement garrPlacement = GarrisonableBuildingAddon.getGarrison((Unit) this.mob);
            BuildingPlacement targetGarrPlacement = null;
            if (target instanceof Unit unit)
                targetGarrPlacement = GarrisonableBuildingAddon.getGarrison(unit);

            GarrisonableBuildingAddon garr = garrPlacement != null ? garrPlacement.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class) : null;
            GarrisonableBuildingAddon targetGarr = targetGarrPlacement != null ? targetGarrPlacement.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class) : null;
            boolean isGarrisoned = garr != null;
            boolean isTargetGarrisoned = targetGarr != null;

            boolean canSeeTarget = true;
            if (target != null)
                canSeeTarget = this.mob.getSensing().hasLineOfSight(target) || isGarrisoned || isTargetGarrisoned;
            boolean flag = this.seeTime > 0;

            if (canSeeTarget != flag) {
                this.seeTime = 0;
            }
            if (canSeeTarget) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }
            double distToTarget;
            if (target != null)
                distToTarget = this.mob.distanceTo(target);
            else
                distToTarget = Math.sqrt(this.mob.distanceToSqr(groundTarget.getX(), groundTarget.getY(), groundTarget.getZ()));

            float attackRange = ((AttackerUnit) this.mob).getAttackRange();

            if (isGarrisoned) {
                attackRange = garr.getAttackRange();
                if (target instanceof GhastUnit)
                    attackRange += GARRISON_BONUS_RANGE_TO_GHASTS;
            }
            else if (isTargetGarrisoned)
                attackRange += targetGarr.getExternalAttackRangeBonus();
            else if (target instanceof GhastUnit ghastUnit)
                attackRange += ghastUnit.getAttackerRangeBonus(this.mob);

            // dont consider garrison range here so the unit still moves towards the edge of the building
            if (!this.mob.isPassenger()) {
                if ((distToTarget > attackRange || !canSeeTarget) &&
                        !((Unit) this.mob).getHoldPosition()) {
                    if (target != null)
                        this.mob.getNavigation().moveTo(target, 1.0f);
                    else
                        this.mob.getNavigation().moveTo(groundTarget.getX(), groundTarget.getY(), groundTarget.getZ(), 1.0f);
                } else {
                    this.mob.getNavigation().stop();
                }
            }

            if (target != null)
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            else
                this.mob.getLookControl().setLookAt(groundTarget.getX(), groundTarget.getY(), groundTarget.getZ(), 30.0F, 30.0F);

            if (this.crossbowState == CHARGED) {
                --this.attackCooldown;
                if (this.attackCooldown <= 0) {
                    this.crossbowState = READY_TO_ATTACK;
                }
            } else if (this.crossbowState == READY_TO_ATTACK && canSeeTarget && distToTarget < attackRange) {
                performAttack();
            }
        }
    }

    public void performAttack() {
        this.mob.performCrossbowAttack(this.mob, 1.6F);
        ItemStack itemstack1 = this.mob.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof CrossbowItem));
        CrossbowItem.setCharged(itemstack1, false);
        this.crossbowState = UNCHARGED;
    }

    private boolean canRun() {
        return this.crossbowState == UNCHARGED;
    }
}
