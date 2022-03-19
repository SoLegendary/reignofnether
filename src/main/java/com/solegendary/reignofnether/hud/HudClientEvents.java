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

    private static ArrayList<Button> buttons = new ArrayList<>();

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.DrawScreenEvent evt) {
        String screenName = evt.getScreen().getTitle().getString();
        if (!OrthoviewClientEvents.isEnabled() || !screenName.equals("topdowngui_container"))
            return;
        if (MC.level == null)
            return;

        ArrayList<LivingEntity> units = new ArrayList<>();
        buttons = new ArrayList<>();

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

            // mob head icon
            String unitName = unit.getName().getString()
                    .replace(" ","")
                    .replace("entity.reignofnether.","")
                    .replace("_unit","");

            buttons.add(new Button(
                    blitX, blitY,
                    mobHeadSize,
                    iconFrameSize,
                    iconFrameSelectedSize,
                    "textures/mobheads/" + unitName +  ".png",
                    "textures/hud/icon_frame.png",
                    "textures/hud/icon_frame_selected.png",
                    unit
            ));

            blitX += iconFrameSize;
        }

        for (Button button : buttons) {
            button.render(evt.getPoseStack());
            button.renderHealthBar(evt.getPoseStack());
        }


    }

    @SubscribeEvent
    public static void mouseEvent(ScreenEvent.MouseClickedEvent evt) {
        for (Button button : buttons) {
            if (evt.getMouseX() >= button.x &&
                evt.getMouseY() >= button.y &&
                evt.getMouseX() < button.x + button.iconFrameSize &&
                evt.getMouseY() < button.y + button.iconFrameSize
            ) {
                System.out.println("Clicked on button: " + button.entity.getId());
            }
        }
    }
}
