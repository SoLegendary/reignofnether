package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GhastUnitMoveControl extends MoveControl {
    private final GhastUnit ghast;
    private int floatDuration;

    public GhastUnitMoveControl(GhastUnit pGhast) {
        super(pGhast);
        this.ghast = pGhast;
    }

    public void tick() {
        if (this.operation == Operation.MOVE_TO) {
            if (this.floatDuration-- <= 0) {
                this.floatDuration += this.ghast.getRandom().nextInt(5) + 2;
                Vec3 $$0 = new Vec3(this.wantedX - this.ghast.getX(), this.wantedY - this.ghast.getY(), this.wantedZ - this.ghast.getZ());
                double $$1 = $$0.length();
                $$0 = $$0.normalize();
                this.ghast.setDeltaMovement(this.ghast.getDeltaMovement().add($$0.scale(0.1)));
            }
        }
    }
}