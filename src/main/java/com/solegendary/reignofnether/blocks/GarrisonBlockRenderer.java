package com.solegendary.reignofnether.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.entity.player.Player;

public class GarrisonBlockRenderer implements BlockEntityRenderer<GarrisonBlockEntity> {

    public GarrisonBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(GarrisonBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Player player = Minecraft.getInstance().player;
        if (player != null && !player.isCreative()) {
            return;
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockEntity.getBlockState(), poseStack, bufferSource, packedLight, packedOverlay);
    }
}