package com.solegendary.reignofnether.units;

import com.solegendary.reignofnether.cursor.CursorClientVanillaEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientVanillaEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class UnitClientVanillaEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseClickedEvent.Post evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        ArrayList<Integer> selectedUnitIds = UnitCommonVanillaEvents.getSelectedUnitIds();
        ArrayList<Integer> preselectedUnitIds = UnitCommonVanillaEvents.getPreselectedUnitIds();

        // Can only detect clicks client side but only see and modify goals serverside so produce entity queues here
        // and consume in onWorldTick; we also can't add entities directly as they will not have goals populated

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            if (selectedUnitIds.size() > 0) {
                // A + left click -> force attack single unit (even if friendly)
                if (CursorClientVanillaEvents.getAttackFlag() && preselectedUnitIds.size() == 1 &&
                        !UnitCommonVanillaEvents.targetingSelf())
                    UnitCommonVanillaEvents.setUnitIdToAttack(preselectedUnitIds.get(0));
                // A + left click -> attack move ground
                else if (CursorClientVanillaEvents.getAttackFlag()) {
                    ArrayList<Integer> unitIdsToAttackMove = new ArrayList<>();
                    unitIdsToAttackMove.addAll(selectedUnitIds);
                    UnitCommonVanillaEvents.setUnitIdsToAttackMove(unitIdsToAttackMove);
                }
            }
            // left click -> (de)select a single unit
            // if shift is held, deselect a unit or add it to the selected group
            if (preselectedUnitIds.size() == 1 && !CursorClientVanillaEvents.getAttackFlag()) {
                if (Keybinds.shiftMod.isDown()) {
                    if (!selectedUnitIds.removeIf(id -> id.equals(preselectedUnitIds.get(0))))
                        if (MC.level.getEntity(preselectedUnitIds.get(0)) instanceof Unit)
                            selectedUnitIds.add(preselectedUnitIds.get(0));
                }
                else {
                    selectedUnitIds = new ArrayList<>();
                    if (MC.level.getEntity(preselectedUnitIds.get(0)) instanceof Unit)
                        selectedUnitIds.add(preselectedUnitIds.get(0));
                }
            }
            CursorClientVanillaEvents.removeAttackFlag();
        }
        else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            if (selectedUnitIds.size() > 0) {
                // right click -> attack unfriendly unit
                if (preselectedUnitIds.size() == 1 &&
                        !UnitCommonVanillaEvents.targetingSelf() &&
                        !UnitCommonVanillaEvents.isUnitFriendly(preselectedUnitIds.get(0)))
                    UnitCommonVanillaEvents.setUnitIdToAttack(preselectedUnitIds.get(0));
                // right click -> follow friendly unit
                else if (preselectedUnitIds.size() == 1 && !UnitCommonVanillaEvents.targetingSelf())
                    UnitCommonVanillaEvents.setUnitIdToFollow(preselectedUnitIds.get(0));
                // right click -> move to ground pos (and disable during camera manip)
                else if (!Keybinds.altMod.isDown()) {
                    ArrayList<Integer> unitIdsToMove = new ArrayList<>();
                    unitIdsToMove.addAll(selectedUnitIds);
                    UnitCommonVanillaEvents.setUnitIdsToMove(unitIdsToMove);
                }
            }
            CursorClientVanillaEvents.removeAttackFlag();
        }

        // update values in UnitCommonVanillaEvents
        UnitCommonVanillaEvents.setSelectedUnitIds(selectedUnitIds);
        UnitCommonVanillaEvents.setPreselectedUnitIds(preselectedUnitIds);
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelLastEvent evt) {
        if (MC.level != null && OrthoviewClientVanillaEvents.isEnabled()) {

            ArrayList<Integer> selectedUnitIds = UnitCommonVanillaEvents.getSelectedUnitIds();
            ArrayList<Integer> preselectedUnitIds = UnitCommonVanillaEvents.getPreselectedUnitIds();

            Set<Integer> unitIdsToDraw = new HashSet<>();
            unitIdsToDraw.addAll(selectedUnitIds);
            unitIdsToDraw.addAll(preselectedUnitIds);

            // draw outlines on all (pre)selected units but only draw once per unit based on conditions
            for (int idToDraw : unitIdsToDraw) {
                Entity entity = MC.level.getEntity(idToDraw);
                if (entity != null) {
                    if (preselectedUnitIds.contains(idToDraw) &&
                            CursorClientVanillaEvents.getAttackFlag() &&
                            !UnitCommonVanillaEvents.targetingSelf())
                        MyRenderer.drawEntityOutline(evt.getPoseStack(), entity, 1.0f, 0.3f,0.3f, 1.0f);
                    else if (selectedUnitIds.contains(idToDraw))
                        MyRenderer.drawEntityOutline(evt.getPoseStack(), entity, 1.0f);
                    else if (preselectedUnitIds.contains(idToDraw))
                        MyRenderer.drawEntityOutline(evt.getPoseStack(), entity, 0.5f);
                }
            }
        }
    }
}
