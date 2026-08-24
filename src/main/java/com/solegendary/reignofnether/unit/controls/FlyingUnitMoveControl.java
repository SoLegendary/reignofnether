package com.solegendary.reignofnether.unit.controls;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class FlyingUnitMoveControl extends MoveControl {
    private final Mob mob;
    private int floatDuration;

    // 0.05 == 0.25 for movement speed attribute
    private static final float MOVE_SPEED_RATIO = 5;

    public FlyingUnitMoveControl(Mob mob) {
        super(mob);
        this.mob = (Mob) mob;
    }

    public void tick() {
        if (this.operation == Operation.MOVE_TO) {
            if (this.floatDuration-- <= 0) {
                this.floatDuration += this.mob.getRandom().nextInt(5) + 2;
                Vec3 $$0 = new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getY(), this.wantedZ - this.mob.getZ());
                double $$1 = $$0.length();
                $$0 = $$0.normalize();
                AttributeInstance ms = this.mob.getAttribute(Attributes.MOVEMENT_SPEED);
                if (ms != null) {
                    this.mob.setDeltaMovement(this.mob.getDeltaMovement().add($$0.scale(ms.getValue() / MOVE_SPEED_RATIO)));
                }
            }
        }
    }
}