package com.solegendary.reignofnether.unit.controls;

import com.solegendary.reignofnether.unit.units.monsters.SlimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class SlimeUnitMoveControl extends MoveControl {
    private int jumpDelay;
    private final SlimeUnit slime;
    private float yRot;
    private boolean hasLineOfSight = false;
    private double lastDistToNodeSqr = Integer.MAX_VALUE;

    private static final Random RANDOM = new Random();

    public SlimeUnitMoveControl(SlimeUnit pSlime) {
        super(pSlime);
        this.slime = pSlime;
    }

    private static int getJumpDelay() {
        return 6;
    }
    private float getSoundVolume() {
        return 0.4F * (float)this.slime.getSize();
    }
    private float getSoundPitch() {
        float f = this.slime.isTiny() ? 1.4F : 0.8F;
        return ((RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2F + 1.0F) * f;
    }
    protected SoundEvent getJumpSound() {
        return this.slime.isTiny() ? SoundEvents.SLIME_JUMP_SMALL : SoundEvents.SLIME_JUMP;
    }

    // if we have direct line of sight of the end pos, then dont worry about smart pathfinding (it's slower anyway)
    @Override
    public void tick() {
        if (this.slime.tickCount % 5 == 0)
            updateLineOfSight();

        if (hasLineOfSight) {
            tickDumb();
        } else {
            tickSmart();
        }
    }

    private void updateLineOfSight() {
        LivingEntity targetEntity = this.slime.getTarget();
        BlockPos moveTarget = slime.getMoveGoal().getMoveTarget();
        this.hasLineOfSight = moveTarget != null && this.slime.hasLineOfSight(moveTarget.above().getCenter()) ||
                            (targetEntity != null && this.slime.hasLineOfSight(targetEntity));
    }

    // uses pathnavigation, but not very efficient, sometimes get stuck around corners and misses the target by a block or two
    private void tickSmart() {
        if (this.operation != Operation.MOVE_TO) {
            this.mob.setZza(0.0F);
            this.mob.setSpeed(0.0F);
            return;
        }
        this.operation = Operation.WAIT;

        // if we are travelling away from the next node, we likely overshot it, so time to recalculate
        if (slime.getMoveGoal().getMoveTarget() != null) {
            Vec3 wanted = new Vec3(wantedX, wantedY, wantedZ);
            double distToNextNodeSqr = slime.distanceToSqr(wanted);
            PathNavigation nav = slime.getNavigation();

            if (distToNextNodeSqr > lastDistToNodeSqr && nav.getPath() != null) {
                int intWidth = Math.max(1, (int) slime.getBbWidth());
                for (int x = 0; x < intWidth; x++) {
                    if (!nav.getPath().isDone())
                        nav.getPath().advance();
                }
                nav.tick();
            }
            lastDistToNodeSqr = distToNextNodeSqr;
        }
        double dx = this.wantedX - this.mob.getX();
        double dz = this.wantedZ - this.mob.getZ();

        // look at the target
        float desiredYaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), desiredYaw, mob.onGround() ? 90.0F : 10.0f));
        this.mob.yHeadRot = this.mob.getYRot();
        this.mob.yBodyRot = this.mob.getYRot();

        float moveSpeed = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        this.mob.setSpeed(moveSpeed);

        // Convert desired direction into a small "probe step" for isWalkable().
        // This is basically "would the next step forward be WALKABLE?"
        double horizLen = Math.sqrt(dx * dx + dz * dz);
        float relX = 0.0F;
        float relZ = 0.0F;
        if (horizLen > 1.0e-6) {
            relX = (float)(dx / horizLen);
            relZ = (float)(dz / horizLen);
        }

        if (this.mob.onGround()) {
            // Count down only while we're actually trying to move somewhere
            if (this.jumpDelay-- <= 0  || !this.isWalkable(relX, relZ)) {
                this.jumpDelay = getJumpDelay();
                this.slime.getJumpControl().jump();
                if (this.slime.getSize() > 0) {
                    this.slime.playSound(this.getJumpSound(), getSoundVolume(), getSoundPitch());
                }
            } else {
                // Between hops, don't "slide" like a normal walker.
                this.slime.xxa = 0.0F;
                this.slime.zza = 0.0F;
                this.mob.setSpeed(0.0F);
            }
        } else {
            this.mob.setSpeed(moveSpeed);
        }
    }

    // no pathnavigation, always tries to go in a direct straight line regardless of obstacles but is usually more block-accurate
    private void tickDumb() {
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
        this.mob.yHeadRot = this.mob.getYRot();
        this.mob.yBodyRot = this.mob.getYRot();
        if (this.operation != Operation.MOVE_TO) {
            this.mob.setZza(0.0F);
        } else {
            this.operation = Operation.WAIT;
            if (this.mob.onGround()) {
                this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                if (this.jumpDelay-- <= 0) {
                    this.jumpDelay = getJumpDelay();
                    this.slime.getJumpControl().jump();
                    if (this.slime.getSize() > 0) {
                        this.slime.playSound(this.getJumpSound(), getSoundVolume(), getSoundPitch());
                    }
                } else {
                    this.slime.xxa = 0.0F;
                    this.slime.zza = 0.0F;
                    this.mob.setSpeed(0.0F);
                }
            } else {
                this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            }
        }

        BlockPos bp = this.slime.getNavigation().getTargetPos();
        if (bp != null) {
            double d0 = bp.getX() - this.slime.getX();
            double d2 = bp.getZ() - this.slime.getZ();
            float f = (float)(Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F;
            this.slime.setYRot(this.rotlerp(this.slime.getYRot(), f, 10f));
            this.yRot = this.slime.getYRot();
        } else {
            LivingEntity targetEntity = this.slime.getTarget();
            if (targetEntity != null) {
                this.slime.lookAt(targetEntity, 10.0F, 10.0F);
            }
            this.yRot = this.slime.getYRot();
        }
    }
}
