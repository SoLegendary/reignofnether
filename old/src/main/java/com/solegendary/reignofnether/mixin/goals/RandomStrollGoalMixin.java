package com.solegendary.reignofnether.mixin.goals;

import com.solegendary.reignofnether.unit.NonUnitServerEvents;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomStrollGoal.class)
public abstract class RandomStrollGoalMixin {

    @Shadow @Final protected PathfinderMob mob;

    @Inject(
            method = "canContinueToUse",
            at = @At("HEAD"),
            cancellable = true
    )
    public void canContinueToUse(CallbackInfoReturnable<Boolean> cir) {
        synchronized (NonUnitServerEvents.moveSuppressedNonUnits) {
            if (NonUnitServerEvents.moveSuppressedNonUnits.contains(mob))
                cir.setReturnValue(false);
        }
    }
}
