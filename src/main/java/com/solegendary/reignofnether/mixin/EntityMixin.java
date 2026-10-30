package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.unit.targeting.EntityTargetIndex;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "setPosRaw(DDD)V", at = @At("TAIL"))
    private void reignofnether$updateTargetPosition(double x, double y, double z, CallbackInfo ci) {
        EntityTargetIndex.positionChanged((Entity) (Object) this);
    }

    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("TAIL"))
    private void reignofnether$updateTargetOwner(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        EntityTargetIndex.ownerMayHaveChanged((Entity) (Object) this);
    }

    @Shadow private static double viewScale;
    @Shadow private AABB bb;
    @Shadow public abstract EntityType<?> getType();

    @Inject(
        method = "shouldRenderAtSqrDistance(D)Z",
        at = @At("HEAD"),
        cancellable=true
    )
    private void shouldRenderAtSqrDistance(
            double pDistance, CallbackInfoReturnable<Boolean> cir
    ) {
        if (!OrthoviewClientEvents.isEnabled() || this.getType() != EntityType.ITEM)
            return;

        double d0 = this.bb.getSize();
        if (Double.isNaN(d0)) {
            d0 = 1.0D;
        }
        // make item entities render at 4x normal distance
        d0 *= 64.0D * viewScale * 4;
        cir.setReturnValue(pDistance < d0 * d0);
    }

    // use this mixin if you want a mob to avoid damage and not even register a damage animation
    @Inject(
            method = "isInvulnerableTo",
            at = @At("HEAD"),
            cancellable=true
    )
    private void isInvulnerableTo(DamageSource pSource, CallbackInfoReturnable<Boolean> cir) {
        if (pSource == damageSources().inWall())
            cir.setReturnValue(true);
    }

    @Shadow public int getTicksRequiredToFreeze() { return 140; }
    @Shadow public int getTicksFrozen() { return 0; }

    @Shadow public abstract DamageSources damageSources();
    @Shadow public abstract Component getName();
    @Shadow public abstract void remove(Entity.RemovalReason pReason);
    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract BlockPos getOnPos();
    @Shadow public abstract Level level();


    @Shadow private Level level;

    @Inject(
            method = "getPercentFrozen",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void getPercentFrozen(CallbackInfoReturnable<Float> cir) {
        int i = this.getTicksRequiredToFreeze();
        float percent = (float)Math.min(this.getTicksFrozen(), 140) / (float)i;
        cir.setReturnValue(Math.min(percent, 0.5f));
    }

    @Inject(
            method = "extinguishFire()V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void extinguishFire(CallbackInfo ci) {
        if ((Object)this instanceof LivingEntity le && le.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get())) {
            ci.cancel();
        }
    }

    @Inject(
            method = "playEntityOnFireExtinguishedSound",
            at = @At("HEAD"),
            cancellable = true
    )
    public void playEntityOnFireExtinguishedSound(CallbackInfo ci) {
        if ((Object)this instanceof LivingEntity le && le.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get())) {
            ci.cancel();
        }
    }

    @Inject(
            method = "setRemainingFireTicks",
            at = @At("HEAD"),
            cancellable = true
    )
    public void setRemainingFireTicks(int pRemainingFireTicks, CallbackInfo ci) {
        if (pRemainingFireTicks <= 0 && (Object)this instanceof LivingEntity le &&
            le.hasEffect(MobEffectRegistrar.SOULS_AFLAME.get())) {
            ci.cancel();
        }
    }

    // use this mixin if you want a mob to avoid damage and not even register a damage animation
    @Inject(
            method = "isCurrentlyGlowing",
            at = @At("HEAD"),
            cancellable=true
    )
    private void isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (ron$isHighlightedAnimal()) {
            cir.setReturnValue(true);
        }
        if (level.isClientSide() && OrthoviewClientEvents.isEnabled() &&
                ((Object) this) instanceof ItemEntity ie && ItemUtil.isUnitItem(ie)) {
            cir.setReturnValue(true);
        }
    }

    // glow colour is the only per-entity data that reaches the outline buffer (alpha is always 255), so tag
    // highlighted animals with a near-white marker colour that entity_sobel.fsh uses to draw a thicker outline.
    // Must be kept in sync with isThick() in assets/minecraft/shaders/program/entity_sobel.fsh
    @Inject(
            method = "getTeamColor",
            at = @At("HEAD"),
            cancellable = true
    )
    private void getTeamColor(CallbackInfoReturnable<Integer> cir) {
        if (ron$isHighlightedAnimal()) {
            cir.setReturnValue(0xFFFFFE);
        }
    }

    private boolean ron$isHighlightedAnimal() {
        return level.isClientSide() && MinimapClientEvents.shouldHighlightAnimals() &&
                (Object) this instanceof LivingEntity le && ResourceSources.isHuntableAnimal(le);
    }

    // units run on-death commands with permission level 2, so stop non-ops setting them through spawn egg EntityTag.
    // Units extend many different vanilla mobs with no shared base class, and an interface default can't
    // override Entity's concrete method, so this has to be a mixin
    @Inject(method = "onlyOpCanSetNbt", at = @At("HEAD"), cancellable = true)
    private void onlyOpCanSetNbt(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Unit)
            cir.setReturnValue(true);
    }
}
