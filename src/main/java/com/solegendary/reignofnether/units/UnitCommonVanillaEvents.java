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
    public static void setUnitIdsToMove(ArrayList<Integer> unitIds) { unitIdsToMove = unitIds; }
    public static void setUnitIdsToAttackMove(ArrayList<Integer> unitIds) { unitIdsToAttackMove = unitIds; }


    // TODO: consider changing PathfinderMob to Unit?

    // note this seems to fire twice per entity, once serverside and once clientside
    // the clientside entity has no goals registered
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent evt) {
        Entity entity = evt.getEntity();
    }
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveWorldEvent evt) {
        int entityId = evt.getEntity().getId();
        preselectedUnitIds.removeIf(e -> e == entityId);
        selectedUnitIds.removeIf(e -> e == entityId);
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
            // deselect all units
            if (Keybinds.keyF1.isDown())
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
