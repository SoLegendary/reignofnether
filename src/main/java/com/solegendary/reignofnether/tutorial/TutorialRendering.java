package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.systems.RenderSystem;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

public class TutorialRendering {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final ResourceLocation TEXTURE_ARROW_RIGHT = ResourceLocation.fromNamespaceAndPath(
        "reignofnether",
        "textures/hud/tutorial_arrow_right.png"
    );
    private static final ResourceLocation TEXTURE_ARROW_LEFT = ResourceLocation.fromNamespaceAndPath(
        "reignofnether",
        "textures/hud/tutorial_arrow_left.png"
    );
    private static final ResourceLocation TEXTURE_ARROW_UP = ResourceLocation.fromNamespaceAndPath(
        "reignofnether",
        "textures/hud/tutorial_arrow_up.png"
    );
    private static final ResourceLocation TEXTURE_ARROW_DOWN = ResourceLocation.fromNamespaceAndPath(
        "reignofnether",
        "textures/hud/tutorial_arrow_down.png"
    );

    private static String buttonNameToHighlight = "";

    static int xOffset2 = 0;
    static int yOffset2 = 0;

    public static void setButtonName(String name) {
        if (name != null) {
            buttonNameToHighlight = I18n.get(name);
        }
    }

    public static void clearButtonName() {
        buttonNameToHighlight = "";
    }

    public static void highlightNextButton(GuiGraphics guiGraphics, ArrayList<Button> buttons) {
        if (!TutorialClientEvents.isEnabled() || buttonNameToHighlight.isEmpty()) {
            return;
        }

        TutorialStage stage = TutorialClientEvents.getStage();

        Button activeButton = null;
        for (Button button : buttons) {
            if (button.name.equals(buttonNameToHighlight) && !button.isHidden.get() && button.isEnabled.get()
                && !button.isSelected.get() && (
                button.hotkey != null || button.name.equals(TutorialClientEvents.helpButton.name)
                    || TutorialClientEvents.getStage() == TutorialStage.PLACE_WORKERS_B
            )) {
                activeButton = button;
                break;
            }
        }
        if (activeButton == null) {
            return;
        }

        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            guiGraphics.fill(
                // x1,y1, x2,y2,
                activeButton.x,
                activeButton.y,
                activeButton.x + Button.DEFAULT_ICON_FRAME_SIZE,
                activeButton.y + Button.DEFAULT_ICON_FRAME_SIZE,
                0x32FFFFFF
            ); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255
        }
        pointAtWithArrow(guiGraphics, activeButton.x, activeButton.y, true);
    }

    public static void pointAtWithArrow(GuiGraphics guiGraphics, int x, int y, boolean vertical) {
        if (MC.screen == null) {
            return;
        }

        int xOffset = 0;
        int yOffset = 0;

        ResourceLocation texture;
        if (vertical && y < MC.screen.height / 2) {
            xOffset = -5;
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_UP);
            yOffset = (int) MiscUtil.getOscillatingFloat(20, 36);
            texture = TEXTURE_ARROW_UP;
        } else if (vertical) {
            xOffset = -5;
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_DOWN);
            yOffset = (int) MiscUtil.getOscillatingFloat(-46, -30, 500);
            texture = TEXTURE_ARROW_DOWN;
        } else if (x < MC.screen.width / 2) {
            yOffset = -5;
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_LEFT);
            xOffset = (int) MiscUtil.getOscillatingFloat(20, 36);
            texture = TEXTURE_ARROW_LEFT;
        } else {
            yOffset = -5;
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_RIGHT);
            xOffset = (int) MiscUtil.getOscillatingFloat(-46, -30, 500);
            texture = TEXTURE_ARROW_RIGHT;
        }

        guiGraphics.blit(texture, x + xOffset + xOffset2, y + yOffset + yOffset2, 32, 32, 32, 32, 32, 32, 32);
    }
}
