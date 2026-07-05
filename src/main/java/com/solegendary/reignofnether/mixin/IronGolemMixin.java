package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.IronGolemUnit;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

// make golems respect evasion chance
@Mixin(IronGolem.class)
public abstract class IronGolemMixin extends Mob {

    @Shadow
    private int attackAnimationTick;

    private static final Random RANDOM = new Random();

    protected IronGolemMixin(EntityType<? extends Mob> pEntityType, Level pLevel) {
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
                this.attackAnimationTick = 10;
                this.level().broadcastEntityEvent(this, (byte)4);
                this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
            }
        }
    }
}
