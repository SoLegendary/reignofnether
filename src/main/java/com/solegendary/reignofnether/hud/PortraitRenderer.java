package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

class PortraitRenderer<T extends LivingEntity, M extends EntityModel<T>, R extends LivingEntityRenderer<T, M>> {
    public R renderer;
    public Model model;

    public int headSize = 30;
    public int frameSize = 42;
    public int headOffsetX = 22;

    // change these randomly every few seconds to make the
    private int headLookX = 0;
    private int headLookY = 0;
    private int headLookTargetX = 0;
    private int headLookTargetY = 0;

    public PortraitRenderer(R renderer) {
        this.renderer = renderer;
    }

    public void renderHeadOnScreen(PoseStack matrixStack2, int x, int y, LivingEntity entity) {
        // draw icon frame
        ResourceLocation iconFrameResource = new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/unit_frame.png");
        RenderSystem.setShaderTexture(0, iconFrameResource);
        GuiComponent.blit(matrixStack2,
                x,y, 0,
                0,0, // where on texture to start drawing from
                frameSize, frameSize, // dimensions of blit texture
                frameSize, frameSize // size of texture itself (if < dimensions, texture is repeated)
        );

        int drawX = x + headOffsetX;
        int drawY = y + getHeadOffsetY(this.model);

        // hide all model parts except the head
        setNonHeadModelVisibility(this.model, false);
        List<RenderLayer<T, M>> layers = renderer.layers;
        renderer.layers = List.of();
        drawEntityOnScreen(matrixStack2, entity, drawX, drawY, headSize);
        renderer.layers = layers;
        setNonHeadModelVisibility(this.model, true);
    }

    private static int getHeadOffsetY(Model model) {
        if (model instanceof HumanoidModel)
            return 70;
        if (model instanceof CreeperModel)
            return 60;
        return 0;
    }

    private void setNonHeadModelVisibility(Model model, boolean visibility) {
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

    private void drawEntityOnScreen(PoseStack matrixStack2, LivingEntity entity, int x, int y, int size) {

        float f = (float) Math.atan((double) (-headLookX / 40F));
        float g = (float) Math.atan((double) (-headLookY / 40F));
        PoseStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushPose();
        matrixStack.translate((double) x, (double) y, 1050.0D);
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
}
