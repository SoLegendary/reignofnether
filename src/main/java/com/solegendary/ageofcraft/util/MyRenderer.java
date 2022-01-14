package com.solegendary.ageofcraft.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public class MyRenderer {

    private static final Minecraft MC = Minecraft.getInstance();
    public static void drawBlockOutline(PoseStack matrixStack, BlockPos blockpos, float alpha) {
        AABB aabb = new AABB(blockpos).move(0,0.01,0);
        drawOutline(matrixStack, aabb, alpha);
    }

    public static void drawEntityOutline(PoseStack matrixStack, Entity entity, float alpha) {
        drawOutline(matrixStack, entity.getBoundingBox(), alpha);
    }

    public static void drawOutline(PoseStack matrixStack, AABB aabb, float alpha) {
        Entity camEntity = MC.getCameraEntity();
        double d0 = camEntity.getX();
        double d1 = camEntity.getY() + camEntity.getEyeHeight();
        double d2 = camEntity.getZ();

        RenderSystem.depthMask(false); // disable showing lines through blocks
        VertexConsumer vertexConsumer = MC.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        matrixStack.pushPose();
        matrixStack.translate(-d0, -d1, -d2); // because we start at 0,0,0 relative to camera
        LevelRenderer.renderLineBox(matrixStack, vertexConsumer, aabb, 1.0f, 1.0f, 1.0f, alpha);
        matrixStack.popPose();

    }
}
