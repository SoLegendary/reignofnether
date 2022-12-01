package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.villagers.VillagerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.model.*;
import net.minecraft.core.BlockPos;
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

import static com.solegendary.reignofnether.unit.UnitClientEvents.getPlayerToEntityRelationship;

public class HudClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    private static String tempMsg = "";
    private static int tempMsgTicksLeft = 0;
    private static final int tempMsgTicksFade = 50; // ticks left when the msg starts to fade
    private static final int tempMsgTicksMax = 150; // ticks to show the msg for

    private static final ArrayList<Button> genericActionButtonsWorker = new ArrayList<>(Arrays.asList(
            ActionButtons.BUILD_REPAIR,
            ActionButtons.GATHER,
            ActionButtons.HOLD,
            ActionButtons.MOVE,
            ActionButtons.STOP
    ));
    private static final ArrayList<Button> genericActionButtonsAttacker = new ArrayList<>(Arrays.asList(
            ActionButtons.ATTACK,
            ActionButtons.HOLD,
            ActionButtons.MOVE,
            ActionButtons.STOP
    ));
    private static final ArrayList<ControlGroup> controlGroups = new ArrayList<>(10);

    private static final ArrayList<Button> unitButtons = new ArrayList<>();
    private static final ArrayList<Button> productionButtons = new ArrayList<>();
    // buttons which are rendered at the moment in RenderEvent
    private static final ArrayList<Button> renderedButtons = new ArrayList<>();

    // unit type that is selected in the list of unit icons
    public static LivingEntity hudSelectedEntity = null;
    // classes used to render unit or building portrait (mode, frame, healthbar, stats)
    public static PortraitRendererUnit portraitRendererUnit = new PortraitRendererUnit(null);
    public static PortraitRendererBuilding portraitRendererBuilding = new PortraitRendererBuilding();

    private static RectZone unitPortraitZone = null;
    private static RectZone buildingPortraitZone = null;

    private static int mouseX = 0;
    private static int mouseY = 0;
    private static int mouseLeftDownX = 0;
    private static int mouseLeftDownY = 0;

    private final static int iconBgColour = 0x64000000;
    private final static int frameBgColour = 0xA0000000;

    private static final ArrayList<RectZone> hudZones = new ArrayList<>();


    // eg. entity.reignofnether.zombie_unit -> zombie
    public static String getSimpleEntityName(Entity entity) {
        if (entity instanceof Unit)
            return entity.getName().getString()
                .replace(" ","")
                .replace("entity.reignofnether.","")
                .replace("_unit","")
                .replace(".none","");
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

        // where to start drawing the centre hud (from left to right: portrait, stats, unit icon buttons)
        int hudStartingXPos = (MC.getWindow().getGuiScaledWidth() / 5) + 20;

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

        unitButtons.clear();
        productionButtons.clear();
        renderedButtons.clear();
        hudZones.clear();
        unitPortraitZone = null;
        buildingPortraitZone = null;

        int blitX = hudStartingXPos;
        int blitY = MC.getWindow().getGuiScaledHeight();

        Building selBuilding = BuildingClientEvents.getSelectedBuilding();

        if (selBuilding != null) {
            boolean buildingOwned = BuildingClientEvents.getPlayerToBuildingRelationship(selBuilding) == Relationship.OWNED;

            // -----------------
            // Building portrait
            // -----------------
            blitY -= portraitRendererBuilding.frameHeight;

            buildingPortraitZone = portraitRendererBuilding.render(
                    evt.getPoseStack(),
                    blitX, blitY, selBuilding);
            hudZones.add(buildingPortraitZone);

            blitX += portraitRendererBuilding.frameWidth + 10;

            // -------------------------
            // Building production queue
            // -------------------------

            // bottom row for all other queued items
            if (buildingOwned && selBuilding instanceof ProductionBuilding selProdBuilding) {
                int blitXStart = blitX;
                blitY = screenHeight - iconFrameSize * 2 - 5;

                for (int i = 0; i < selProdBuilding.productionQueue.size(); i++)
                    productionButtons.add(selProdBuilding.productionQueue.get(i).getCancelButton(selProdBuilding, i == 0));

                if (productionButtons.size() >= 1) {
                    // background frame
                    hudZones.add(MyRenderer.renderFrameWithBg(evt.getPoseStack(), blitX - 5, blitY - 10,
                            iconFrameSize * buttonsPerRow + 10,
                            iconFrameSize * 2 + 15,
                            frameBgColour));

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

            // TODO: doesn't work for towers?
            if (buildingOwned && !selBuilding.isBuilt) {
                Button cancelButton = new Button(
                        "Cancel",
                        iconSize,
                        new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
                        Keybindings.cancelBuild,
                        () -> false,
                        () -> false,
                        () -> true,
                        () -> BuildingServerboundPacket.cancelBuilding(BuildingUtils.getMinCorner(selBuilding.getBlocks())),
                        null,
                        List.of(FormattedCharSequence.forward("Cancel", Style.EMPTY))
                );
                cancelButton.render(evt.getPoseStack(), 0, screenHeight - iconFrameSize, mouseX, mouseY);
                renderedButtons.add(cancelButton);
            }
            else if (buildingOwned && selBuilding instanceof ProductionBuilding selProdBuilding) {
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


            unitPortraitZone = portraitRendererUnit.render(
                    evt.getPoseStack(), nameCap,
                    blitX, blitY, hudSelectedEntity);
            hudZones.add(unitPortraitZone);

            blitX += portraitRendererUnit.frameWidth;

            if (hudSelectedEntity instanceof Unit)
                hudZones.add(portraitRendererUnit.renderStats(
                        evt.getPoseStack(), nameCap,
                        blitX, blitY, (Unit) hudSelectedEntity));

            blitX += portraitRendererUnit.frameWidth;
        }

        // ----------------------------------------------
        // Unit icons using mob heads on 2 rows if needed
        // ----------------------------------------------
        int blitXStart = blitX;
        blitY = screenHeight - iconFrameSize * 2 - 10;

        for (LivingEntity unit : units) {
            if (getPlayerToEntityRelationship(unit) == Relationship.OWNED &&
                    unitButtons.size() < (buttonsPerRow * 2)) {
                // mob head icon
                String unitName = getSimpleEntityName(unit);

                unitButtons.add(new Button(
                    unitName,
                    iconSize,
                    new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/" + unitName + ".png"),
                    unit,
                    () -> getSimpleEntityName(hudSelectedEntity).equals(unitName),
                    () -> false,
                    () -> true,
                    () -> {
                        // click to select this unit type as a group
                        if (getSimpleEntityName(hudSelectedEntity).equals(unitName)) {
                            UnitClientEvents.setSelectedUnits(new ArrayList<>());
                            UnitClientEvents.addSelectedUnit(unit);
                        } else { // select this one specific unit
                            hudSelectedEntity = unit;
                        }
                    },
                    null,
                    null
                ));
            }
        }

        if (unitButtons.size() >= 2) {
            // background frame
            hudZones.add(MyRenderer.renderFrameWithBg(evt.getPoseStack(), blitX - 5, blitY - 10,
                    iconFrameSize * buttonsPerRow + 10,
                    iconFrameSize * 2 + 20,
                    frameBgColour));

            int buttonsRendered = 0;
            for (Button unitButton : unitButtons) {
                // replace last icon with a +X number of units icon and hover tooltip for what those units are
                if (buttonsRendered >= (buttonsPerRow * 2) - 1 &&
                        units.size() > (buttonsPerRow * 2)) {
                    int numExtraUnits = units.size() - (buttonsPerRow * 2) + 1;
                    RectZone plusUnitsZone = MyRenderer.renderIconFrameWithBg(evt.getPoseStack(), blitX, blitY, iconFrameSize, iconBgColour);
                    GuiComponent.drawCenteredString(evt.getPoseStack(), MC.font, "+" + numExtraUnits,
                            blitX + iconFrameSize/2, blitY + 8, 0xFFFFFF);

                    if (plusUnitsZone.isMouseOver(mouseX, mouseY)) {
                        List<FormattedCharSequence> tooltipLines = new ArrayList<>();
                        int numUnits = 0;

                        for (int i = units.size() - numExtraUnits; i < units.size(); i++) {

                            LivingEntity unit = units.get(i);
                            LivingEntity nextUnit = null;
                            String unitName = HudClientEvents.getSimpleEntityName(unit);
                            String nextUnitName = null;
                            numUnits += 1;

                            if (i < units.size() - 1) {
                                nextUnit = units.get(i + 1);
                                nextUnitName = HudClientEvents.getSimpleEntityName(nextUnit);
                            }
                            if (!unitName.equals(nextUnitName)) {
                                tooltipLines.add(FormattedCharSequence.forward("x" + numUnits + " " + unitName, Style.EMPTY));
                                numUnits = 0;
                            }
                        }
                        MyRenderer.renderTooltip(evt.getPoseStack(), tooltipLines, mouseX, mouseY);
                    }
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
        ArrayList<LivingEntity> selUnits = UnitClientEvents.getSelectedUnits();
        if (selUnits.size() > 0 &&
            getPlayerToEntityRelationship(selUnits.get(0)) == Relationship.OWNED) {

            blitX = 0;
            blitY = screenHeight - iconFrameSize;

            ArrayList<Button> genericActionButtons = hudSelectedEntity instanceof VillagerUnit ?
                    genericActionButtonsWorker : genericActionButtonsAttacker;

            for (Button actionButton : genericActionButtons) {

                // GATHER button does not have a static icon
                if (actionButton == ActionButtons.GATHER && hudSelectedEntity instanceof WorkerUnit workerUnit) {
                    switch(workerUnit.getGatherResourceGoal().getTargetResourceName()) {
                        case NONE -> actionButton.iconResource = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/no_gather.png");
                        case FOOD -> actionButton.iconResource = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/hoe.png");
                        case WOOD -> actionButton.iconResource =  new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/axe.png");
                        case ORE -> actionButton.iconResource = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/pickaxe.png");
                    }
                    actionButton.tooltipLines = List.of(
                        FormattedCharSequence.forward("Gather Resources (" + UnitClientEvents.getSelectedUnitResourceTarget() + ")", Style.EMPTY),
                        FormattedCharSequence.forward("Click to change target resource", Style.EMPTY));
                }
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
                hudZones.add(MyRenderer.renderFrameWithBg(evt.getPoseStack(), blitX + iconFrameSize - 1, blitY,
                        49,
                        iconFrameSize,
                        frameBgColour));
                hudZones.add(MyRenderer.renderIconFrameWithBg(evt.getPoseStack(), blitX, blitY, iconFrameSize, iconBgColour));
                MyRenderer.renderIcon(evt.getPoseStack(),
                        new ResourceLocation(ReignOfNether.MOD_ID, rlPath),
                        blitX+4, blitY+4, iconSize
                );
                GuiComponent.drawCenteredString(evt.getPoseStack(), MC.font, resValueStr,
                        blitX + (iconFrameSize) + 24 , blitY + (iconSize / 2) + 1, 0xFFFFFF);
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

        // ---------------------
        // Control group buttons
        // ---------------------

        blitX = 100;
        // clean up untracked entities/buildings from control groups
        for (ControlGroup controlGroup : controlGroups) {
            controlGroup.clean();

            if (!controlGroup.isEmpty()) {
                Button ctrlGroupButton = controlGroup.getButton();
                ctrlGroupButton.render(evt.getPoseStack(), blitX, 0, mouseX, mouseY);
                renderedButtons.add(ctrlGroupButton);
                blitX += iconFrameSize;
            }
        }

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

    public static boolean isMouseOverAnyButtonOrHud() {
        for (RectZone hudZone : hudZones)
            if (hudZone.isMouseOver(mouseX, mouseY))
                return true;
        if (MinimapClientEvents.isPointInsideMinimap(mouseX, mouseY))
            return true;
        return isMouseOverAnyButton();
    }

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {

        for (Button button : renderedButtons) {
            if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1)
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), true);
            else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2)
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), false);
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            mouseLeftDownX = (int) evt.getMouseX();
            mouseLeftDownY = (int) evt.getMouseY();
        }
    }

    // for some reason some bound vanilla keys like Q and E don't trigger KeyPressed but still trigger keyReleased
    @SubscribeEvent
    public static void onKeyRelease(ScreenEvent.KeyReleased.KeyReleased.Post evt) {
        for (Button button : renderedButtons)
            button.checkPressed(evt.getKeyCode());
    }

    // remove effects clientside when in orthoview to stop it rendering
    @SubscribeEvent
    public static void onRenderMobEffects(ScreenEvent.RenderInventoryMobEffects evt) {
        if (OrthoviewClientEvents.isEnabled())
            evt.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent evt) {

        if (OrthoviewClientEvents.isEnabled())
            portraitRendererUnit.tickAnimation();

        // move camera to unit or building when its portrait is clicked/held on
        if (MiscUtil.isLeftClickDown(MC)) {
            if (buildingPortraitZone != null &&
                buildingPortraitZone.isMouseOver(mouseX, mouseY) &&
                buildingPortraitZone.isMouseOver(mouseLeftDownX, mouseLeftDownY) &&
                MC.player != null) {
                Building selBuilding = BuildingClientEvents.getSelectedBuilding();
                BlockPos pos = BuildingUtils.getCentrePos(selBuilding.getBlocks());
                OrthoviewClientEvents.centreCameraOnPos(pos.getX(), pos.getZ());
            }
            else if (unitPortraitZone != null &&
                    unitPortraitZone.isMouseOver(mouseX, mouseY) &&
                    unitPortraitZone.isMouseOver(mouseLeftDownX, mouseLeftDownY) &&
                    MC.player != null) {
                OrthoviewClientEvents.centreCameraOnPos(hudSelectedEntity.getX(), hudSelectedEntity.getZ());
            }
        }
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

    // MANAGE CONTROL GROUPS
    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.KeyPressed.Pre evt) {
        // prevent spectator mode options from showing up
        if (OrthoviewClientEvents.isEnabled()) {
            for (Keybinding numKey : Keybindings.nums)
                if (numKey.key == evt.getKeyCode())
                    evt.setCanceled(true);
        }

        // deselect everything
        if (evt.getKeyCode() == Keybindings.getFnum(1).key) {
            UnitClientEvents.setSelectedUnits(new ArrayList<>());
            BuildingClientEvents.setSelectedBuilding(null);
            BuildingClientEvents.setBuildingToPlace(null);
        }

        // initialise with empty arrays
        if (controlGroups.size() <= 0)
            for (Keybinding keybinding : Keybindings.nums)
                controlGroups.add(new ControlGroup());

        for (Keybinding keybinding : Keybindings.nums) {
            int index = Integer.parseInt(keybinding.buttonLabel);

            // loadToSelected is handled by renderedButtons
            if (Keybindings.ctrlMod.isDown() && evt.getKeyCode() == keybinding.key)
                controlGroups.get(index).saveFromSelected(keybinding);
        }
    }
}
