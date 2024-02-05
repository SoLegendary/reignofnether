package com.solegendary.reignofnether.mixin.fogofwar;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// brightness shading for blocks excluding liquids and flat flace blocks (like tall grass)

@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererMixin {

    @Inject(
            method = "vertex",
            at = @At("HEAD"),
            cancellable = true,
            remap=false
    )
    public void vertex(VertexConsumer pConsumer, double pX, double pY, double pZ, float pRed, float pGreen, float pBlue,
                       float alpha, float pU, float pV, int pPackedLight, CallbackInfo ci) {
        if (!FogOfWarClientEvents.isEnabled())
            return;

        ci.cancel();

        float br = FogOfWarClientEvents.getPosBrightness(new BlockPos(pX, pY, pZ));

        pConsumer.vertex(pX, pY, pZ)
                .color(pRed * br, pGreen * br, pBlue * br, alpha)
                .uv(pU, pV)
                .uv2(pPackedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
