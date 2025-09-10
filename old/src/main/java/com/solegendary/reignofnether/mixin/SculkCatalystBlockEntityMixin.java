package com.solegendary.reignofnether.mixin;


import com.solegendary.reignofnether.building.BuildingUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkCatalystBlockEntity.CatalystListener.class)
public abstract class SculkCatalystBlockEntityMixin {

    @Shadow
    @Final
    SculkSpreader sculkSpreader;


    @Shadow
    protected abstract void bloom(ServerLevel pLevel, BlockPos pPos, BlockState pState, RandomSource pRandom);

    @Shadow
    @Final
    private BlockState blockState;

    @Shadow @Final private PositionSource positionSource;

    @Inject(
            method = "handleGameEvent",
            at = @At("HEAD"),
            cancellable = true
    )
    private void handleGameEvent(ServerLevel pLevel, GameEvent pGameEvent, GameEvent.Context pContext, Vec3 pPos, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();

        if (this.sculkSpreader == null) {
            cir.setReturnValue(false);
            return;
        }
        GameEvent.Context $$2 = pContext;
        if (pGameEvent == GameEvent.ENTITY_DIE) {
            Entity var5 = $$2.sourceEntity();
            if (var5 instanceof LivingEntity) {
                LivingEntity $$3 = (LivingEntity) var5;
                if (BuildingUtils.isWithinRangeOfMaxedCatalyst($$3)) {
                    cir.setReturnValue(false);
                    return;
                }
                if (!$$3.wasExperienceConsumed()) {
                    int $$4 = $$3.getExperienceReward();
                    if ($$3.shouldDropExperience() && $$4 > 0) {
                        this.sculkSpreader.addCursors(BlockPos.containing(pPos.relative(Direction.UP, (double) 0.5F)), $$4);
                        LivingEntity $$5 = $$3.getLastHurtByMob();
                        if ($$5 instanceof ServerPlayer) {
                            ServerPlayer $$6 = (ServerPlayer) $$5;
                            DamageSource $$7 = $$3.getLastDamageSource() == null ? $$3.damageSources().playerAttack($$6) : $$3.getLastDamageSource();
                            CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST.trigger($$6, $$2.sourceEntity(), $$7);
                        }
                    }
                    $$3.skipDropExperience();
                    positionSource.getPosition(pLevel).ifPresent((p_289513_) -> this.bloom(pLevel, BlockPos.containing(p_289513_), this.blockState, pLevel.getRandom()));
                }
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }
}
