package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.targeting.EntityTargetIndex;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
public abstract class ServerLevelEntityCallbacksMixin {
    @Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void reignofnether$startTracking(Entity entity, CallbackInfo ci) {
        EntityTargetIndex.startTracking(entity);
    }

    @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void reignofnether$stopTracking(Entity entity, CallbackInfo ci) {
        EntityTargetIndex.stopTracking(entity);
    }

    @Inject(method = "onSectionChange(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void reignofnether$sectionChanged(Entity entity, CallbackInfo ci) {
        EntityTargetIndex.sectionChanged(entity);
    }
}
