package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.units.UnitClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class HudClientEvents {

    private static final ResourceLocation TEXTURE_ICONFRAME = new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png");
    private static final ResourceLocation TEXTURE_ICONFRAME_SELECTED = new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame_selected.png");

    private static final Minecraft MC = Minecraft.getInstance();

    private static final int mobHeadSize = 14;
    private static final int iconFrameSize = 22;
    private static final int iconFrameSelectedSize = 24;

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.DrawScreenEvent evt) {
        String screenName = evt.getScreen().getTitle().getString();
        if (!OrthoviewClientEvents.isEnabled() || !screenName.equals("topdowngui_container"))
            return;

        ArrayList<String> unitNames = new ArrayList<>();
        if (MC.level != null)
        {
            for (int id: UnitClientEvents.getSelectedUnitIds()) {
                Entity entity = MC.level.getEntity(id);
                if (entity != null)
                    unitNames.add(entity.getName().getString()
                            .replace(" ","")
                            .replace("entity.reignofnether.","")
                            .replace("_unit","")
                    );
            }
        }

        int screenWidth = MC.getWindow().getGuiScaledWidth();
        int screenHeight = MC.getWindow().getGuiScaledHeight();

        int blitX = (screenWidth / 2) - (unitNames.size() * iconFrameSize / 2);
        int blitY = screenHeight - iconFrameSize;

        for (String unitName : unitNames) {
            // icon frame and transparent background
            RenderSystem.setShaderTexture(0,
                    new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png")
            );
            GuiComponent.blit(evt.getPoseStack(),
                    blitX, blitY, 0,
                    0,0, // where on texture to start drawing from
                    iconFrameSize, iconFrameSize, // dimensions of blit texture
                    iconFrameSize, iconFrameSize // size of texture itself (if < dimensions, texture is repeated)
            );

            // mob head icon
            RenderSystem.setShaderTexture(0,
                    new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/" + unitName +  ".png")
            );
            GuiComponent.blit(evt.getPoseStack(),
                    blitX + 4, blitY + 4, 0,
                    0,0, // where on texture to start drawing from
                    mobHeadSize, mobHeadSize, // dimensions of blit area
                    mobHeadSize, mobHeadSize // size of texture (if < dimensions, texture is repeated)
            );
            blitX += iconFrameSize;

            // TODO: show health bars above unit icon frames
        }
    }
}
