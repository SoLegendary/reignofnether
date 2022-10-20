package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.platform.InputConstants;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.Unit;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.model.*;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class HudClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    private static String tempMsg = "";
    private static int tempMsgTicksLeft = 0;
    private static final int tempMsgTicksFade = 50; // ticks left when the msg starts to fade
    private static final int tempMsgTicksMax = 150; // ticks to show the msg for

    private static final ArrayList<Button> genericActionButtons = new ArrayList<>(Arrays.asList(
            ActionButtons.attack,
            ActionButtons.stop,
            ActionButtons.hold,
            ActionButtons.move
    ));
    private static final ArrayList<Button> unitButtons = new ArrayList<>();
    private static final ArrayList<Button> productionButtons = new ArrayList<>();
    // buttons which are rendered at the moment in RenderEvent
    private static final ArrayList<Button> renderedButtons = new ArrayList<>();

    // unit type that is selected in the list of unit icons
    public static LivingEntity hudSelectedEntity = null;
    // classes used to render unit or building portrait (mode, frame, healthbar, stats)
    public static PortraitRendererUnit portraitRendererUnit = new PortraitRendererUnit(null);
    public static PortraitRendererBuilding portraitRendererBuilding = new PortraitRendererBuilding();

    // where to start drawing the centre hud (from left to right: portrait, stats, unit icon buttons)
    private static int hudStartingXPos = 0;

    private static int mouseX = 0;
    private static int mouseY = 0;

    private final static int iconBgColour = 0x64000000;
    private final static int frameBgColour = 0xA0000000;


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

    public static void showTemporaryMessage(String msg) {
        tempMsgTicksLeft = tempMsgTicksMax;
        tempMsg = msg;
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render evt) {
        String screenName = evt.getScreen().getTitle().getString();
        if (!OrthoviewClientEvents.isEnabled() || !screenName.equals("topdowngui_container"))
            return;
        if (MC.level == null)
            return;

        mouseX = evt.getMouseX();
        mouseY = evt.getMouseY();

        hudStartingXPos = (MC.getWindow().getGuiScaledWidth() / 5) + 4;

        ArrayList<LivingEntity> units = UnitClientEvents.getSelectedUnits();

        // create all the unit buttons for this frame
        int screenWidth = MC.getWindow().getGuiScaledWidth();
        int screenHeight = MC.getWindow().getGuiScaledHeight();

        int iconSize = 14;
        int iconFrameSize = Button.iconFrameSize;

        // screenWidth ranges roughly between 440-540
        int buttonsPerRow = (int) Math.ceil((float) (screenWidth - 340) / iconFrameSize);
        buttonsPerRow = Math.min(buttonsPerRow, 8);
        buttonsPerRow = Math.max(buttonsPerRow, 4);

        // refresh button lists
        unitButtons.clear();
        productionButtons.clear();
        renderedButtons.clear();

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

            blitX += portraitRendererBuilding.frameWidth + 10;

            // -------------------------
            // Building production queue
            // -------------------------

            // bottom row for all other queued items
            if (selBuilding instanceof ProductionBuilding selProdBuilding) {
                int blitXStart = blitX;
                blitY = screenHeight - iconFrameSize * 2 - 5;

                for (int i = 0; i < selProdBuilding.productionQueue.size(); i++)
                    productionButtons.add(selProdBuilding.productionQueue.get(i).getCancelButton(selProdBuilding, i == 0));

                if (productionButtons.size() >= 1) {
                    // background frame
                    MyRenderer.renderFrameWithBg(evt.getPoseStack(), blitX - 5, blitY - 10,
                            iconFrameSize * buttonsPerRow + 10,
                            iconFrameSize * 2 + 15,
                            frameBgColour);

                    // name and progress %
                    ProductionItem firstProdItem = selProdBuilding.productionQueue.get(0);
                    float percentageDone = (float) firstProdItem.ticksLeft / (float) firstProdItem.ticksToProduce;

                    GuiComponent.drawCenteredString(evt.getPoseStack(), MC.font,
                            Math.round(100 - (percentageDone * 100f)) + "% " + firstProdItem.getItemName(),
                            blitX + iconFrameSize * 2 + 12,
                            blitY + 2,
                            0xFFFFFF);

                    int buttonsRendered = 0;
                    for (Button prodButton : productionButtons) {
                        // top row for currently-in-progress item
                        if (buttonsRendered == 0) {
                            prodButton.greyPercent = 1 - percentageDone;
                            prodButton.render(evt.getPoseStack(), blitX, blitY - 5, mouseX, mouseY);
                            renderedButtons.add(prodButton);
                        }
                        // replace last icon with a +X number of production items left in queue
                        else if (buttonsRendered >= buttonsPerRow &&
                                productionButtons.size() > (buttonsPerRow + 1))
                        {
                            int numExtraItems = productionButtons.size() - buttonsPerRow;
                            MyRenderer.renderIconFrameWithBg(evt.getPoseStack(), blitX, blitY + iconFrameSize, iconFrameSize, iconBgColour);
                            GuiComponent.drawCenteredString(evt.getPoseStack(), MC.font, "+" + numExtraItems,
                                    blitX + iconFrameSize/2, blitY + iconFrameSize + 8, 0xFFFFFF);
                            break;
                        }
                        // bottom row for all other queued items
                        else {
                            prodButton.render(evt.getPoseStack(), blitX, blitY + iconFrameSize, mouseX, mouseY);
                            renderedButtons.add(prodButton);
                            blitX += iconFrameSize;
                        }
                        buttonsRendered += 1;
                    }
                }
            }


            // ---------------------------
            // Building production buttons
            // ---------------------------
            blitX = 0;
            blitY = screenHeight - iconFrameSize;

            if (!selBuilding.isBuilt) {
                Button cancelButton = new Button(
                        "Cancel",
                        iconSize,
                        "textures/icons/items/barrier.png",
                        Keybinding.cancelBuild,
                        () -> false,
                        () -> false,
                        () -> true,
                        () -> {
                            BuildingServerboundPacket.cancelBuilding(BuildingUtils.getMinCorner(selBuilding.getBlocks()));
                        },
                        List.of(FormattedCharSequence.forward("Cancel", Style.EMPTY))
                );
                cancelButton.render(evt.getPoseStack(), 0, screenHeight - iconFrameSize, mouseX, mouseY);
                renderedButtons.add(cancelButton);
            }
            else if (selBuilding instanceof ProductionBuilding selProdBuilding) {
                for (Button productionButton : selProdBuilding.productionButtons) {
                    productionButton.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
                    productionButtons.add(productionButton);
                    renderedButtons.add(productionButton);
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
        int blitXStart = blitX;
        blitY = screenHeight - iconFrameSize * 2 - 10;

        for (LivingEntity unit : units) {
            if (UnitClientEvents.getPlayerToEntityRelationship(unit.getId()) == Relationship.OWNED &&
                    unitButtons.size() < (buttonsPerRow * 2)) {
                // mob head icon
                String unitName = getSimpleEntityName(unit);

                unitButtons.add(new Button(
                    unitName,
                    iconSize,
                    "textures/mobheads/" + unitName + ".png",
                    unit,
                    () -> getSimpleEntityName(hudSelectedEntity).equals(unitName),
                    () -> false,
                    () -> true,
                    () -> {
                        // click to select this unit type as a group
                        if (getSimpleEntityName(hudSelectedEntity).equals(unitName)) {
                            UnitClientEvents.setSelectedUnitIds(new ArrayList<>());
                            UnitClientEvents.addSelectedUnitId(unit.getId());
                        } else { // select this one specific unit
                            hudSelectedEntity = unit;
                        }
                    },
                    null
                ));
            }
        }

        if (unitButtons.size() >= 2) {
            // background frame
            MyRenderer.renderFrameWithBg(evt.getPoseStack(), blitX - 5, blitY - 10,
                    iconFrameSize * buttonsPerRow + 10,
                    iconFrameSize * 2 + 20,
                    frameBgColour);

            int buttonsRendered = 0;
            for (Button unitButton : unitButtons) {
                // replace last icon with a +X number of units icon
                if (buttonsRendered >= (buttonsPerRow * 2) - 1 &&
                        units.size() > (buttonsPerRow * 2)) {
                    int numExtraUnits = units.size() - (buttonsPerRow * 2) + 1;
                    MyRenderer.renderIconFrameWithBg(evt.getPoseStack(), blitX, blitY, iconFrameSize, iconBgColour);
                    GuiComponent.drawCenteredString(evt.getPoseStack(), MC.font, "+" + numExtraUnits,
                            blitX + iconFrameSize/2, blitY + 8, 0xFFFFFF);
                    break;
                }
                else {
                    unitButton.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
                    renderedButtons.add(unitButton);
                    unitButton.renderHealthBar(evt.getPoseStack());
                    blitX += iconFrameSize;
                    if (buttonsRendered == buttonsPerRow - 1) {
                        blitX = blitXStart;
                        blitY += iconFrameSize + 6;
                    }
                }
                buttonsRendered += 1;
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
                renderedButtons.add(actionButton);
                blitX += iconFrameSize;
            }
            blitX = 0;
            blitY = screenHeight - (iconFrameSize * 2);
            for (LivingEntity unit : units) {
                if (getSimpleEntityName(unit).equals(getSimpleEntityName(hudSelectedEntity))) {
                    for (AbilityButton abilityButton : ((Unit) unit).getAbilities()) {
                        abilityButton.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
                        renderedButtons.add(abilityButton);
                        blitX += iconFrameSize;
                    }
                    break;
                }
            }
        }

        // ---------------------------
        // Resources icons and amounts
        // ---------------------------
        Resources resources = ResourcesClientEvents.getOwnResources();

        if (resources != null) {
            blitX = 0;
            blitY = 0;

            for (String resourceName : new String[]{ "food", "wood", "ore", "pop" }) {
                String rlPath = "";
                String resValueStr = "";
                switch (resourceName) {
                    case "food" -> {
                        rlPath = "textures/icons/items/wheat.png";
                        resValueStr = String.valueOf(resources.food);
                    }
                    case "wood" -> {
                        rlPath = "textures/icons/items/wood.png";
                        resValueStr = String.valueOf(resources.wood);
                    }
                    case "ore" -> {
                        rlPath = "textures/icons/items/iron_ore.png";
                        resValueStr = String.valueOf(resources.ore);
                    }
                    case "pop" -> {
                        rlPath = "textures/icons/items/bed.png";
                        resValueStr = UnitClientEvents.getCurrentPopulation() + "/" + BuildingClientEvents.getTotalPopulationSupply();
                    }
                }
                MyRenderer.renderFrameWithBg(evt.getPoseStack(), blitX + iconFrameSize - 1, blitY,
                        49,
                        iconFrameSize,
                        frameBgColour);
                MyRenderer.renderIconFrameWithBg(evt.getPoseStack(), blitX, blitY, iconFrameSize, iconBgColour);
                MyRenderer.renderIcon(evt.getPoseStack(),
                        new ResourceLocation(ReignOfNether.MOD_ID, rlPath),
                        blitX+4, blitY+4, iconSize
                );
                GuiComponent.drawCenteredString(evt.getPoseStack(), MC.font, resValueStr,
                        blitX + (iconFrameSize) + 24 , blitY + (iconSize / 2) + 1, 0xFFFFFF);
                /*
                if (resToAddValue != 0) {
                    boolean positive = resToAddValue > 0;
                    GuiComponent.drawString(evt.getPoseStack(), MC.font, (positive ? "+" : "") + resToAddValue,
                            blitX + (iconFrameSize) * 3/2 + 42, blitY + (iconSize / 2) + 1, (positive ? 0x00FF00 : 0xFF0000));
                }*/
                blitY += iconFrameSize - 1;
            }
        }

        // --------------------------
        // Temporary warning messages
        // --------------------------
        if (tempMsgTicksLeft > 0 && tempMsg.length() > 0) {
            int ticksUnderFade = Math.min(tempMsgTicksLeft, tempMsgTicksFade);
            int alpha = (int) (0xFF * ((float) ticksUnderFade / (float) tempMsgTicksFade));

            GuiComponent.drawCenteredString(evt.getPoseStack(), MC.font,
                tempMsg,
                screenWidth / 2,
                screenHeight - iconFrameSize * 2 - 50,
                0xFFFFFF + (alpha << 24));
        }
        if (tempMsgTicksLeft > 0)
            tempMsgTicksLeft -= 1;

        // ------------------------------------------------------
        // Button tooltips (has to be rendered last to be on top)
        // ------------------------------------------------------
        for (Button button : renderedButtons)
            if (button.isMouseOver(mouseX, mouseY))
                button.renderTooltip(evt.getPoseStack(), mouseX, mouseY);
    }

    public static boolean isMouseOverAnyButton() {
        for (Button button : renderedButtons)
            if (button.isMouseOver(mouseX, mouseY))
                return true;
        return false;
    }

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {
        if (evt.getButton() != GLFW.GLFW_MOUSE_BUTTON_1)
            return;
        for (Button button : renderedButtons)
            button.checkLeftClicked((int) evt.getMouseX(), (int) evt.getMouseY());
    }

    // TODO: Q and E don't work properly (probably due to conflicting with vanilla hotkeys?)
    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key evt) {
        if (evt.getAction() != InputConstants.PRESS)
            return;
        for (Button button : renderedButtons)
            button.checkPressed(evt.getKey());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent evt) {
        if (OrthoviewClientEvents.isEnabled())
            portraitRendererUnit.tickAnimation();
    }

    @SubscribeEvent
    // hudSelectedEntity and portraitRendererUnit should be assigned in the same event to avoid desyncs
    public static void onRenderLivingEntity(RenderLivingEvent.Pre<? extends LivingEntity, ? extends Model> evt) {

        ArrayList<LivingEntity> units = UnitClientEvents.getSelectedUnits();

        // sort and hudSelect the first unit type in the list
        units.sort(Comparator.comparing(HudClientEvents::getSimpleEntityName));

        if (units.size() <= 0)
            hudSelectedEntity = null;
        else if (hudSelectedEntity == null || units.size() == 1 || !units.contains(hudSelectedEntity))
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
    public static void onRenderNamePlate(RenderNameTagEvent evt) {
        if (OrthoviewClientEvents.isEnabled())
            evt.setResult(Event.Result.DENY);
    }

    /*
    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
            "x: " + MC.mouseHandler.xpos(),
            "y: " + MC.mouseHandler.ypos()
        });
    }*/

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
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {

        if (hudSelectedEntity != null)
            MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
                    "headOffsetX: " + portraitRendererUnit.headOffsetX,
                    "headOffsetY: " + portraitRendererUnit.headOffsetY,
                    "headSize: " + portraitRendererUnit.headSize,
                    "eyeHeight: " + hudSelectedEntity.getEyeHeight(),
            });
    }
     */

    /*
    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {

        if (hudSelectedEntity != null)
            MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
                    "entity eye height: " + hudSelectedEntity.getEyeHeight(),
                    "entity eye pos: " + hudSelectedEntity.getEyePosition(),
            });
    }*/
}
