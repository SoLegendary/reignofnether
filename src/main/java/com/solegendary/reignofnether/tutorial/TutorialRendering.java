package com.solegendary.reignofnether.tutorial;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class TutorialRendering {

    private static Minecraft MC = Minecraft.getInstance();

    private static final ResourceLocation TEXTURE_ARROW_RIGHT = new ResourceLocation("reignofnether", "textures/hud/tutorial_arrow_right.png");
    private static final ResourceLocation TEXTURE_ARROW_LEFT = new ResourceLocation("reignofnether", "textures/hud/tutorial_arrow_left.png");
    private static final ResourceLocation TEXTURE_ARROW_UP = new ResourceLocation("reignofnether", "textures/hud/tutorial_arrow_up.png");
    private static final ResourceLocation TEXTURE_ARROW_DOWN = new ResourceLocation("reignofnether", "textures/hud/tutorial_arrow_down.png");

    static int xOffset2 = 0;
    static int yOffset2 = 0;

    public static void highlightNextButton(PoseStack poseStack, ArrayList<Button> buttons) {
        if (!TutorialClientEvents.isEnabled())
            return;

        TutorialStage stage = TutorialClientEvents.getStage();

        Button activeButton = null;
        for (Button button : buttons) {
            /*
            if (stage == TutorialStage.INTRO && button.name.equals("Villagers") ||
                stage == TutorialStage.BUILD_TOWN_CENTRE && button.name.equals(TownCentre.buildingName)) {
                activeButton = button;
                break;
            }
             */
            if (button.name.equals("Villagers") ||
                button.name.equals(TownCentre.buildingName)) {
                activeButton = button;
                break;
            }
        }
        if (activeButton == null)
            return;

        if ((System.currentTimeMillis() / 500) % 2 == 0)  {
            GuiComponent.fill(poseStack, // x1,y1, x2,y2,
                    activeButton.x, activeButton.y,
                    activeButton.x + Button.iconFrameSize,
                    activeButton.y + Button.iconFrameSize,
                    0x32FFFFFF); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255
        }
        pointAtWithArrow(poseStack, activeButton.x, activeButton.y, false);
    }

    public static void pointAtWithArrow(PoseStack poseStack, int x, int y, boolean vertical) {
        if (MC.screen == null)
            return;

        int xOffset = 0;
        int yOffset = 0;

        if (vertical && y < MC.screen.height / 2) {
            xOffset = -5;
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_UP);
            yOffset = (int) MiscUtil.getOscillatingFloat(20, 36);
        }
        else if (vertical) {
            xOffset = -5;
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_DOWN);
            yOffset = (int) MiscUtil.getOscillatingFloat(-46, -30, 500);
        }
        else if (x < MC.screen.width / 2) {
            yOffset = -5;
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_LEFT);
            xOffset = (int) MiscUtil.getOscillatingFloat(20, 36);
        }
        else {
            yOffset = -5;
            RenderSystem.setShaderTexture(0, TEXTURE_ARROW_RIGHT);
            xOffset = (int) MiscUtil.getOscillatingFloat(-46, -30, 500);
        }

        GuiComponent.blit(poseStack,
                x + xOffset + xOffset2, y + yOffset + yOffset2,
                32,
                32, 32,
                32, 32,
                32, 32
        );
    }

    @SubscribeEvent
    public static void onButtonPress(ScreenEvent.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT) {
            xOffset2 -= 1;
        } else if (evt.getKeyCode() == GLFW.GLFW_KEY_RIGHT) {
            xOffset2 += 1;
        } else if (evt.getKeyCode() == GLFW.GLFW_KEY_DOWN) {
            yOffset2 -= 1;
        } else if (evt.getKeyCode() == GLFW.GLFW_KEY_UP) {
            yOffset2 += 1;
        }
    }

    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
                "xOffset2: " + xOffset2,
                "yOffset2: " + yOffset2,
        });
    }
}
