package com.solegendary.ageofcraft.units;

import com.solegendary.ageofcraft.cursor.CursorClientVanillaEvents;
import com.solegendary.ageofcraft.orthoview.OrthoviewClientVanillaEvents;
import com.solegendary.ageofcraft.registrars.Keybinds;
import com.solegendary.ageofcraft.util.MyRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class UnitCommonVanillaEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    // units moused over or inside a box select
    private static ArrayList<Integer> preselectedUnitIds = new ArrayList<>();
    // units selected by click or box select
    private static ArrayList<Integer> selectedUnitIds = new ArrayList<>();
    // unit targeted by a right click for attack or follow
    private static int unitIdToAttack = -1; // QUEUED - selected units attack this unit
    private static int unitIdToFollow = -1; // QUEUED - selected units follow this unit
    private static ArrayList<Integer> unitIdsToMove = new ArrayList<>(); // QUEUED - these units move to cursorBlockPos
    private static ArrayList<Integer> unitIdsToAttackMove = new ArrayList<>(); // QUEUED - these units attack move to cursorBlockPos
    private static ArrayList<ArrayList<Integer>> controlGroups = new ArrayList<>(10);

    public static ArrayList<Integer> getPreselectedUnitIds() { return preselectedUnitIds; }
    public static ArrayList<Integer> getSelectedUnitIds() {
        return selectedUnitIds;
    }
    public static void addPreselectedUnitId(Integer unitId) { preselectedUnitIds.add(unitId); }
    public static void addSelectedUnitId(Integer unitId) { selectedUnitIds.add(unitId); }
    public static void setPreselectedUnitIds(ArrayList<Integer> unitIds) { preselectedUnitIds = unitIds; }
    public static void setSelectedUnitIds(ArrayList<Integer> unitIds) { selectedUnitIds = unitIds; }
    public static int getUnitIdToAttack() { return unitIdToAttack; }
    public static int getUnitIdToFollow() { return unitIdToFollow; }
    public static void setUnitIdToAttack(int id) { unitIdToAttack = id; }
    public static void setUnitIdToFollow(int id) { unitIdToFollow = id; }


    // TODO: consider changing PathfinderMob to Unit?

    // note this seems to fire twice per entity, once serverside and once clientside
    // the clientside entity has no goals registered
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent evt) {
        Entity entity = evt.getEntity();

        // TODO: replace all vanilla mobs registered as units with our version, eg. Skeleton -> SkeletonUnit
    }
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveWorldEvent evt) {
        int entityId = evt.getEntity().getId();
        preselectedUnitIds.removeIf(e -> e == entityId);
        selectedUnitIds.removeIf(e -> e == entityId);
    }

    @SubscribeEvent
    public static void onMouseClick(GuiScreenEvent.MouseClickedEvent.Post evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        // Can only detect clicks client side but only see and modify goals serverside so produce entity queues here
        // and consume in onWorldTick; we also can't add entities directly as they will not have goals populated

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            if (selectedUnitIds.size() > 0) {
                // A + left click -> force attack single unit (even if friendly)
                if (CursorClientVanillaEvents.getAttackFlag() && preselectedUnitIds.size() == 1 && !targetingSelf())
                    unitIdToAttack = preselectedUnitIds.get(0);
                // A + left click -> attack move ground
                else if (CursorClientVanillaEvents.getAttackFlag()) {
                    unitIdsToAttackMove = new ArrayList<>();
                    unitIdsToAttackMove.addAll(selectedUnitIds);
                }
            }
            // left click -> (de)select a single unit
            if (preselectedUnitIds.size() == 1 && !CursorClientVanillaEvents.getAttackFlag()) {
                if (!Keybinds.shiftMod.isDown())
                    selectedUnitIds = new ArrayList<>();
                else
                    selectedUnitIds.removeIf(id -> id.equals(preselectedUnitIds.get(0)));
                if (!selectedUnitIds.contains(preselectedUnitIds.get(0)) &&
                     MC.level.getEntity(preselectedUnitIds.get(0)) instanceof Unit)
                    selectedUnitIds.add(preselectedUnitIds.get(0));

            }
            CursorClientVanillaEvents.removeAttackFlag();
        }
        else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            if (selectedUnitIds.size() > 0) {
                // right click -> attack unfriendly unit
                if (preselectedUnitIds.size() == 1 && !targetingSelf() && !isUnitFriendly(preselectedUnitIds.get(0)))
                    unitIdToAttack = preselectedUnitIds.get(0);
                // right click -> follow friendly unit
                else if (preselectedUnitIds.size() == 1 && !targetingSelf())
                    unitIdToFollow = preselectedUnitIds.get(0);
                // right click -> move to ground pos
                else {
                    unitIdsToMove = new ArrayList<>();
                    unitIdsToMove.addAll(selectedUnitIds);
                }
            }
            CursorClientVanillaEvents.removeAttackFlag();
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        ServerLevel world = (ServerLevel) evt.world;

        // manage control groups
        if (controlGroups.size() <= 0) // initialise with empty arrays
            for (KeyMapping keyMapping : Keybinds.nums)
                controlGroups.add(new ArrayList<>());

        for (KeyMapping keyMapping : Keybinds.nums) {
            int index = Integer.parseInt(keyMapping.getKey().getDisplayName().getContents());

            if (Keybinds.ctrlMod.isDown() && keyMapping.isDown() && selectedUnitIds.size() > 0)
                controlGroups.set(index, selectedUnitIds);
            else if (keyMapping.isDown() && controlGroups.get(index).size() > 0)
                selectedUnitIds = controlGroups.get(index);
        }

        // Consume queues produced in clientside events like onMouseClick; remember to always get the serverside
        // entity first via the ID or else goals are not able to be manipulated (and also so we can cast to Unit)
        if (!world.isClientSide()) {

            // stop command
            if (Keybinds.keyS.isDown()) {
                for (int id : unitIdsToMove) {
                    Unit unit = (Unit) world.getEntity(id);
                    if (unit != null)
                        unit.resetTargets();
                }
            }
            // deselect all units on escape
            if (Keybinds.escape.isDown())
                selectedUnitIds = new ArrayList<>();

            for (int id : unitIdsToMove) {
                Unit unit = (Unit) world.getEntity(id);
                if (unit != null)
                    unit.setMoveTarget(CursorClientVanillaEvents.getPreselectedBlockPos());
            }
            unitIdsToMove = new ArrayList<>();

            for (int id : unitIdsToAttackMove) {
                Unit unit = (Unit) world.getEntity(id);
                if (unit != null)
                    unit.setAttackMoveTarget(CursorClientVanillaEvents.getPreselectedBlockPos());
            }
            unitIdsToAttackMove = new ArrayList<>();

            for (int id : selectedUnitIds) {
                Unit unit = (Unit) world.getEntity(id);
                if (unit != null) {
                    if (unitIdToAttack >= 0)
                        unit.setAttackTarget((LivingEntity) world.getEntity(unitIdToAttack));
                    if (unitIdToFollow >= 0)
                        unit.setFollowTarget((LivingEntity) world.getEntity(unitIdToFollow));
                }
            }
            unitIdToAttack = -1;
            unitIdToFollow = -1;
        }
    }

    @SubscribeEvent

    public static void onRenderWorld(RenderWorldLastEvent evt) {
        if (MC.level != null && OrthoviewClientVanillaEvents.isEnabled()) {

            Set<Integer> unitIdsToDraw = new HashSet<>();
            unitIdsToDraw.addAll(selectedUnitIds);
            unitIdsToDraw.addAll(preselectedUnitIds);

            // draw outlines on all (pre)selected units but only draw once per unit based on conditions
            for (int idToDraw : unitIdsToDraw) {
                Entity entity = MC.level.getEntity(idToDraw);
                if (entity != null) {
                    if (preselectedUnitIds.contains(idToDraw) && CursorClientVanillaEvents.getAttackFlag() && !targetingSelf())
                        MyRenderer.drawEntityOutline(evt.getMatrixStack(), entity, 1.0f, 0.3f,0.3f, 1.0f);
                    else if (selectedUnitIds.contains(idToDraw))
                        MyRenderer.drawEntityOutline(evt.getMatrixStack(), entity, 1.0f);
                    else if (preselectedUnitIds.contains(idToDraw))
                        MyRenderer.drawEntityOutline(evt.getMatrixStack(), entity, 0.5f);
                }
            }

            // TODO: this is only active for 1 frame... probably just check isRightClickDown and render on preselectedUnits
            //if (targetedUnitId >= 0)
            //    MyRenderer.drawEntityOutline(evt.getMatrixStack(), evt.get(targetedUnitId), 1.0f, 0, 0 ,0.5f);
        }
    }

    // TODO: change this later to check for the unit's player controller instead of just type
    public static boolean isUnitFriendly(int unitId) {
        if (MC.level != null) {
            return MC.level.getEntity(unitId) instanceof Unit;
        }
        return true;
    }

    public static boolean targetingSelf() {
        return selectedUnitIds.size() == 1 &&
                preselectedUnitIds.size() == 1 &&
                selectedUnitIds.get(0).equals(preselectedUnitIds.get(0));
    }
}
