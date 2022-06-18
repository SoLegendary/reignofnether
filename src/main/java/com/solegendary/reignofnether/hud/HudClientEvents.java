package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.units.Relationship;
import com.solegendary.reignofnether.units.Unit;
import com.solegendary.reignofnether.units.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class HudClientEvents {

    private static final ResourceLocation[] TEXTURE_STAT_ICONS = {
            new ResourceLocation("reignofnether", "textures/icons/items/sword.png"), // DAMAGE
            new ResourceLocation("reignofnether", "textures/icons/items/sparkler.png"), // ATTACK SPEED
            new ResourceLocation("reignofnether", "textures/icons/items/bow.png"), // RANGE
            new ResourceLocation("reignofnether", "textures/icons/items/chestplate.png"), // ARMOUR
            new ResourceLocation("reignofnether", "textures/icons/items/boots.png"), // MOVE SPEED
    };

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
    // private class used to render only the head of a unit on screen for the portrait
    public static PortraitRenderer portraitRenderer = new PortraitRenderer(null);

    // where to start drawing the centre hud (from left to right: portrait, stats, unit icon buttons)
    private static int hudStartingXPos = 0;

    // if we are rendering > this amount, then just render an empty icon with +N for the remaining units
    private static final int unitButtonsPerRow = 8;

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
    public static void onDrawScreen(ScreenEvent.DrawScreenEvent evt) {
        String screenName = evt.getScreen().getTitle().getString();
        if (!OrthoviewClientEvents.isEnabled() || !screenName.equals("topdowngui_container"))
            return;
        if (MC.level == null)
            return;

        hudStartingXPos = (MC.getWindow().getGuiScaledWidth() / 5) + 4;

        mouseX = evt.getMouseX();
        mouseY = evt.getMouseY();

        ArrayList<LivingEntity> units = UnitClientEvents.getSelectedUnits();

        // create all of the unit buttons for this frame
        int screenHeight = MC.getWindow().getGuiScaledHeight();

        int iconSize = 14;
        int iconFrameSize = Button.iconFrameSize;

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

        // --------------------------------------------------------
        // Unit head portrait (based on selected unit type) + stats
        // --------------------------------------------------------
        int blitX = hudStartingXPos;
        int blitY = MC.getWindow().getGuiScaledHeight() - portraitRenderer.frameHeight;

        if (hudSelectedEntity != null &&
            portraitRenderer.model != null &&
            portraitRenderer.renderer != null) {

            // write capitalised unit name
            String name = HudClientEvents.getSimpleEntityName(hudSelectedEntity);
            String nameCap = name.substring(0, 1).toUpperCase() + name.substring(1);
            GuiComponent.drawString(
                    evt.getPoseStack(), Minecraft.getInstance().font,
                    nameCap,
                    blitX+4,blitY-9,
                    0xFFFFFFFF
            );

            portraitRenderer.renderWithFrame(
                    evt.getPoseStack(), blitX, blitY,
                    hudSelectedEntity);

            // draw unit stats
            if (hudSelectedEntity instanceof Unit) {
                blitX += portraitRenderer.frameWidth - 2;
                MyRenderer.renderFrameWithBg(evt.getPoseStack(), blitX, blitY,
                        43,
                        portraitRenderer.frameHeight,
                        0xA0000000);

                int blitXIcon = blitX + 6;
                int blitYIcon = blitY + 7;
                for (int i = 0; i < TEXTURE_STAT_ICONS.length; i++) {
                    MyRenderer.renderIcon(
                            evt.getPoseStack(),
                            TEXTURE_STAT_ICONS[i],
                            blitXIcon, blitYIcon, 8
                    );
                    String statString = "";

                    Unit unit = (Unit) hudSelectedEntity;
                    switch (i) {
                        case 0 -> statString = String.valueOf((int) unit.getDamage()); // DAMAGE
                        case 1 -> statString = String.valueOf((int) (100 / unit.getAttackCooldown())); // ATTACK SPEED
                        case 2 -> statString = String.valueOf((int) (unit.getAttackRange())); // RANGE
                        case 3 -> statString = String.valueOf(hudSelectedEntity.getArmorValue()); // ARMOUR
                        case 4 -> statString = String.valueOf((int) (unit.getSpeedModifier() * 100)); // MOVE SPEED
                    }
                    GuiComponent.drawString(evt.getPoseStack(), MC.font, statString, blitXIcon + 13, blitYIcon, 0xFFFFFF);
                    blitYIcon += 10;
                }
            }
        }

        // ----------------------------------------------
        // Unit icons using mob heads on 2 rows if needed
        // ----------------------------------------------
        int buttonsRendered = 0;
        blitX += portraitRenderer.frameWidth + 20;
        int blitXStart = blitX;
        blitY = screenHeight - iconFrameSize;
        if (unitButtons.size() > unitButtonsPerRow)
            blitY -= iconFrameSize + 5;

        for (Button unitButton : unitButtons) {
            // replace last icon with a +X number of units icon
            if (buttonsRendered == (unitButtonsPerRow * 2) - 1 &&
                    units.size() > (unitButtonsPerRow * 2)) {
                int numExtraUnits = units.size() - (unitButtonsPerRow * 2) + 1;
                MyRenderer.renderIconFrameWithBg(evt.getPoseStack(), blitX, blitY, iconFrameSize, 0x64000000);
                GuiComponent.drawCenteredString(evt.getPoseStack(), MC.font, "+" + numExtraUnits,
                        blitX + 8, blitY + 8, 0xFFFFFF);
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

        // -------------------------------------------------------
        // Unit action icons (attack, stop, move, abilities etc.)
        // -------------------------------------------------------

        ArrayList<Integer> selUnitIds = UnitClientEvents.getSelectedUnitIds();
        if (selUnitIds.size() > 0 &&
            UnitClientEvents.getPlayerToEntityRelationship(selUnitIds.get(0)) == Relationship.OWNED) {

            blitX = 0;
            blitY = screenHeight - iconFrameSize;
            for (Button actionButton : genericActionButtons) {
                actionButton.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
                actionButton.checkPressed();
                blitX += iconFrameSize;
            }
            blitX = 0;
            blitY = screenHeight - (iconFrameSize * 2);
            for (LivingEntity unit : units) {
                if (getSimpleEntityName(unit).equals(getSimpleEntityName(hudSelectedEntity))) {
                    for (AbilityButton ability : ((Unit) unit).getAbilities()) {
                        ability.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
                        ability.checkPressed();
                        blitX += iconFrameSize;
                    }
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseReleasedEvent.Post evt) {
        int mouseX = (int) evt.getMouseX();
        int mouseY = (int) evt.getMouseY();

        ArrayList<Button> buttons = new ArrayList<>();
        buttons.addAll(genericActionButtons);
        buttons.addAll(unitButtons);

        for (Button button : buttons)
            button.checkClicked(mouseX, mouseY);
    }

    @SubscribeEvent
    // hudSelectedUnit and portraitRenderer should be assigned in the same event to avoid desyncs
    public static void onRenderLivingEntity(RenderLivingEvent.Pre<? extends LivingEntity, ? extends Model> evt) {

        ArrayList<LivingEntity> units = UnitClientEvents.getSelectedUnits();

        // sort and hudSelect the first unit type in the list
        units.sort(Comparator.comparing(HudClientEvents::getSimpleEntityName));

        if (units.size() <= 0)
            hudSelectedEntity = null;
        else if (hudSelectedEntity == null || units.size() == 1)
            hudSelectedEntity = units.get(0);

        if (hudSelectedEntity == null) {
            portraitRenderer.model = null;
            portraitRenderer.renderer = null;
        }
        else if (evt.getEntity() == hudSelectedEntity) {
            portraitRenderer.model = evt.getRenderer().getModel();
            portraitRenderer.renderer = evt.getRenderer();
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent evt) {
        if (OrthoviewClientEvents.isEnabled())
            portraitRenderer.tickAnimation();
    }

    // uncomment to adjust render position/size of portraits

    @SubscribeEvent
    public static void onInput(InputEvent.KeyInputEvent evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions
            if (evt.getKey() == Keybinds.panMinusX.getKey().getValue())
                portraitRenderer.headOffsetX += 1;
            if (evt.getKey() == Keybinds.panPlusX.getKey().getValue())
                portraitRenderer.headOffsetX -= 1;
            if (evt.getKey() == Keybinds.panMinusZ.getKey().getValue())
                portraitRenderer.headOffsetY += 1;
            if (evt.getKey() == Keybinds.panPlusZ.getKey().getValue())
                portraitRenderer.headOffsetY -= 1;

            if (evt.getKey() == Keybinds.nums[9].getKey().getValue())
                portraitRenderer.headSize -= 1;
            if (evt.getKey() == Keybinds.nums[0].getKey().getValue())
                portraitRenderer.headSize += 1;
        }
    }
    @SubscribeEvent
    public static void onRenderOverLay(RenderGameOverlayEvent.Pre evt) {
        if (hudSelectedEntity != null)
            MiscUtil.drawDebugStrings(evt.getMatrixStack(), MC.font, new String[] {
                    "headOffsetX: " + portraitRenderer.headOffsetX,
                    "headOffsetY: " + portraitRenderer.headOffsetY,
                    "headSize: " + portraitRenderer.headSize,
                    "eyeHeight: " + hudSelectedEntity.getEyeHeight(),
            });
    }
}
