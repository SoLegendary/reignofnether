package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin extends LivingEntity {

    protected ArmorStandMixin(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    // deal damage to the building when the armorstand is damaged
    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hurt(DamageSource pSource, float pAmount, CallbackInfoReturnable<Boolean> cir) {
        if (pSource.is(DamageTypes.MOB_ATTACK) || pSource.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) {
            BuildingPlacement building = BuildingUtils.findBuilding(level().isClientSide(), blockPosition());
            if (building != null) {
                building.destroyRandomBlocks((int) (pAmount / 2));
            }
            cir.cancel();
            cir.setReturnValue(true);
        }
    }
}