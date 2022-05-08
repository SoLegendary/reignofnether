package com.solegendary.reignofnether.fogofwar;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Locale;

public class FogOfWarClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    /*
    @SubscribeEvent
    public static void onBlockActivated(BlockEvent.EntityPlaceEvent evt) {
        System.out.println(evt.getPos());

        BlockState block = MC.level.getBlockState(evt.getPos());

    }


    private static ArrayList<Entity> entities = new ArrayList<>();
    @SubscribeEvent
    public static void onRenderEntity(RenderLivingEvent.Post<? extends LivingEntity, ? extends EntityModel<?>> evt) {

        boolean addEntity = true;
        for (Entity entity : entities) {
            if (entity.getId() == evt.getEntity().getId()) {
                addEntity = false;
                break;
            }
        }
        if (addEntity)
            entities.add(evt.getEntity());
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post evt) {
        if (entities.size() > 0)
            drawEntityOnScreen(evt.getMatrixStack(), 20, 35, 13, -80, -20, (LivingEntity) entities.get(0), 1.0f);
    }


    @SubscribeEvent
    public static void onRenderWorld(RenderLevelLastEvent evt) {

        for (Entity entity : entities) {
            PoseStack matrix = evt.getPoseStack();
            Minecraft MC = Minecraft.getInstance();
            Entity camEntity = MC.getCameraEntity();
            double d0 = camEntity.getX();
            double d1 = camEntity.getY() + camEntity.getEyeHeight();
            double d2 = camEntity.getZ();

            RenderSystem.depthMask(false); // disable showing lines through blocks
            VertexConsumer vertexConsumer = MC.renderBuffers().bufferSource().getBuffer(RenderType.lines());
            matrix.pushPose();
            matrix.translate(-d0, -d1, -d2); // because we start at 0,0,0 relative to camera
            LevelRenderer.renderLineBox(matrix, vertexConsumer, entity.getBoundingBox(), 1.0f, 1.0f, 1.0f, 0.2f);
            matrix.popPose();

            //VertexConsumer vertexConsumer = MC.renderBuffers().bufferSource().getBuffer(RenderType.lines());
            //EntityRenderDispatcher.renderHitbox(evt.getPoseStack(), vertexConsumer, entity, 1.0f);
        }
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

        // TODO: for some reason this snippet causes onRenderWorld() outlines to not be drawn while in spectator mode
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
    }*/
}
