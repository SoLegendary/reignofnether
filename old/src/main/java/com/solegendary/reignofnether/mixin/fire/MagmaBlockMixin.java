package com.solegendary.reignofnether.mixin.fire;

import com.solegendary.reignofnether.blocks.WalkableMagmaBlock;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.GruntUnit;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MagmaBlock.class)
public abstract class MagmaBlockMixin {

    @Inject(
            method = "stepOn",
            at = @At("HEAD"),
            cancellable = true
    )
    public void stepOn(Level pLevel, BlockPos pPos, BlockState pState, Entity pEntity, CallbackInfo ci) {
        ci.cancel();

        boolean isPiglinFaction = pEntity instanceof Unit unit && unit.getFaction() == Faction.PIGLINS;
        boolean isDamageTick = pEntity.tickCount % WalkableMagmaBlock.DAMAGE_DELAY == 0;

        if (!pEntity.isSteppingCarefully() &&
            pEntity instanceof LivingEntity &&
            !(pEntity instanceof GruntUnit) &&
            !EnchantmentHelper.hasFrostWalker((LivingEntity)pEntity) &&
            !isPiglinFaction && isDamageTick) {
            pEntity.hurt(pEntity.damageSources().hotFloor(), WalkableMagmaBlock.DAMAGE);
        }
    }
}
