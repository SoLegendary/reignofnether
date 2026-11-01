package com.solegendary.reignofnether.tutorial;

import com.solegendary.reignofnether.hud.layout.HudGeometry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

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

    /** Rings the button the current stage wants clicked, at the rect it was drawn at this frame (HUD units). */
    public static void highlightNextButton(GuiGraphics guiGraphics, List<HudFrame.Placed> placed) {
        if (!TutorialClientEvents.isEnabled() || buttonNameToHighlight.isEmpty()) {
            return;
        }

        Rect r = null;
        for (HudFrame.Placed p : placed) {
            Button button = p.button();
            if (button.name.equals(buttonNameToHighlight) && !button.isHidden.get() && button.isEnabled.get()
                && !button.isSelected.get() && (
                button.hotkey != null || button.name.equals(TutorialClientEvents.helpButton.name)
                    || TutorialClientEvents.getStage() == TutorialStage.PLACE_WORKERS_B
            )) {
                r = p.rect();
                break;
            }
        }
        if (r == null) {
            return;
        }

        // pulsing gold ring hugging the cell, with a faint halo outside it
        float pulse = MiscUtil.getOscillatingFloat(0.45, 1);
        Theme.outline(guiGraphics, r.x(), r.y(), r.w(), r.h(), 2, Theme.withAlpha(Theme.GOLD, pulse));
        Theme.outline(guiGraphics, r.x() - 2, r.y() - 2, r.w() + 4, r.h() + 4, 1, Theme.withAlpha(Theme.GOLD, pulse * 0.35f));
    }

    public static void pointAtWithArrow(GuiGraphics guiGraphics, int x, int y, boolean vertical) {
        if (MC.screen == null) {
            return;
        }

        int xOffset = 0;
        int yOffset = 0;

        ResourceLocation texture;
        if (vertical && y < HudGeometry.layout().height() / 2) {
            xOffset = -5;
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_UP);
            yOffset = (int) MiscUtil.getOscillatingFloat(20, 36);
            texture = TEXTURE_ARROW_UP;
        } else if (vertical) {
            xOffset = -5;
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_DOWN);
            yOffset = (int) MiscUtil.getOscillatingFloat(-46, -30, 500);
            texture = TEXTURE_ARROW_DOWN;
        } else if (x < HudGeometry.layout().width() / 2) {
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
