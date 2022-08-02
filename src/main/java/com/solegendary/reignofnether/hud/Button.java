package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.healthbars.HealthBarClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Supplier;

/**
 * Class for creating buttons that consist of an icon inside of a frame which is selectable
 * All functionality that occurs on click/hover/etc. is enforced by HudClientEvents
 */

public class Button {

    public String name;
    public int x; // top left
    public int y;
    int iconSize;
    public static int iconFrameSize = 22;
    public static int iconFrameSelectedSize = 24;

    ResourceLocation iconResource;

    public KeyMapping hotkey = null; // for action/ability buttons
    public LivingEntity entity = null; // for selected unit buttons

    /** https://stackoverflow.com/questions/29945627/java-8-lambda-void-argument
     * Supplier       ()    -> x
     * Consumer       x     -> ()
     * Runnable       ()    -> ()
     * Predicate      x     -> boolean
     */
    public Supplier<Boolean> isSelected; // controls selected frame rendering
    public Runnable onUse;

    // TODO: enforce not enabled (and if !enabled and rendered, render dark overlay)
    public boolean enabled = false; // allowed to click and use hotkey?

    Minecraft MC = Minecraft.getInstance();

    public Button(String name, int iconSize,
                  String iconResourcePath, KeyMapping hotkey,
                  Supplier<Boolean> isSelected, Runnable onClick) {
        this.name = name;
        this.iconResource = new ResourceLocation(ReignOfNether.MOD_ID, iconResourcePath);
        this.iconSize = iconSize;
        this.hotkey = hotkey;
        this.isSelected = isSelected;
        this.onUse = onClick;
    }

    public Button(String name, int iconSize,
                  String iconResourcePath, LivingEntity entity,
                  Supplier<Boolean> isSelected, Runnable onClick) {
        this.name = name;
        this.iconResource = new ResourceLocation(ReignOfNether.MOD_ID, iconResourcePath);
        this.iconSize = iconSize;
        this.entity = entity;
        this.isSelected = isSelected;
        this.onUse = onClick;
    }

    public void render(PoseStack poseStack, int x, int y, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;

        MyRenderer.renderIconFrameWithBg(poseStack, x, y, iconFrameSize, 0x64000000);

        // item/unit icon
        MyRenderer.renderIcon(
                poseStack,
                iconResource,
                x+4, y+4,
                iconSize
        );

        // selected frame
        if (isSelected.get() || (hotkey != null && hotkey.isDown()) || (isMouseOver(mouseX, mouseY) && MiscUtil.isLeftClickDown(MC))) {
            ResourceLocation iconFrameSelectedResource = new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_selected.png");
            MyRenderer.renderIcon(
                    poseStack,
                    iconFrameSelectedResource,
                    x-1,y-1,
                    iconFrameSelectedSize
            );
        }

        // hotkey letter
        if (this.hotkey != null) {
            GuiComponent.drawCenteredString(poseStack, MC.font,
                    hotkey.getKey().getDisplayName().getString().toUpperCase(),
                    x + iconSize + 4,
                    y + iconSize - 1,
                    0xFFFFFF);
        }
        checkHover(poseStack, mouseX, mouseY);
    }

    public void renderHealthBar(PoseStack poseStack) {
        HealthBarClientEvents.renderForEntity(poseStack, entity,
                x + ((float) iconFrameSize / 2), y - 5,
                iconFrameSize - 1,
                HealthBarClientEvents.RenderMode.GUI_ICON);
    }

    private boolean isMouseOver(int mouseX, int mouseY) {
        return (mouseX >= x &&
                mouseY >= y &&
                mouseX < x + iconFrameSize &&
                mouseY < y + iconFrameSize
        );
    }

    private void checkHover(PoseStack poseStack, int mouseX, int mouseY) {
        // light up on hover
        if (isMouseOver(mouseX, mouseY)) {
            GuiComponent.fill(poseStack, // x1,y1, x2,y2,
                    x, y,
                    x + iconFrameSize,
                    y + iconFrameSize,
                    0x32FFFFFF); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255
        }
    }

    public void checkClicked(int mouseX, int mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            if (this.entity != null)
                System.out.println("Clicked on button - entity id: " + entity.getId());
            else if (this.hotkey != null)
                System.out.println("Clicked on button - hotkey: " + hotkey.getKey().getDisplayName());

            this.onUse.run();
        }
    }

    public void checkPressed() {
        if (hotkey != null && hotkey.isDown())
            this.onUse.run();
    }
}
