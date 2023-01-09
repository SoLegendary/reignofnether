package com.solegendary.reignofnether.mixin.fogofwar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.List;

// brightness shading for blocks excluding liquids and flat flace blocks (like tall grass)

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {

    @Shadow private void calculateShape(BlockAndTintGetter pLevel, BlockState pState, BlockPos pPos, int[] pVertices, Direction pDirection, @Nullable float[] pShape, BitSet pShapeFlags) {}
    @Shadow private void putQuadData(BlockAndTintGetter pLevel, BlockState pState, BlockPos pPos, VertexConsumer pConsumer, PoseStack.Pose pPose, BakedQuad pQuad, float pBrightness0, float pBrightness1, float pBrightness2, float pBrightness3, int pLightmap0, int pLightmap1, int pLightmap2, int pLightmap3, int pPackedOverlay) {}

    @Inject(
        method = "renderModelFaceFlat",
        at = @At("HEAD"),
        cancellable = true
    )
    private void renderModelFaceFlat(BlockAndTintGetter pLevel, BlockState pState, BlockPos pPos, int pPackedLight, int pPackedOverlay, boolean pRepackLight,
                                     PoseStack pPoseStack, VertexConsumer pConsumer, List<BakedQuad> pQuads, BitSet pShapeFlags, CallbackInfo ci) {
        ci.cancel();

        for(BakedQuad bakedquad : pQuads) {
            if (pRepackLight) {
                this.calculateShape(pLevel, pState, pPos, bakedquad.getVertices(), bakedquad.getDirection(), null, pShapeFlags);
                BlockPos blockpos = pShapeFlags.get(0) ? pPos.relative(bakedquad.getDirection()) : pPos;
                pPackedLight = LevelRenderer.getLightColor(pLevel, pState, blockpos);
            }

            float f = pLevel.getShade(bakedquad.getDirection(), bakedquad.isShade()) * FogOfWarClientEvents.getPosBrightness(pPos);
            this.putQuadData(pLevel, pState, pPos, pConsumer, pPoseStack.last(), bakedquad, f, f, f, f, pPackedLight, pPackedLight, pPackedLight, pPackedLight, pPackedOverlay);
        }
    }
}
