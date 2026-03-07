package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SlimeUnit;
import com.solegendary.reignofnether.unit.units.piglins.MarauderUnit;
import com.solegendary.reignofnether.unit.units.piglins.PiglinMerchantUnit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

// based on MeleeAttackGoal
public abstract class AbstractMeleeAttackUnitGoal extends Goal {
    protected final Mob mob;
    private final boolean followingTargetEvenIfNotSeen;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    protected int ticksUntilNextPathRecalculation;
    protected final int tickPathRecalcMax = 5;
    protected int ticksUntilNextAttack;
    private long lastCanUseCheck;

    public AbstractMeleeAttackUnitGoal(Mob mob, boolean followingTargetEvenIfNotSeen) {
        this.mob = mob;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public void tickAttackCooldown() {
        if (this.ticksUntilNextAttack > ((AttackerUnit) this.mob).getAttackCooldown())
            this.ticksUntilNextAttack = getAttackInterval();
        if (ticksUntilNextAttack > 0) { // tick down even when not targeting anything
            this.ticksUntilNextAttack -= 1;
        }
    }

    public boolean canUse() {
        long i = this.mob.level().getGameTime();
        if (i - this.lastCanUseCheck < 20L) {
            return false;
        } else {
            this.lastCanUseCheck = i;
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity == null) {
                return false;
            } else if (!livingentity.isAlive()) {
                return false;
            } else {
                this.path = this.mob.getNavigation().createPath(livingentity, 0);
                if (this.path != null) {
                    return true;
                } else {
                    return this.getAttackReachSqr(livingentity) >= this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
                }
            }
        }
    }

    public boolean canContinueToUse() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity == null) {
            return false;
        } else if (!livingentity.isAlive()) {
            return false;
        } else if (!this.followingTargetEvenIfNotSeen) {
            return !this.mob.getNavigation().isDone();
        } else if (!this.mob.isWithinRestriction(livingentity.blockPosition())) {
            return false;
        } else {
            boolean canContinue = !(livingentity instanceof Player) || !livingentity.isSpectator() && !((Player)livingentity).isCreative();
            if (canContinue) {
                this.path = this.mob.getNavigation().createPath(livingentity, 0);
                this.mob.getNavigation().moveTo(this.path,  Unit.getSpeedModifier((Unit) this.mob));
            }
            return canContinue;
        }
    }

    public void start() {
        if (!((Unit) this.mob).getHoldPosition())
            this.mob.getNavigation().moveTo(this.path,  Unit.getSpeedModifier((Unit) this.mob));
        this.mob.setAggressive(true);
    }

    public void stop() {
        LivingEntity livingentity = this.mob.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
            this.mob.setTarget(null);
        }
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());

            if (distSqr < this.getAttackReachSqr(target))
                this.mob.getNavigation().stop();
            else if (!((Unit) this.mob).getHoldPosition()) {
                if (ticksUntilNextPathRecalculation <= 0) {
                    Path path = mob.getNavigation().createPath(target.getX(), target.getY(), target.getZ(), 0);
                    this.mob.getNavigation().moveTo(path, Unit.getSpeedModifier((Unit) this.mob));
                    if (distSqr < 16)
                        ticksUntilNextPathRecalculation = tickPathRecalcMax;
                    else if (distSqr < 64)
                        ticksUntilNextPathRecalculation = tickPathRecalcMax * 2;
                    else
                        ticksUntilNextPathRecalculation = tickPathRecalcMax * 4;
                } else {
                    ticksUntilNextPathRecalculation -= 1;
                }
            }
            this.checkAndPerformAttack(target, distSqr);
        }
    }

    public void checkAndPerformAttackIgnoreDist(LivingEntity target) {
        checkAndPerformAttack(target, 0);
    }

    protected void checkAndPerformAttack(LivingEntity target, double distSqr) {
        double d = this.getAttackReachSqr(target);
        if (distSqr <= d && this.ticksUntilNextAttack <= 0) {
            this.ticksUntilNextAttack = this.adjustedTickDelay(getAttackInterval());
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(target);
        }
    }

    protected int getTicksUntilNextAttack() {
        return this.ticksUntilNextAttack;
    }

    protected int getAttackInterval() {
        return this.adjustedTickDelay((int) ((AttackerUnit) this.mob).getAttackCooldown());
    }

    protected double getAttackReachSqr(LivingEntity target) {
        float width = mob.getBbWidth();
        if (mob instanceof AttackerUnit attackerUnit)
            width += attackerUnit.getBonusMeleeRange();
        float targetWidth = target.getBbWidth();
        if (target instanceof Unit unit) {
            targetWidth += unit.getBonusMeleeRangeForAttackers();
        }
        return width * 2.0F * width * 2.0F + targetWidth;
    }
}
