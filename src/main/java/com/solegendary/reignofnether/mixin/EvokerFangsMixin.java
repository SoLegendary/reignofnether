package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EvokerFangs.class)
public class EvokerFangsMixin {

    @Shadow public LivingEntity getOwner() { return null; }

    @Inject(
        method = "dealDamageTo",
        at = @At("HEAD"),
        cancellable=true
    )
    private void dealDamageTo(LivingEntity pTarget, CallbackInfo ci) {
        ci.cancel();
        LivingEntity owner = this.getOwner();

        // prevent friendly fire
        if (this.getOwner() instanceof Unit unit &&
            UnitServerEvents.getUnitToEntityRelationship(unit, pTarget) == Relationship.FRIENDLY)
            return;

        if (pTarget.isAlive() && !pTarget.isInvulnerable() && pTarget != this.getOwner())
            pTarget.hurt(DamageSource.MAGIC, 6.0F);
    }

}
