package com.solegendary.reignofnether.util;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
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

    public static void drawEntityOnScreen(PoseStack matrixStack2, int x, int y, int size, float mouseX,
                                          float mouseY, LivingEntity entity, float scale) {

        float f = (float) Math.atan((double) (mouseX / 40.0F));
        float g = (float) Math.atan((double) (mouseY / 40.0F));
        PoseStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushPose();
        matrixStack.translate((double) x * scale, (double) y * scale, 1050.0D * scale);
        matrixStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        matrixStack2.pushPose();
        matrixStack2.translate(0.0D, 0.0D, 1000.0D);
        matrixStack2.scale((float) size, (float) size, (float) size);
        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternion2 = Vector3f.XP.rotationDegrees(g * 20.0F);
        quaternion.mul(quaternion2);
        matrixStack2.mulPose(quaternion);
        float h = entity.yBodyRot; // bodyYaw;
        float i = entity.getYRot(); // getYaw();
        float j = entity.getXRot(); // getPitch();
        float k = entity.yHeadRotO; // prevHeadYaw;
        float l = entity.yHeadRot; // headYaw;
        entity.yBodyRot = 180.0F + f * 20.0F;
        entity.setYRot(180.0F + f * 40.0F);
        entity.setXRot(-g * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher =
                Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion2.conj();
        entityrenderdispatcher.setRenderShadow(false);
        entityrenderdispatcher.overrideCameraOrientation(quaternion2);

        // for some reason this snippet causes drawLineBox to draw lines in completely wrong locations while in spectator mode
        RenderSystem.runAsFancy(() -> {
            MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
            entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrixStack2, immediate,
                    15728880);
            immediate.endBatch();
        });
        entityrenderdispatcher.setRenderShadow(true);
        entity.yBodyRot = h;
        entity.setYRot(i);
        entity.setXRot(j);
        entity.yHeadRotO = k;
        entity.yHeadRot = l;
        matrixStack.popPose();
        matrixStack2.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

    public static void setNonHeadModelVisibility(Model model, boolean visibility) {
        if (model instanceof HumanoidModel) {
            ((HumanoidModel) model).hat.visible = visibility;
            ((HumanoidModel) model).body.visible = visibility;
            ((HumanoidModel) model).rightArm.visible = visibility;
            ((HumanoidModel) model).leftArm.visible = visibility;
            ((HumanoidModel) model).rightLeg.visible = visibility;
            ((HumanoidModel) model).leftLeg.visible = visibility;
        }
        if (model instanceof CreeperModel) {
            ((CreeperModel) model).rightHindLeg.visible = visibility;
            ((CreeperModel) model).leftHindLeg.visible = visibility;
            ((CreeperModel) model).rightFrontLeg.visible = visibility;
            ((CreeperModel) model).leftFrontLeg.visible = visibility;
            ((CreeperModel) model).root.getChild("body").visible = visibility;
        }
    }
}
