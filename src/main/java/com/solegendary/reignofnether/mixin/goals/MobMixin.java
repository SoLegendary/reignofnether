package com.solegendary.reignofnether.mixin.goals;

import com.solegendary.reignofnether.unit.NonUnitServerEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity {

    protected MobMixin(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
            method = "setTarget",
            at = @At("HEAD"),
            cancellable = true
    )
    public void setTarget(LivingEntity pTarget, CallbackInfo ci) {
        synchronized (NonUnitServerEvents.attackSuppressedNonUnits) {
            if (pTarget != null) {
                for (PathfinderMob attackSuppressedNonUnit : NonUnitServerEvents.attackSuppressedNonUnits) {
                    int id = attackSuppressedNonUnit.getId();
                    if (id == getId()) {
                        ci.cancel();
                        break;
                    }
                }
            }
        }
    }
}
