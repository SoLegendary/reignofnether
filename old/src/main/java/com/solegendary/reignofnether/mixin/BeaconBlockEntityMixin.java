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
                float[] colour;

                if (beacon.getAuraEffect() == MobEffects.LUCK)
                    colour = new float[] { 110/255f, 255/255f, 129/255f }; // pale green
                else if (beacon.getAuraEffect() == MobEffects.DIG_SPEED)
                    colour = new float[] { 255/255f, 232/255f, 102/255f }; // pale yellow
                else if (beacon.getAuraEffect() == MobEffects.REGENERATION)
                    colour = new float[] { 240/255f, 91/255f, 153/255f }; // pink
                else if (beacon.getAuraEffect() == MobEffects.DAMAGE_BOOST)
                    colour = new float[] { 245/255f, 170/255f, 95/255f }; // bronze
                else if (beacon.getAuraEffect() == MobEffects.DAMAGE_RESISTANCE)
                    colour = new float[] { 180/255f, 180/255f, 180/255f }; // silver
                else
                    colour = new float[] { 1.0f, 1.0f, 1.0f }; // white

                BeaconBlockEntity.BeaconBeamSection beam = new BeaconBlockEntity.BeaconBeamSection(colour);
                cir.setReturnValue(List.of(beam));
            } else {
                cir.setReturnValue(List.of());
            }
        }
    }
}
