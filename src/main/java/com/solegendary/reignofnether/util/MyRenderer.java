package com.solegendary.reignofnether.util;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public class MyRenderer {

    private static final Minecraft MC = Minecraft.getInstance();

    public static void drawBlockOutline(PoseStack matrixStack, BlockPos blockpos, float a) {
        AABB aabb = new AABB(blockpos).move(0,0.01,0);
        drawLineBox(matrixStack, aabb, 1.0f,1.0f,1.0f, a);
    }

    public static void drawEntityOutline(PoseStack matrixStack, Entity entity, float a) {
        drawLineBox(matrixStack, entity.getBoundingBox(), 1.0f,1.0f,1.0f, a);
    }

    public static void drawEntityOutline(PoseStack matrixStack, Entity entity, float r, float g, float b, float a) {
        drawLineBox(matrixStack, entity.getBoundingBox(), r, g, b, a);
    }

    // like drawEntityOutline but only the bottom square
    public static void drawEntityOutlineBottom(PoseStack matrixStack, Entity entity, float r, float g, float b, float a) {
        AABB aabb = entity.getBoundingBox();
        aabb = aabb.setMaxY(aabb.minY);
        drawLineBox(matrixStack, aabb, r, g, b, a);
    }

    public static void drawLineBox(PoseStack matrixStack, AABB aabb, float r, float g, float b, float a) {
        Entity camEntity = MC.getCameraEntity();
        double d0 = camEntity.getX();
        double d1 = camEntity.getY() + camEntity.getEyeHeight();
        double d2 = camEntity.getZ();

        RenderSystem.depthMask(false); // disable showing lines through blocks
        VertexConsumer vertexConsumer = MC.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        matrixStack.pushPose();
        matrixStack.translate(-d0, -d1, -d2); // because we start at 0,0,0 relative to camera
        LevelRenderer.renderLineBox(matrixStack, vertexConsumer, aabb, r, g, b, a);
        matrixStack.popPose();
    }

    public static void renderFrameWithBg(PoseStack poseStack, int x, int y, int width, int height, int bg) {
        // draw icon frame with dark transparent bg
        GuiComponent.fill(poseStack, // x1,y1, x2,y2,
                x + 2, y + 2,
                x + width - 2,
                y + height - 2,
                bg); //ARGB(hex); note that alpha ranges between ~0-16 in RenderOverlayEvent, not 0-255

        ResourceLocation iconFrameResource = new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/unit_frame_no_bg.png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        GuiComponent.blit(poseStack,
                x, y, 0,
                0,0, // where on texture to start drawing from
                width, height, // dimensions of blit texture
                width, height // size of texture itself (if < dimensions, texture is repeated)
        );
    }

    public static void renderIconFrameWithBg(PoseStack poseStack, int x, int y, int size, int bg) {
        //transparent background
        GuiComponent.fill(poseStack, // x1,y1, x2,y2,
                x, y,
                x + size,
                y + size,
                bg); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255

        // icon frame
        ResourceLocation iconFrameResource = new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        GuiComponent.blit(poseStack,
                x, y, 0,
                0,0, // where on texture to start drawing from
                size, size, // dimensions of blit texture
                size, size // size of texture itself (if < dimensions, texture is repeated)
        );
    }

    public static void renderIcon(PoseStack poseStack, ResourceLocation resourceLocation, int x, int y, int size) {
        RenderSystem.setShaderTexture(0, resourceLocation);
        GuiComponent.blit(poseStack,
                x, y, 0,
                0,0, // where on texture to start drawing from
                size, size, // dimensions of blit texture
                size, size // size of texture itself (if < dimensions, texture is repeated)
        );
    }
}
