package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Projectile {

    protected AbstractArrowMixin(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Shadow public abstract boolean isNoPhysics();

    // prevent arrows from colliding with the building that a garrisoned unit is inside of
    @Inject(
            method = "isNoPhysics",
            at = @At("HEAD"),
            cancellable = true
    )
    public void isNoPhysics(CallbackInfoReturnable<Boolean> cir) {
        if (this.getOwner() instanceof Unit unit) {
            Building garrison = GarrisonableBuilding.getGarrison(unit);
            if (garrison != null && garrison.isPosInsideBuilding(this.blockPosition()) &&
                this.blockPosition().getY() > garrison.originPos.getY() + 2)
                cir.setReturnValue(true);
        }
    }

    //
    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    public void tick(CallbackInfo ci) {
        if (this.isNoPhysics()) {
            Vec3 vec3 = this.getDeltaMovement();
            double d4 = vec3.horizontalDistance();
            double d6 = vec3.y;
            this.setXRot((float)(Mth.atan2(d6, d4) * 57.2957763671875));
            this.setXRot(lerpRotation(this.xRotO, this.getXRot()));
            this.setYRot(lerpRotation(this.yRotO, this.getYRot()));
        }
    }
}
