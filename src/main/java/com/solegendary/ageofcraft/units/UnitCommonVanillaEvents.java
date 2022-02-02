package com.solegendary.ageofcraft.units;

import com.solegendary.ageofcraft.cursor.CursorClientVanillaEvents;
import com.solegendary.ageofcraft.orthoview.OrthoviewClientVanillaEvents;
import com.solegendary.ageofcraft.util.MyRenderer;
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

import java.util.ArrayList;

public class UnitCommonVanillaEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    // units moused over or inside a box select
    private static ArrayList<PathfinderMob> preselectedUnits = new ArrayList<>();
    // units selected by click or box select
    private static ArrayList<PathfinderMob> selectedUnits = new ArrayList<>();
    // unit targeted by a right click for attack or follow
    private static int targetedUnitId = -1; // QUEUE
    private static ArrayList<Integer> unitIdsToMove = new ArrayList<>(); // QUEUE
    private static ArrayList<Integer> unitIdsWithAttackFlag = new ArrayList<>(); // QUEUE

    public static ArrayList<PathfinderMob> getPreselectedUnits() { return preselectedUnits; }
    public static ArrayList<PathfinderMob> getSelectedUnits() {
        return selectedUnits;
    }
    public static void addPreselectedUnit(PathfinderMob unit) { preselectedUnits.add(unit); }
    public static void addSelectedUnit(PathfinderMob unit) { selectedUnits.add(unit); }
    public static void setPreselectedUnits(ArrayList<PathfinderMob> units) {
        preselectedUnits = units;
    }
    public static void setSelectedUnits(ArrayList<PathfinderMob> units) { selectedUnits = units; }
    public static int getTargetedUnitId() { return targetedUnitId; }

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
        preselectedUnits.removeIf(e -> e.getId() == entityId);
        selectedUnits.removeIf(e -> e.getId() == entityId);
    }

    @SubscribeEvent
    public static void onMouseClick(GuiScreenEvent.MouseClickedEvent.Post evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        // Can only detect clicks client side but only see and modify goals serverside so produce entity queues here
        // and consume in onWorldTick; we also can't add entities directly as they will not have goals populated

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) { // left click
            if (preselectedUnits.size() == 1)
                targetedUnitId = preselectedUnits.get(0).getId();
            else {
                unitIdsWithAttackFlag = new ArrayList<>();
                for (PathfinderMob unit : selectedUnits)
                    unitIdsWithAttackFlag.add(unit.getId());
            }
        }
        else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) { // right click
            // prioritise attacks and don't both attack and move
            if (preselectedUnits.size() == 1)
                targetedUnitId = preselectedUnits.get(0).getId();
            else {
                unitIdsToMove = new ArrayList<>();
                for (PathfinderMob unit : selectedUnits)
                    unitIdsToMove.add(unit.getId());
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        ServerLevel world = (ServerLevel) evt.world;

        // Consume list produced in clientside events like onMouseClick; remember to always get the serverside
        // entity first via the ID or else goals are not able to be manipulated (and also so we can cast to Unit)
        if (!world.isClientSide()) {

            // right-click move
            for (int id : unitIdsToMove) {
                Unit unit = (Unit) world.getEntity(id);
                if (unit != null)
                    unit.setMoveTarget(CursorClientVanillaEvents.getPreselectedBlockPos());
            }
            unitIdsToMove = new ArrayList<>();

            // left-click attackmove
            for (int id : unitIdsWithAttackFlag) {
                Unit unit = (Unit) world.getEntity(id);
                if (unit != null)
                    unit.setAttackMoveTarget(CursorClientVanillaEvents.getPreselectedBlockPos());
                CursorClientVanillaEvents.removeAttackFlag();
            }
            unitIdsToMove = new ArrayList<>();

            ArrayList<Integer> selectedUnitIds = new ArrayList<>();
            for (PathfinderMob mob : selectedUnits)
                selectedUnitIds.add(mob.getId());

            if (targetedUnitId >= 0) {
                for (PathfinderMob mob : selectedUnits) {

                    if (targetedUnitId != mob.getId() && CursorClientVanillaEvents.getAttackFlag()) {
                        Unit unit = (Unit) world.getEntity(mob.getId());
                        if (unit != null)
                            unit.setAttackTarget((LivingEntity) world.getEntity(targetedUnitId));
                    }
                    // if targeting a friendly, move to them instead of attacking, except if attackFlag was set
                    else if (selectedUnitIds.contains(targetedUnitId)) {
                        Unit unit = (Unit) world.getEntity(mob.getId());
                        if (unit != null)
                            unit.setMoveTarget(CursorClientVanillaEvents.getPreselectedBlockPos());
                    }
                    // prevent units targeting themselves or friendlies
                    else if (targetedUnitId != mob.getId()) {
                        Unit unit = (Unit) world.getEntity(mob.getId());
                        if (unit != null)
                            unit.setAttackTarget((LivingEntity) world.getEntity(targetedUnitId));
                    }
                }
                targetedUnitId = -1;
                CursorClientVanillaEvents.removeAttackFlag();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldLastEvent evt) {
        if (MC.level != null && OrthoviewClientVanillaEvents.isEnabled()) {
            for (PathfinderMob unit : selectedUnits)
                MyRenderer.drawEntityOutline(evt.getMatrixStack(), unit, 1.0f);
            for (PathfinderMob unit : preselectedUnits)
                MyRenderer.drawEntityOutline(evt.getMatrixStack(), unit,0.5f);

            // TODO: this is only active for 1 frame... probably just check isRightClickDown and render on preselectedUnits
            //if (targetedUnitId >= 0)
            //    MyRenderer.drawEntityOutline(evt.getMatrixStack(), evt.get(targetedUnitId), 1.0f, 0, 0 ,0.5f);
        }
    }
}
