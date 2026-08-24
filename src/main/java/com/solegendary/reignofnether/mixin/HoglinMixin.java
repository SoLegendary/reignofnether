package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

// make hoglins and zoglins respect evasion chance
@Mixin(Hoglin.class)
public abstract class HoglinMixin extends Mob {

    private static final Random RANDOM = new Random();

    @Shadow
    private int attackAnimationRemainingTicks;

    protected HoglinMixin(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
            method = "doHurtTarget",
            at = @At("HEAD"),
            cancellable = true
    )
    public void doHurtTarget(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
        if (pEntity instanceof Unit unit && unit.getEvasionChance() > 0) {
            if (RANDOM.nextFloat() < unit.getEvasionChance()) {
                cir.setReturnValue(false);
                this.attackAnimationRemainingTicks = 10;
                this.level().broadcastEntityEvent(this, (byte)4);
                this.playSound(SoundEvents.HOGLIN_ATTACK, 1.0F, this.getVoicePitch());
            }
        }
    }
}
