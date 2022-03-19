package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.healthbars.HealthBarClientEvents;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Class for creating buttons that consist of an icon inside of a frame which is selectable
 * All functionality that occurs on click/hover/etc. is enforced by HudClientEvents
 */

public class Button {

    int iconSize;
    int iconFrameSize;
    int iconFrameSelectedSize;

    int x; // top left
    int y;

    ResourceLocation iconResource;
    ResourceLocation iconFrameResource;
    ResourceLocation iconFrameSelectedResource;

    LivingEntity entity;

    public boolean selected = false;
    public boolean hovered = false;

    public Button(int x, int y, int iconSize, int iconFrameSize, int iconFrameSelectedSize,
            String iconResourcePath, String iconFrameResourcePath, String iconFrameSelectedResourcePath, LivingEntity entity) {
        this.x = x;
        this.y = y;
        this.iconResource = new ResourceLocation(ReignOfNether.MOD_ID, iconResourcePath);
        this.iconFrameResource = new ResourceLocation(ReignOfNether.MOD_ID, iconFrameResourcePath);
        this.iconFrameSelectedResource = new ResourceLocation(ReignOfNether.MOD_ID, iconFrameSelectedResourcePath);
        this.iconSize = iconSize;
        this.iconFrameSize = iconFrameSize;
        this.iconFrameSelectedSize = iconFrameSelectedSize;
        this.entity = entity; // optional, for selected unit icons
    }

    public void render(PoseStack poseStack) {
        // icon frame and transparent background
        RenderSystem.setShaderTexture(0, iconFrameResource);
        GuiComponent.blit(poseStack,
                x, y, 0,
                0,0, // where on texture to start drawing from
                iconFrameSize, iconFrameSize, // dimensions of blit texture
                iconFrameSize, iconFrameSize // size of texture itself (if < dimensions, texture is repeated)
        );

        // item icon
        RenderSystem.setShaderTexture(0, iconResource);
        GuiComponent.blit(poseStack,
                x + 4, y + 4, 0,
                0,0, // where on texture to start drawing from
                iconSize, iconSize, // dimensions of blit area
                iconSize, iconSize // size of texture (if < dimensions, texture is repeated)
        );
    }

    public void renderHealthBar(PoseStack poseStack) {
        HealthBarClientEvents.render(poseStack, entity,
                x + ((float) iconFrameSize / 2), y - 4,
                iconFrameSize - 1,
                false);
    }

    // callback: on click

    // callback: on hover

    // render tooltip (on hover)

    // render texture (unselected)
    // render texture (selected)

}
