package com.solegendary.reignofnether.unit.controls;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.monster.Slime;

import java.util.Random;

public class SlimeUnitMoveControl extends MoveControl {
    private float yRot;
    private int jumpDelay;
    private final Slime slime;
    private boolean isAggressive;

    private static final Random RANDOM = new Random();

    public SlimeUnitMoveControl(Slime pSlime) {
        super(pSlime);
        this.slime = pSlime;
        this.yRot = 180.0F * pSlime.getYRot() / 3.1415927F;
    }

    public void setDirection(float pYRot, boolean pAggressive) {
        this.yRot = pYRot;
        this.isAggressive = pAggressive;
    }

    public void setWantedMovement(double pSpeed) {
        this.speedModifier = pSpeed;
        this.operation = Operation.MOVE_TO;
    }

    private static int getJumpDelay() {
        return 20;
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

    public void tick() {
        //if (this.operation != Operation.MOVE_TO &&
        //    !((Unit) this.slime).getMoveGoal().isAtDestination())
        //    this.operation = Operation.MOVE_TO;

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
                    if (this.isAggressive) {
                        this.jumpDelay /= 3;
                    }
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

        if (this.slime instanceof Unit unit) {
            BlockPos bp = ((Mob) unit).getNavigation().getTargetPos();
            if (bp != null) {
                double d0 = bp.getX() - this.slime.getX();
                double d2 = bp.getZ() - this.slime.getZ();
                double d1 = bp.getY() - this.slime.getEyeY();
                //double d3 = Math.sqrt(d0 * d0 + d2 * d2);
                float f = (float)(Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F;
                //loat f1 = (float)(-(Mth.atan2(d1, d3) * 57.2957763671875));
                //this.slime.setXRot(this.rotlerp(this.slime.getXRot(), f1, 10f));
                this.slime.setYRot(this.rotlerp(this.slime.getYRot(), f, 10f));
                setDirection(this.slime.getYRot(), true);
            } else {
                LivingEntity targetEntity = this.slime.getTarget();
                if (targetEntity != null) {
                    this.slime.lookAt(targetEntity, 10.0F, 10.0F);
                }
                setDirection(this.slime.getYRot(), true);
            }
        }
    }
}






