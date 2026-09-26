package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.monsters.SpiderLair;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WebBlock.class)
public abstract class WebBlockMixin {
    @Inject(
            method = "entityInside",
            at = @At("HEAD"),
            cancellable = true
    )
    private void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity, CallbackInfo ci) {
        BuildingPlacement building = BuildingUtils.findBuilding(pLevel.isClientSide, pPos);
        if (building != null && building.getBuilding() instanceof SpiderLair &&
            pEntity instanceof Unit unit && Factions.getFaction(unit).equals(Factions.MONSTERS))
            ci.cancel();
    }
}
