package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

// make wolves respect evasion chance
@Mixin(Wolf.class)
public abstract class WolfMixin {

    protected WolfMixin(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super();
    }

    private static final Random RANDOM = new Random();

    @Inject(
            method = "doHurtTarget",
            at = @At("HEAD"),
            cancellable = true
    )
    public void doHurtTarget(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
        if (pEntity instanceof Unit unit && unit.getEvasionChance() > 0)
            if (RANDOM.nextFloat() < unit.getEvasionChance())
                cir.setReturnValue(false);
    }
}
