package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ability.abilities.FirewallShot;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.units.piglins.BlazeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SpawnPlacements.class)
public class AnimalMixin {

    @Inject(
            method = "checkSpawnRules",
            at = @At("HEAD"),
            cancellable = true
    )
    private static <T extends Entity> void checkSpawnRules(EntityType<T> pEntityType, ServerLevelAccessor pServerLevel,
                                                           MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom,
                                                           CallbackInfoReturnable<Boolean> cir) {
        List<EntityType<? extends Entity>> entityTypes = List.of(
                EntityType.HORSE,
                EntityType.COW,
                EntityType.PIG,
                EntityType.CHICKEN,
                EntityType.GOAT,
                EntityType.SHEEP,
                EntityType.POLAR_BEAR,
                EntityType.RABBIT,
                EntityType.DONKEY,
                EntityType.MULE,
                EntityType.LLAMA
        );
        if (entityTypes.contains(pEntityType))
            cir.setReturnValue(true);
    }
}
