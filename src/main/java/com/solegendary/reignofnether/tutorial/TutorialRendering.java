package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

public class TutorialRendering {

    private static Minecraft MC = Minecraft.getInstance();

    private static final ResourceLocation TEXTURE_ARROW_RIGHT = new ResourceLocation("reignofnether", "textures/hud/tutorial_arrow_right.png");
    private static final ResourceLocation TEXTURE_ARROW_LEFT = new ResourceLocation("reignofnether", "textures/hud/tutorial_arrow_left.png");
    private static final ResourceLocation TEXTURE_ARROW_UP = new ResourceLocation("reignofnether", "textures/hud/tutorial_arrow_up.png");
    private static final ResourceLocation TEXTURE_ARROW_DOWN = new ResourceLocation("reignofnether", "textures/hud/tutorial_arrow_down.png");

    public static void highlightNextButton(PoseStack poseStack, ArrayList<Button> buttons) {
        if (!TutorialClientEvents.isEnabled())
            return;
        if (MC.screen == null)
            return;

        TutorialStage stage = TutorialClientEvents.getStage();

        Button activeButton = null;
        for (Button button : buttons) {
            if (stage == TutorialStage.INTRO && button.name.equals("Villagers") ||
                stage == TutorialStage.BUILD_TOWN_CENTRE && button.name.equals(TownCentre.buildingName)) {
                activeButton = button;
                break;
            }
        }
        if (activeButton == null)
            return;

        if (activeButton.y < MC.screen.height / 2) {
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_UP);
            yOffset = (int) MiscUtil.getOscillatingFloat(20, 36);
        }
        else {
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_DOWN);
            yOffset = (int) MiscUtil.getOscillatingFloat(-46, -30);
        }

        if ((System.currentTimeMillis() / 500) % 2 == 0)  {
            GuiComponent.fill(poseStack, // x1,y1, x2,y2,
                    activeButton.x, activeButton.y,
                    activeButton.x + Button.iconFrameSize,
                    activeButton.y + Button.iconFrameSize,
                    0x32FFFFFF); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255
        }
        GuiComponent.blit(poseStack,
                activeButton.x + xOffset, activeButton.y + yOffset,
                32,
                pUOffset, pVOffset,
                pUWidth, pVHeight,
                pTextHeight, pTextWidth
        );
    }

    static int xOffset = -5;
    static int yOffset = 0;

    static int pUOffset = 32; // coordinates on the text file itself to draw from
    static int pVOffset = 32;

    static int pUWidth = 32; // total mapped texture area, if > pTexture it will repeat
    static int pVHeight = 32;

    static int pTextHeight = 32;
    static int pTextWidth = 32;

    /*
    @SubscribeEvent
    public static void onButtonPress(ScreenEvent.KeyPressed.Pre evt) {
        if (Keybindings.shiftMod.isDown() && Keybindings.ctrlMod.isDown()) {
            if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT) {
                xOffset -= 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_RIGHT) {
                xOffset += 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_DOWN) {
                yOffset -= 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_UP) {
                yOffset += 1;
            }
        } else if (Keybindings.shiftMod.isDown()) {
            if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT) {
                pUOffset -= 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_RIGHT) {
                pUOffset += 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_DOWN) {
                pVOffset -= 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_UP) {
                pVOffset += 1;
            }
        } else if (Keybindings.ctrlMod.isDown()) {
            if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT) {
                pUWidth -= 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_RIGHT) {
                pUWidth += 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_DOWN) {
                pVHeight -= 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_UP) {
                pVHeight += 1;
            }
        } else {
            if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT) {
                pTextWidth -= 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_RIGHT) {
                pTextWidth += 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_DOWN) {
                pTextHeight -= 1;
            } else if (evt.getKeyCode() == GLFW.GLFW_KEY_UP) {
                pTextHeight += 1;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
                "xOffset: " + xOffset,
                "yOffset: " + yOffset,
                "pUOffset: " + pUOffset,
                "pVOffset: " + pVOffset,
                "pUWidth: " + pUWidth,
                "pVHeight: " + pVHeight,
                "pTextWidth: " + pTextWidth,
                "pTextHeight: " + pTextHeight,
        });
    }

     */
}
