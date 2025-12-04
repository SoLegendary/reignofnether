package com.solegendary.reignofnether.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;

public class GarrisonBlockRenderer implements BlockEntityRenderer<GarrisonBlockEntity> {

    public GarrisonBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(GarrisonBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Player player = Minecraft.getInstance().player;
        if ((player != null && !player.isCreative()) || OrthoviewClientEvents.isEnabled()) {
            return;
        }
        BlockState bs = blockEntity.getBlockState();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel bakedmodel = dispatcher.getBlockModel(bs);
        for (RenderType rt : bakedmodel.getRenderTypes(bs, RandomSource.create(0), ModelData.EMPTY)) {
            dispatcher.getModelRenderer().renderModel(poseStack.last(), bufferSource.getBuffer(RenderTypeHelper.getEntityRenderType(rt, false)), bs, bakedmodel,
                    0, 0, 0, packedLight, packedOverlay, ModelData.EMPTY, rt);
        }
    }
}