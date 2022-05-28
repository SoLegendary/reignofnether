package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.solegendary.reignofnether.healthbars.HealthBarClientEvents;
import com.solegendary.reignofnether.units.Unit;
import com.solegendary.reignofnether.util.MyMath;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

class PortraitRenderer<T extends LivingEntity, M extends EntityModel<T>, R extends LivingEntityRenderer<T, M>> {
    public R renderer;
    public Model model;

    public int headSize = 46;
    public int frameWidth = 60;
    public int frameHeight = 60;
    public int headOffsetX = 31;
    public int headOffsetY = 105; // creepers should be 17 lower

    // change these randomly every few seconds to make the head look around
    private int lookX = 0;
    private int lookY = 0;
    private int lastLookTargetX = 0;
    private int lastLookTargetY = 0;
    private int lookTargetX = 0;
    private int lookTargetY = 0;
    private int ticksLeft = 0;
    private final int ticksLeftMin = 60;
    private final int ticksLeftMax = 120;
    private final int lookRangeX = 100;
    private final int lookRangeY = 40;



    public PortraitRenderer(R renderer) {
        this.renderer = renderer;
    }


    public void randomiseAnimation(Boolean randomisePos) {
        if (randomisePos) {
            lookX = MyMath.randRangeInt(-lookRangeX, lookRangeX);
            lookY = MyMath.randRangeInt(-lookRangeY, lookRangeY);
        }
        ticksLeft = MyMath.randRangeInt(ticksLeftMin, ticksLeftMax);

        lastLookTargetX = lookTargetX;
        lastLookTargetY = lookTargetY;

        while (Math.abs(lookTargetX - lookX) < lookRangeX / 2)
            lookTargetX = MyMath.randRangeInt(-lookRangeX, lookRangeX);
        while (Math.abs(lookTargetY - lookY) < lookRangeY / 2)
            lookTargetY = MyMath.randRangeInt(-lookRangeY, lookRangeY);
    }

    public void tickAnimation() {
        ticksLeft -= 1;
        if (ticksLeft <= 0)
            this.randomiseAnimation(false);

        int lookSpeedX = Math.abs(lastLookTargetX - lookX) / 20;
        int lookSpeedY = Math.abs(lastLookTargetY - lookY) / 20;

        if (lookX < lookTargetX)
            lookX += lookSpeedX;
        if (lookX > lookTargetX)
            lookX -= lookSpeedX;
        if (lookY < lookTargetY)
            lookY += lookSpeedY;
        if (lookY > lookTargetY)
            lookY -= lookSpeedY;

        if (Math.abs(lookTargetX - lookX) < lookSpeedX)
            lookX = lookTargetX;
        if (Math.abs(lookTargetY - lookY) < lookSpeedY)
            lookY = lookTargetY;
    }

    // includes:
    // - background frame
    // - moving head
    // - healthbar
    // - unit name
    public void renderWithFrame(PoseStack poseStack, int x, int y, LivingEntity entity) {
        MyRenderer.renderFrameWithBg(poseStack, x, y,
                frameWidth,
                frameHeight,
                0xA0000000);

        int drawX = x + getHeadOffsetX(this.model);
        int drawY = y + getHeadOffsetY(this.model);

        // TODO: hiding layers causes crash when unit dies while selected (also nonHeadModelVisibiliy sometimes isn't reset?)
        // hide all model parts except the head
        setNonHeadModelVisibility(this.model, false);
        List<RenderLayer<T, M>> layers = renderer.layers;
        renderer.layers = List.of();
        drawEntityOnScreen(poseStack, entity, drawX, drawY, headSize);
        renderer.layers = layers;
        setNonHeadModelVisibility(this.model, true);

        // draw health bar and write min/max hp
        HealthBarClientEvents.render(poseStack, entity,
                x+(frameWidth/2f), y+frameHeight-15,
                frameWidth-9, HealthBarClientEvents.HeightMode.ON_SCREEN_PORTRAIT);

        GuiComponent.drawCenteredString(
                poseStack, Minecraft.getInstance().font,
                (int) entity.getHealth() + "/" + (int) entity.getMaxHealth(),
                x+(frameWidth/2), y+frameHeight-13,
                0xFFFFFFFF
        );
    }

    private int getHeadOffsetX(Model model) {
        if (model != null)
            return headOffsetX;
        return 0;
    }
    private int getHeadOffsetY(Model model) {
        if (model instanceof HumanoidModel)
            return headOffsetY;
        if (model instanceof CreeperModel)
            return headOffsetY - 17;
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

    private void drawEntityOnScreen(PoseStack poseStack, LivingEntity entity, int x, int y, int size) {

        float f = (float) Math.atan((double) (-lookX / 40F));
        float g = (float) Math.atan((double) (-lookY / 40F));
        PoseStack poseStackModel = RenderSystem.getModelViewStack();
        poseStackModel.pushPose();
        poseStackModel.translate((double) x, (double) y, 1050.0D);
        poseStackModel.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 1000.0D);
        poseStack.scale((float) size, (float) size, (float) size);
        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternion2 = Vector3f.XP.rotationDegrees(g * 20.0F);
        quaternion.mul(quaternion2);
        poseStack.mulPose(quaternion);
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
            entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, immediate,
                    15728880);
            immediate.endBatch();
        });
        entityrenderdispatcher.setRenderShadow(true);
        entity.yBodyRot = h;
        entity.setYRot(i);
        entity.setXRot(j);
        entity.yHeadRotO = k;
        entity.yHeadRot = l;
        poseStackModel.popPose();
        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }
}
