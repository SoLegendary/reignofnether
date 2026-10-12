package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BeaconBlockEntity.class)
public class BeaconBlockEntityMixin extends BlockEntity {

    public BeaconBlockEntityMixin(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Inject(
            method = "getBeamSections()Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getBeamSections(CallbackInfoReturnable<List<BeaconBlockEntity.BeaconBeamSection>> cir) {
        if (level == null || !level.isClientSide())
            return;

        BeaconPlacement beacon = BuildingUtils.getBeacon(level.isClientSide());

        if (beacon != null && beacon.getUpgradeLevel() > 0 && worldPosition.equals(beacon.beaconPos)) {
            if (beacon.isBeaconActive()) {
                // 1.21.1 changed BeaconBeamSection to hold one packed ARGB int instead of a
                // float[3], so the per-aura colours are written as ARGB literals.
                int colour;

                if (beacon.getAuraEffect() == MobEffects.LUCK)
                    colour = 0xFF6EFF81; // pale green
                else if (beacon.getAuraEffect() == MobEffects.DIG_SPEED)
                    colour = 0xFFFFE866; // pale yellow
                else if (beacon.getAuraEffect() == MobEffects.REGENERATION)
                    colour = 0xFFF05B99; // pink
                else if (beacon.getAuraEffect() == MobEffects.DAMAGE_BOOST)
                    colour = 0xFFF5AA5F; // bronze
                else if (beacon.getAuraEffect() == MobEffects.DAMAGE_RESISTANCE)
                    colour = 0xFFB4B4B4; // silver
                else
                    colour = 0xFFFFFFFF; // white

                BeaconBlockEntity.BeaconBeamSection beam = new BeaconBlockEntity.BeaconBeamSection(colour);
                cir.setReturnValue(List.of(beam));
            } else {
                cir.setReturnValue(List.of());
            }
        }
    }
}
