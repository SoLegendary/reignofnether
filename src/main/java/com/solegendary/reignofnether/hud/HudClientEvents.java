package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.Unit;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.model.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class HudClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static int mouseX = 0;
    private static int mouseY = 0;

    private static ArrayList<Button> unitButtons = new ArrayList<>();
    private static final ArrayList<Button> genericActionButtons = new ArrayList<>(Arrays.asList(
            ActionButtons.attack,
            ActionButtons.stop,
            ActionButtons.hold,
            ActionButtons.move
    ));
    // unit type that is selected in the list of unit icons
    public static LivingEntity hudSelectedEntity = null;
    // classes used to render unit or building portrait (mode, frame, healthbar, stats)
    public static PortraitRendererUnit portraitRendererUnit = new PortraitRendererUnit(null);
    public static PortraitRendererBuilding portraitRendererBuilding = new PortraitRendererBuilding();

    // where to start drawing the centre hud (from left to right: portrait, stats, unit icon buttons)
    private static int hudStartingXPos = 0;

    // eg. entity.reignofnether.zombie_unit -> zombie
    public static String getSimpleEntityName(Entity entity) {
        if (entity instanceof Unit)
            return entity.getName().getString()
                .replace(" ","")
                .replace("entity.reignofnether.","")
                .replace("_unit","");
        else
            return entity.getName().getString();
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render evt) {
        String screenName = evt.getScreen().getTitle().getString();
        if (!OrthoviewClientEvents.isEnabled() || !screenName.equals("topdowngui_container"))
            return;
        if (MC.level == null)
            return;

        hudStartingXPos = (MC.getWindow().getGuiScaledWidth() / 5) + 4;

        mouseX = evt.getMouseX();
        mouseY = evt.getMouseY();

        ArrayList<LivingEntity> units = UnitClientEvents.getSelectedUnits();

        // create all the unit buttons for this frame
        int screenWidth = MC.getWindow().getGuiScaledWidth();
        int screenHeight = MC.getWindow().getGuiScaledHeight();

        int iconSize = 14;
        int iconFrameSize = Button.iconFrameSize;

        // screenWidth ranges roughly between 440-540
        int unitButtonsPerRow = (int) Math.ceil((float) (screenWidth - 340) / iconFrameSize);
        unitButtonsPerRow = Math.min(unitButtonsPerRow, 10);
        unitButtonsPerRow = Math.max(unitButtonsPerRow, 4);

        unitButtons = new ArrayList<>();
        for (LivingEntity unit : units) {
            if (UnitClientEvents.getPlayerToEntityRelationship(unit.getId()) == Relationship.OWNED &&
                unitButtons.size() < (unitButtonsPerRow * 2)) {
                // mob head icon
                String unitName = getSimpleEntityName(unit);

                unitButtons.add(new Button(
                        unitName,
                        iconSize,
                        "textures/mobheads/" + unitName + ".png",
                        unit,
                        () -> getSimpleEntityName(hudSelectedEntity).equals(unitName),
                        () -> false,
                        () -> {
                            // click to select this unit type as a group
                            if (getSimpleEntityName(hudSelectedEntity).equals(unitName)) {
                                UnitClientEvents.setSelectedUnitIds(new ArrayList<>());
                                UnitClientEvents.addSelectedUnitId(unit.getId());
                            } else { // select this one specific unit
                                hudSelectedEntity = unit;
                            }
                        }
                ));
            }
        }
        int blitX = hudStartingXPos;
        int blitY = MC.getWindow().getGuiScaledHeight();

        Building selBuilding = BuildingClientEvents.getSelectedBuilding();

        if (selBuilding != null) {
            // -----------------
            // Building portrait
            // -----------------
            blitY -= portraitRendererBuilding.frameHeight;

            portraitRendererBuilding.render(
                    evt.getPoseStack(),
                    blitX, blitY, selBuilding);

            blitX += portraitRendererBuilding.frameWidth * 2;

            // -------------------------
            // Building production queue
            // -------------------------


            // ---------------------------
            // Building production buttons
            // ---------------------------
            blitX = 0;
            blitY = screenHeight - iconFrameSize;

            if (selBuilding instanceof ProductionBuilding && selBuilding.isBuilt) {
                for (Button productionButton : ((ProductionBuilding) selBuilding).productionButtons) {
                    productionButton.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
                    productionButton.checkPressed();
                    productionButton.checkClicked(mouseX, mouseY);
                    blitX += iconFrameSize;
                }
            }
        }

        // --------------------------
        // Unit head portrait + stats
        // --------------------------
        else if (hudSelectedEntity != null &&
                portraitRendererUnit.model != null &&
                portraitRendererUnit.renderer != null) {

            blitY -= portraitRendererUnit.frameHeight;

            // write capitalised unit name
            String name = getSimpleEntityName(hudSelectedEntity);
            String nameCap = name.substring(0, 1).toUpperCase() + name.substring(1);

            portraitRendererUnit.render(
                    evt.getPoseStack(), nameCap,
                    blitX, blitY, hudSelectedEntity);

            blitX += portraitRendererUnit.frameWidth * 2;
        }

        // ----------------------------------------------
        // Unit icons using mob heads on 2 rows if needed
        // ----------------------------------------------
        int buttonsRendered = 0;
        int blitXStart = blitX;
        blitY = screenHeight - iconFrameSize * 2 - 10;

        if (unitButtons.size() >= 2) {
            MyRenderer.renderFrameWithBg(evt.getPoseStack(), blitX - 5, blitY - 10,
                    iconFrameSize * unitButtonsPerRow + 10,
                    iconFrameSize * 2 + 20,
                    0xA0000000);

            for (Button unitButton : unitButtons) {
                // replace last icon with a +X number of units icon
                if (buttonsRendered == (unitButtonsPerRow * 2) - 1 &&
                        units.size() > (unitButtonsPerRow * 2)) {
                    int numExtraUnits = units.size() - (unitButtonsPerRow * 2) + 1;
                    MyRenderer.renderIconFrameWithBg(evt.getPoseStack(), blitX, blitY, iconFrameSize, 0x64000000);
                    GuiComponent.drawCenteredString(evt.getPoseStack(), MC.font, "+" + numExtraUnits,
                            blitX + iconFrameSize/2, blitY + 8, 0xFFFFFF);
                }
                else {
                    unitButton.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
                    unitButton.renderHealthBar(evt.getPoseStack());
                    blitX += iconFrameSize;
                    buttonsRendered += 1;
                    if (buttonsRendered == unitButtonsPerRow) {
                        blitX = blitXStart;
                        blitY += iconFrameSize + 6;
                    }
                }
            }
        }

        // --------------------------------------------------------
        // Unit action buttons (attack, stop, move, abilities etc.)
        // --------------------------------------------------------
        ArrayList<Integer> selUnitIds = UnitClientEvents.getSelectedUnitIds();
        if (selUnitIds.size() > 0 &&
            UnitClientEvents.getPlayerToEntityRelationship(selUnitIds.get(0)) == Relationship.OWNED) {

            blitX = 0;
            blitY = screenHeight - iconFrameSize;
            for (Button actionButton : genericActionButtons) {
                actionButton.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
                actionButton.checkPressed();
                actionButton.checkClicked(mouseX, mouseY);
                blitX += iconFrameSize;
            }
            blitX = 0;
            blitY = screenHeight - (iconFrameSize * 2);
            for (LivingEntity unit : units) {
                if (getSimpleEntityName(unit).equals(getSimpleEntityName(hudSelectedEntity))) {
                    for (AbilityButton ability : ((Unit) unit).getAbilities()) {
                        ability.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
                        ability.checkPressed();
                        ability.checkClicked(mouseX, mouseY);
                        blitX += iconFrameSize;
                    }
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseButtonReleased.Post evt) {
        int mouseX = (int) evt.getMouseX();
        int mouseY = (int) evt.getMouseY();

        ArrayList<Button> buttons = new ArrayList<>();
        buttons.addAll(unitButtons);
        for (Button button : unitButtons)
            button.checkClicked(mouseX, mouseY);
    }

    @SubscribeEvent
    // hudSelectedUnit and portraitRendererUnit should be assigned in the same event to avoid desyncs
    public static void onRenderLivingEntity(RenderLivingEvent.Pre<? extends LivingEntity, ? extends Model> evt) {

        ArrayList<LivingEntity> units = UnitClientEvents.getSelectedUnits();

        // sort and hudSelect the first unit type in the list
        units.sort(Comparator.comparing(HudClientEvents::getSimpleEntityName));

        if (units.size() <= 0)
            hudSelectedEntity = null;
        else if (hudSelectedEntity == null || units.size() == 1)
            hudSelectedEntity = units.get(0);

        if (hudSelectedEntity == null) {
            portraitRendererUnit.model = null;
            portraitRendererUnit.renderer = null;
        }
        else if (evt.getEntity() == hudSelectedEntity) {
            portraitRendererUnit.model = evt.getRenderer().getModel();
            portraitRendererUnit.renderer = evt.getRenderer();
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent evt) {
        if (OrthoviewClientEvents.isEnabled())
            portraitRendererUnit.tickAnimation();
    }

    @SubscribeEvent
    public static void onRenderNamePlate(RenderNameTagEvent evt) {
        if (OrthoviewClientEvents.isEnabled())
            evt.setResult(Event.Result.DENY);
    }

    // uncomment to adjust render position/size of portraits
    /*
    @SubscribeEvent
    public static void onInput(InputEvent.KeyInputEvent evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions
            if (evt.getKey() == Keybinding.panMinusX.getKey().getValue())
                portraitRendererUnit.headOffsetX += 1;
            if (evt.getKey() == Keybinding.panPlusX.getKey().getValue())
                portraitRendererUnit.headOffsetX -= 1;
            if (evt.getKey() == Keybinding.panMinusZ.getKey().getValue())
                portraitRendererUnit.headOffsetY += 1;
            if (evt.getKey() == Keybinding.panPlusZ.getKey().getValue())
                portraitRendererUnit.headOffsetY -= 1;

            if (evt.getKey() == Keybinding.nums[9].getKey().getValue())
                portraitRendererUnit.headSize -= 1;
            if (evt.getKey() == Keybinding.nums[0].getKey().getValue())
                portraitRendererUnit.headSize += 1;
        }
    }
    @SubscribeEvent
    public static void onRenderOverLay(RenderGameOverlayEvent.Pre evt) {

        if (hudSelectedEntity != null)
            MiscUtil.drawDebugStrings(evt.getMatrixStack(), MC.font, new String[] {
                    "headOffsetX: " + portraitRendererUnit.headOffsetX,
                    "headOffsetY: " + portraitRendererUnit.headOffsetY,
                    "headSize: " + portraitRendererUnit.headSize,
                    "eyeHeight: " + hudSelectedEntity.getEyeHeight(),
            });
    }
     */

    /*
    @SubscribeEvent
    public static void onRenderOverLay(RenderGameOverlayEvent.Pre evt) {

        if (hudSelectedEntity != null)
            MiscUtil.drawDebugStrings(evt.getMatrixStack(), MC.font, new String[] {
                    "entity eye height: " + hudSelectedEntity.getEyeHeight(),
                    "entity eye pos: " + hudSelectedEntity.getEyePosition(),
            });
    }*/
}
