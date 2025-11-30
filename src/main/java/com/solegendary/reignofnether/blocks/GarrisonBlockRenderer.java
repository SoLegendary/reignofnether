package com.solegendary.reignofnether.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import java.util.ArrayList;
import java.util.List;

public class GarrisonBlockRenderer implements BlockEntityRenderer<GarrisonBlockEntity> {

    public GarrisonBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(GarrisonBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isCreative() || OrthoviewClientEvents.isEnabled()) {
            return;
        }

        renderModel(blockEntity, poseStack, bufferSource, packedLight);
    }

    private static void renderModel(GarrisonBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        BlockState state = blockEntity.getBlockState();
        BakedModel model = dispatcher.getBlockModel(state);
        ModelBlockRenderer modelRenderer = dispatcher.getModelRenderer();
        ModelData modelData = blockEntity.getModelData();
        if (modelData == null) {
            modelData = ModelData.EMPTY;
        }

        long seed = state.getSeed(blockEntity.getBlockPos());
        ChunkRenderTypeSet renderTypeSet = model.getRenderTypes(state, RandomSource.create(seed), modelData);
        List<RenderType> renderTypes = new ArrayList<>(renderTypeSet.asList());
        if (renderTypes.isEmpty()) {
            renderTypes.add(RenderType.solid());
        }

        poseStack.pushPose();
        for (RenderType renderType : renderTypes) {
            var vertexConsumer = bufferSource.getBuffer(renderType);
            modelRenderer.renderModel(
                    poseStack.last(),
                    vertexConsumer,
                    state,
                    model,
                    1.0F,
                    1.0F,
                    1.0F,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    modelData,
                    renderType
            );
        }
        poseStack.popPose();
    }
}