package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.worldborder.WorldBorderClientEvents;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// fog tint at BiomeColors so grass/leaves/water stay biome-blended under vanilla and Embeddium
@Mixin(BiomeColors.class)
public abstract class BiomeColorsMixin {

    @Inject(method = "getAverageGrassColor", at = @At("RETURN"), cancellable = true)
    private static void ron_fogTintGrass(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        applyTint(level, pos, cir);
    }

    @Inject(method = "getAverageFoliageColor", at = @At("RETURN"), cancellable = true)
    private static void ron_fogTintFoliage(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        applyTint(level, pos, cir);
    }

    @Inject(method = "getAverageWaterColor", at = @At("RETURN"), cancellable = true)
    private static void ron_fogTintWater(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        applyTint(level, pos, cir);
    }

    private static void applyTint(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (level == null || pos == null) return;
        int tint = 0;
        int original = cir.getReturnValueI();
        if (WorldBorderClientEvents.isOutsideWorldBorder(pos)) {
            tint = WorldBorderClientEvents.OUTSIDE_WORLD_BORDER_TINT;
        }
        else if (FogOfWarClientEvents.isEnabled()) {
            // The average-color sample straddles the block's W/N edges (and Embeddium samples 4 positions per
            // vertex). Test block-level visibility at all four touched corners and tint if any is fogged, so the
            // block-granular fog edge has no bright seam.
            int x = pos.getX();
            int z = pos.getZ();
            boolean allVisible =
                    FogOfWarClientEvents.isBlockVisible(x, z) &&
                            FogOfWarClientEvents.isBlockVisible(x - 1, z) &&
                            FogOfWarClientEvents.isBlockVisible(x, z - 1) &&
                            FogOfWarClientEvents.isBlockVisible(x - 1, z - 1);
            if (allVisible) return;
            tint = FogOfWarClientEvents.FOG_TINT_RGB;
        }
        if (tint == 0) return;
        int r = (((original >> 16) & 0xFF) * ((tint >> 16) & 0xFF)) / 255;
        int g = (((original >> 8)  & 0xFF) * ((tint >> 8)  & 0xFF)) / 255;
        int b = (( original        & 0xFF) * ( tint        & 0xFF)) / 255;
        cir.setReturnValue((r << 16) | (g << 8) | b);
    }
}
