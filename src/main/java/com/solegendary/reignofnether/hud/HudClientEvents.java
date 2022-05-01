package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.units.Unit;
import com.solegendary.reignofnether.units.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

public class HudClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    private static ArrayList<Button> unitButtons = new ArrayList<>();
    private static final ArrayList<Button> actionButtons = new ArrayList<>(Arrays.asList(
            ActionButtons.attack,
            ActionButtons.stop,
            ActionButtons.hold,
            ActionButtons.move
    ));
    // unit type that is selected in the list of unit icons
    private static Entity hudSelectedUnitClass = null;

    // if we are rendering > this amount, then just render an empty icon with +N for the remaining units
    private static final int maxUnitButtons = 8;

    // eg. entity.reignofnether.zombie_unit -> zombie
    private static String getSimpleUnitName(Entity unit) {
        return unit.getName().getString()
            .replace(" ","")
            .replace("entity.reignofnether.","")
            .replace("_unit","");
    }

    public static void drawEntityOnScreen(PoseStack matrixStack2, int x, int y, int size, float mouseX,
                                  float mouseY, LivingEntity entity, float scale) {
        float f = (float) Math.atan((double) (mouseX / 40.0F));
        float g = (float) Math.atan((double) (mouseY / 40.0F));
        PoseStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushPose();
        matrixStack.translate((double) x * scale, (double) y * scale, 1050.0D * scale);
        matrixStack.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        matrixStack2.pushPose();
        matrixStack2.translate(0.0D, 0.0D, 1000.0D);
        matrixStack2.scale((float) size, (float) size, (float) size);
        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternion2 = Vector3f.XP.rotationDegrees(g * 20.0F);
        quaternion.mul(quaternion2);
        matrixStack2.mulPose(quaternion);
        float h = entity.yBodyRot; // bodyYaw;
        float i = entity.getYRot(); // getYaw();
        float j = entity.getXRot(); // getPitch();
        float k = entity.yHeadRotO; // prevHeadYaw;
        float l = entity.yHeadRot; // headYaw;
        entity.yBodyRot = 180.0F + f * 20.0F;
        entity.setYRot(180.0F + f * 40.0F);
        entity.setXRot(-g * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher =
                Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion2.conj();
        entityrenderdispatcher.overrideCameraOrientation(quaternion2);
        entityrenderdispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource immediate =
                Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.runAsFancy(() -> {
            entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrixStack2, immediate,
                    15728880);
        });

        immediate.endBatch();
        entityrenderdispatcher.setRenderShadow(true);
        entity.yBodyRot = h;
        entity.setYRot(i);
        entity.setXRot(j);
        entity.yHeadRotO = k;
        entity.yHeadRot = l;
        matrixStack.popPose();
        matrixStack2.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }

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

        // sort and hudSelect the first unit type in the list
        units.sort(Comparator.comparing(HudClientEvents::getSimpleUnitName));

        if (units.size() <= 0)
            hudSelectedUnitClass = null;
        else if (hudSelectedUnitClass == null)
            hudSelectedUnitClass = units.get(0);

        // create all of the unit buttons for this frame
        int screenWidth = MC.getWindow().getGuiScaledWidth();
        int screenHeight = MC.getWindow().getGuiScaledHeight();

        int iconSize = 14;
        int iconFrameSize = Button.iconFrameSize;

        for (LivingEntity unit : units) {

            if (unitButtons.size() < maxUnitButtons) {
                // mob head icon
                String unitName = getSimpleUnitName(unit);

                unitButtons.add(new Button(
                        unitName,
                        iconSize,
                        "textures/mobheads/" + unitName + ".png",
                        unit,
                        () -> getSimpleUnitName(hudSelectedUnitClass).equals(unitName),
                        () -> {
                            // click to select this unit type as a group
                            if (getSimpleUnitName(hudSelectedUnitClass).equals(unitName)) {
                                UnitClientEvents.setSelectedUnitIds(new ArrayList<>());
                                UnitClientEvents.addSelectedUnitId(unit.getId());
                            } else { // select this one specific unit
                                hudSelectedUnitClass = unit;
                            }
                        }
                ));
            }
        }

        // ---------------------------
        // Unit icons using mob heads
        // ---------------------------
        int numUnitButtons = Math.min(units.size(), maxUnitButtons);
        int blitX = (screenWidth / 2) - (numUnitButtons * iconFrameSize / 2);
        int blitY = screenHeight - iconFrameSize;

        for (Button unitButton : unitButtons) {
            unitButton.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
            unitButton.renderHealthBar(evt.getPoseStack());
            blitX += iconFrameSize;
        }

        // ------------------------------------------------
        // Unit head portrait (based on selected unit type)
        // ------------------------------------------------


        if (hudSelectedUnitClass != null) {
            MiscUtil.drawDebugStrings(evt.getPoseStack(), MC.font, new String[] {
                    "hudSelectedUnitType: " + getSimpleUnitName(hudSelectedUnitClass)
            });

            if (getSimpleUnitName(hudSelectedUnitClass).toLowerCase(Locale.ROOT).contains("skeleton")) {

                /*
                // icon frame
                ResourceLocation iconFrameResource = new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/unit_frame.png");
                RenderSystem.setShaderTexture(0, iconFrameResource);
                GuiComponent.blit(evt.getPoseStack(),
                        0,0, 0,
                        0,0, // where on texture to start drawing from
                        42, 42, // dimensions of blit texture
                        42, 42 // size of texture itself (if < dimensions, texture is repeated)
                );

                drawEntityOnScreen(evt.getPoseStack(), 0,0, 1, evt.getMouseX(),  evt.getMouseY(), (LivingEntity) hudSelectedUnitClass, 1.0f);
                 */
            }
        }



        // -------------------------------------------------------
        // Unit action icons (attack, stop, move, abilities etc.)
        // -------------------------------------------------------

        if (UnitClientEvents.getSelectedUnitIds().size() > 0) {
            blitX = 0;
            blitY = screenHeight - iconFrameSize;
            for (Button actionButton : actionButtons) {
                actionButton.render(evt.getPoseStack(), blitX, blitY, mouseX, mouseY);
                actionButton.checkPressed();
                blitX += iconFrameSize;
            }
            blitX = 0;
            blitY = screenHeight - (iconFrameSize * 2);
            for (LivingEntity unit : units) {
                if (getSimpleUnitName(unit).equals(getSimpleUnitName(hudSelectedUnitClass))) {
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
        buttons.addAll(actionButtons);
        buttons.addAll(unitButtons);

        for (Button button : buttons)
            button.checkClicked(mouseX, mouseY);
    }
}
