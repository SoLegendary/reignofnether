package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.healthbars.HealthBarClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.units.UnitClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
        if (MC.level == null)
            return;

        ArrayList<LivingEntity> units = new ArrayList<>();

        for (int id: UnitClientEvents.getSelectedUnitIds()) {
            Entity entity = MC.level.getEntity(id);
            if (entity instanceof LivingEntity)
                units.add((LivingEntity) entity);
        }

        int screenWidth = MC.getWindow().getGuiScaledWidth();
        int screenHeight = MC.getWindow().getGuiScaledHeight();

        int blitX = (screenWidth / 2) - (units.size() * iconFrameSize / 2);
        int blitY = screenHeight - iconFrameSize;

        // TODO: sort units
        //Collections.sort(units);

        for (LivingEntity unit : units) {

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
            String unitName = unit.getName().getString()
                    .replace(" ","")
                    .replace("entity.reignofnether.","")
                    .replace("_unit","");

            RenderSystem.setShaderTexture(0,
                    new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/" + unitName +  ".png")
            );
            GuiComponent.blit(evt.getPoseStack(),
                    blitX + 4, blitY + 4, 0,
                    0,0, // where on texture to start drawing from
                    mobHeadSize, mobHeadSize, // dimensions of blit area
                    mobHeadSize, mobHeadSize // size of texture (if < dimensions, texture is repeated)
            );

            HealthBarClientEvents.render(evt.getPoseStack(), unit,
                    blitX + ((float) iconFrameSize / 2), blitY - 4,
                    iconFrameSize - 1,
                    false);






            blitX += iconFrameSize;
        }
    }
}
