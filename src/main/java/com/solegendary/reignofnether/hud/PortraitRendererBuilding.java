package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.monsters.SculkCatalyst;
import com.solegendary.reignofnether.building.buildings.placements.SculkCatalystPlacement;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.healthbars.HealthBarClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.LanguageUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.joml.Quaternionf;

// Renders a Building's portrait including an animated block, name, healthbar, list of stats and UI frames

public class PortraitRendererBuilding {
    public int frameWidth = 60;
    public int frameHeight = 60;
    public int xBlock = 31;
    public int yBlock = 25;

    public PortraitRendererBuilding() {
    }

    // Render the portrait including:
    // - background frame
    // - block model representing the building
    // - healthbar
    // - building name
    // Must be called from DrawScreenEvent
    public RectZone render(GuiGraphics guiGraphics, int x, int y, BuildingPlacement building) {
        Relationship rs = BuildingClientEvents.getPlayerToBuildingRelationship(building);

        String name;
        if (building.getBuilding() instanceof CustomBuilding customBuilding) {
            name = customBuilding.name;
        } else {
            ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(building.getBuilding());
            name = LanguageUtil.getTranslation("buildings." + (building.getFaction() != null && building.getFaction() != Faction.NONE ? building.getFaction().name().toLowerCase() : "neutral") + "." + key.getNamespace() + "." + key.getPath());
        }

        if (building.getUpgradeLevel() > 0)
            name = building.getUpgradedName();

        if (!building.isBuilt)
            name += " (" + (int) (building.getBlocksPlacedPercent() * 100) + "%)";

        if (rs != Relationship.OWNED && !building.ownerName.isBlank())
            name += " (" + building.ownerName + ")";

        if (building instanceof SculkCatalystPlacement sc && building.isBuilt)
            name += " (" + sc.getNightRange() + "/" + SculkCatalyst.nightRangeMax + " range)";

        // draw name
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                name,
                x+4,y-9,
                0xFFFFFFFF
        );
        int bgCol = PlayerColors.getPlayerPortraitDisplayColorHex(building.ownerName);
        MyRenderer.renderFrameWithBg(guiGraphics, x, y,
                frameWidth,
                frameHeight,
                bgCol);

        drawBlockOnScreen(x, y, building.getBuilding().portraitBlock, 5.5f);

        // draw health bar and write min/max hp
        HealthBarClientEvents.renderForBuilding(guiGraphics.pose(), building,
                x+(frameWidth/2f), y+frameHeight-15,
                frameWidth-9, HealthBarClientEvents.RenderMode.GUI_PORTRAIT);

        guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                building.getHealth() + "/" + building.getMaxHealth(),
                x+(frameWidth/2), y+frameHeight-13,
                0xFFFFFFFF
        );

        return RectZone.getZoneByLW(x, y, frameWidth, frameHeight);
    }

    public void drawBlockOnScreen(int x, int y, Block block, float blockScale) {
        ItemStack item = new ItemStack(block);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        PoseStack poseStack = RenderSystem.getModelViewStack();
        poseStack.pushPose();
        poseStack.translate(x+xBlock, y+yBlock, 100.0F);
        poseStack.scale(blockScale, -blockScale, blockScale);
        RenderSystem.applyModelViewMatrix();

        float angle = (System.currentTimeMillis() / 100) % 360;
        Quaternionf quaternion = Axis.XP.rotationDegrees(25);
        Quaternionf quaternion2 = Axis.YP.rotationDegrees(angle);
        quaternion.mul(quaternion2);
        PoseStack blockPoseStack = new PoseStack();
        blockPoseStack.pushPose();
        blockPoseStack.mulPose(quaternion);
        blockPoseStack.scale(8, 8, 8);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        Minecraft.getInstance().getItemRenderer().renderStatic(
                item, ItemDisplayContext.FIXED,
                15728880, OverlayTexture.NO_OVERLAY,
                blockPoseStack, bufferSource, null, 0);
        bufferSource.endBatch();

        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }
}
