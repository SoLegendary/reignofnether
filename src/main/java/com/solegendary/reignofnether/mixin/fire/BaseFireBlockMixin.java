package com.solegendary.reignofnether.mixin.fire;

import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.WitherSkeletonUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.SoulFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {

    private static final int DAMAGE_DELAY = 20; // higher == damage less often
    private static final int DAMAGE = 3;

    private boolean researchImmune(BlockState state, Level level, Entity entity) {
        if (level.isClientSide())
            return false;
        if (state.getBlock() instanceof SoulFireBlock)
            return false;

        if (entity instanceof WitherSkeletonUnit witherSkeletonUnit)
            return ResearchServerEvents.playerHasResearch(witherSkeletonUnit.getOwnerName(), ProductionItems.RESEARCH_FIRE_RESISTANCE);

        return false;
    }

    @Inject(
            method = "entityInside",
            at = @At("HEAD"),
            cancellable = true
    )
    public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity, CallbackInfo ci) {
        ci.cancel();
        if (!pEntity.fireImmune()) {
            if (pEntity.getRemainingFireTicks() < (7 * 20) - 1)
                pEntity.setRemainingFireTicks((8 * 20) - 1); // prevent damage from being ON fire from happening every tick
            boolean isDamageTick = pEntity.tickCount % DAMAGE_DELAY == 0;
            if (isDamageTick && !researchImmune(pState, pLevel, pEntity)) {
                pEntity.hurt(pEntity.damageSources().inFire(), DAMAGE);
            }
        }
    }
}
