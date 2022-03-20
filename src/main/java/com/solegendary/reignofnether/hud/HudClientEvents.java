package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.hud.actions.ActionButtons;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.units.UnitClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class HudClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    private static ArrayList<Button> unitButtons = new ArrayList<>();
    private static final ArrayList<Button> actionButtons = new ArrayList<>(Arrays.asList(
            ActionButtons.attack,
            ActionButtons.stop,
            ActionButtons.hold,
            ActionButtons.move
    ));

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.DrawScreenEvent evt) {
        String screenName = evt.getScreen().getTitle().getString();
        if (!OrthoviewClientEvents.isEnabled() || !screenName.equals("topdowngui_container"))
            return;
        if (MC.level == null)
            return;

        int mouseX = evt.getMouseX();
        int mouseY = evt.getMouseY();

        ArrayList<LivingEntity> units = new ArrayList<>();
        unitButtons = new ArrayList<>();

        for (int id: UnitClientEvents.getSelectedUnitIds()) {
            Entity entity = MC.level.getEntity(id);
            if (entity instanceof LivingEntity)
                units.add((LivingEntity) entity);
        }

        int screenWidth = MC.getWindow().getGuiScaledWidth();
        int screenHeight = MC.getWindow().getGuiScaledHeight();

        int iconSize = 14;
        int iconFrameSize = 22;
        int iconFrameSelectedSize = 24;

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

            unitButtons.add(new Button(
                    blitX, blitY,
                    iconSize,
                    iconFrameSize,
                    iconFrameSelectedSize,
                    "textures/mobheads/" + unitName +  ".png",
                    "textures/hud/icon_frame.png",
                    "textures/hud/icon_frame_selected.png",
                    unit,
                    () -> { return false; },
                    () -> {
                        UnitClientEvents.setSelectedUnitIds(new ArrayList<>());
                        UnitClientEvents.addSelectedUnitId(unit.getId());
                    }
            ));
            blitX += iconFrameSize;
        }

        for (Button button : unitButtons) {
            button.render(evt.getPoseStack(), mouseX, mouseY);
            button.renderHealthBar(evt.getPoseStack());
        }

        // -------------------------------------------------------
        // Unit action icons (attack, stop, move, abilities etc.)
        // -------------------------------------------------------
        blitX = 0;
        blitY = screenHeight - iconFrameSize;

        if (UnitClientEvents.getSelectedUnitIds().size() > 0) {

            for (Button actionButton : actionButtons) {
                actionButton.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
                blitX += actionButton.iconFrameSize;
                actionButton.checkPressed();
            }
        }

    }

    @SubscribeEvent
    public static void mouseEvent(ScreenEvent.MouseClickedEvent evt) {
        int mouseX = (int) evt.getMouseX();
        int mouseY = (int) evt.getMouseY();

        ArrayList<Button> buttons = new ArrayList<>();
        buttons.addAll(actionButtons);
        buttons.addAll(unitButtons);

        for (Button button : buttons) {
            if (mouseX >= button.x &&
                mouseY >= button.y &&
                mouseX < button.x + button.iconFrameSize &&
                mouseY < button.y + button.iconFrameSize
            ) {
                button.checkClicked(mouseX, mouseY);
            }
        }
    }
}
