package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.joml.Quaternionf;

// Renders a building's spinning portrait block
public class PortraitRendererBuilding {

    /**
     * Draws a slowly spinning block (the building's portrait block) centred on (cx, cy). Positions go
     * on the GUI pose so the block follows the HUD scale; blockScale 5.5 fits a 60 unit box.
     */
    public static void drawBlock(GuiGraphics g, Block block, float cx, float cy, float blockScale) {
        ItemStack item = new ItemStack(block);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 100.0F);
        pose.scale(blockScale * 8, -blockScale * 8, blockScale * 8);
        float angle = (System.currentTimeMillis() / 100) % 360;
        Quaternionf rotation = Axis.XP.rotationDegrees(25);
        rotation.mul(Axis.YP.rotationDegrees(angle));
        pose.mulPose(rotation);

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Minecraft.getInstance().getItemRenderer().renderStatic(
                item, ItemDisplayContext.FIXED,
                15728880, OverlayTexture.NO_OVERLAY,
                pose, bufferSource, null, 0);
        bufferSource.endBatch();
        pose.popPose();
    }
}
