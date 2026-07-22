package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// fog-tint fluids on the vanilla renderer (Embeddium replaces this class)
@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererMixin {

    @Redirect(
            method = "tesselate(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/extensions/common/IClientFluidTypeExtensions;getTintColor(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"
            )
    )
    private int ron_fogTintLiquid(IClientFluidTypeExtensions self, FluidState state, BlockAndTintGetter level, BlockPos pos) {
        int original = self.getTintColor(state, level, pos);
        if (!FogOfWarClientEvents.isEnabled()) return original;
        if (FogOfWarClientEvents.brightChunks.contains(new ChunkPos(pos))) return original;

        int fog = FogOfWarClientEvents.FOG_TINT_RGB;
        int a = (original >>> 24) & 0xFF;
        int r = (((original >> 16) & 0xFF) * ((fog >> 16) & 0xFF)) / 255;
        int g = (((original >> 8)  & 0xFF) * ((fog >> 8)  & 0xFF)) / 255;
        int b = (( original        & 0xFF) * ( fog        & 0xFF)) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
