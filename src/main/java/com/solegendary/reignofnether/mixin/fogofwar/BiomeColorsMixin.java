package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// fog tint at BiomeColors so grass/leaves/water stay biome-blended under vanilla and Embeddium
@Mixin(BiomeColors.class)
public abstract class BiomeColorsMixin {

    @Inject(method = "getAverageGrassColor", at = @At("RETURN"), cancellable = true)
    private static void ron_fogTintGrass(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        applyFog(level, pos, cir);
    }

    @Inject(method = "getAverageFoliageColor", at = @At("RETURN"), cancellable = true)
    private static void ron_fogTintFoliage(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        applyFog(level, pos, cir);
    }

    @Inject(method = "getAverageWaterColor", at = @At("RETURN"), cancellable = true)
    private static void ron_fogTintWater(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        applyFog(level, pos, cir);
    }

    private static void applyFog(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (level == null || pos == null) return;
        if (!FogOfWarClientEvents.isEnabled()) return;

        // Embeddium samples 4 positions per vertex; some land in the neighbor chunk.
        // Tint if any of the 4 touched chunks is dark to avoid bright seams.
        Set<ChunkPos> bright = FogOfWarClientEvents.brightChunks;
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        int cxW = (pos.getX() - 1) >> 4;
        int czN = (pos.getZ() - 1) >> 4;
        boolean allBright =
                bright.contains(new ChunkPos(cx, cz)) &&
                (cxW == cx || bright.contains(new ChunkPos(cxW, cz))) &&
                (czN == cz || bright.contains(new ChunkPos(cx, czN))) &&
                ((cxW == cx && czN == cz) || bright.contains(new ChunkPos(cxW, czN)));
        if (allBright) return;

        int original = cir.getReturnValueI();
        int fog = FogOfWarClientEvents.FOG_TINT_RGB;
        int r = (((original >> 16) & 0xFF) * ((fog >> 16) & 0xFF)) / 255;
        int g = (((original >> 8)  & 0xFF) * ((fog >> 8)  & 0xFF)) / 255;
        int b = (( original        & 0xFF) * ( fog        & 0xFF)) / 255;
        cir.setReturnValue((r << 16) | (g << 8) | b);
    }
}
