package com.solegendary.reignofnether.entities.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.entities.ThrowableTntProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ThrowableTntRenderer extends EntityRenderer<ThrowableTntProjectile> {

    public ThrowableTntRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ThrowableTntProjectile entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        BlockState tntBlockState = Blocks.TNT.defaultBlockState();
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        blockRenderer.renderSingleBlock(
                tntBlockState,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrowableTntProjectile entity) {
        // Block entities don't use this, but it must be implemented
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/block/tnt.png");
    }
}
