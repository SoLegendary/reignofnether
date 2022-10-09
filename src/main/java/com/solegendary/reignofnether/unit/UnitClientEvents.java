package com.solegendary.reignofnether.unit;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;

public class UnitClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final ArrayList<ArrayList<Integer>> controlGroups = new ArrayList<>(10);
    // units moused over or inside a box select
    private static ArrayList<Integer> preselectedUnitIds = new ArrayList<>();
    // units selected by click or box select
    private static ArrayList<Integer> selectedUnitIds = new ArrayList<>();
    // tracking of all existing units
    private static ArrayList<Integer> allUnitIds = new ArrayList<>();
    // id for actions related to individual units (attack target, follow target, etc.)
    private static ArrayList<Integer> unitIdForAction = new ArrayList<>();
    // ids for actions related to groups of units (attack, move, etc.)
    private static ArrayList<Integer> unitIdsForAction = new ArrayList<>();


    public static ArrayList<Integer> getPreselectedUnitIds() { return preselectedUnitIds; }
    public static ArrayList<LivingEntity> getPreselectedUnits() {
        ArrayList<LivingEntity> units = new ArrayList<>();
        for (int id: UnitClientEvents.getPreselectedUnitIds())
            if (MC.level != null)
                units.add((LivingEntity) MC.level.getEntity(id));
        return units;
    }
    public static ArrayList<Integer> getSelectedUnitIds() { return selectedUnitIds; }
    public static ArrayList<LivingEntity> getSelectedUnits() {
        ArrayList<LivingEntity> units = new ArrayList<>();
        for (int id: UnitClientEvents.getSelectedUnitIds())
            if (MC.level != null)
                units.add((LivingEntity) MC.level.getEntity(id));
        return units;
    }
    public static void addPreselectedUnitId(Integer unitId) { preselectedUnitIds.add(unitId); }
    public static void addSelectedUnitId(Integer unitId) {
        selectedUnitIds.add(unitId);
        BuildingClientEvents.setSelectedBuilding(null);
    }
    public static void setPreselectedUnitIds(ArrayList<Integer> unitIds) { preselectedUnitIds = unitIds; }
    public static void setSelectedUnitIds(ArrayList<Integer> unitIds) {
        selectedUnitIds = unitIds;
        if (selectedUnitIds.size() > 0)
            BuildingClientEvents.setSelectedBuilding(null);
    }
    private static long lastLeftClickTime = 0; // to track double clicks
    private static final long doubleClickTimeMs = 500;

    private static boolean isLeftClickAttack() {
        return CursorClientEvents.getLeftClickAction() == UnitAction.ATTACK;
    }

    public static void sendUnitCommand(UnitAction specialAction) {
        PacketHandler.INSTANCE.sendToServer(new UnitServerboundPacket(
            specialAction,
            preselectedUnitIds.size() > 0 ? preselectedUnitIds.get(0) : -1,
            selectedUnitIds.stream().mapToInt(i -> i).toArray(),
            CursorClientEvents.getPreselectedBlockPos()
        ));
    }

    private static void resolveMoveAction() {
        // follow friendly unit
        if (preselectedUnitIds.size() == 1 && !targetingSelf()) {
            sendUnitCommand(UnitAction.FOLLOW);
        }
        // move to ground pos (disabled during camera manip)
        else if (!Keybinding.altMod.isDown()) {
            sendUnitCommand(UnitAction.MOVE);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent evt) {
        int entityId = evt.getEntity().getId();

        preselectedUnitIds.removeIf(e -> e == entityId);
        selectedUnitIds.removeIf(e -> e == entityId);
        allUnitIds.removeIf(e -> e == entityId);

        for (ArrayList<Integer> controlGroup : controlGroups)
            controlGroup.removeIf(e -> e == entityId);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        Entity entity = evt.getEntity();
        if (entity instanceof Unit && evt.getLevel().isClientSide)
            allUnitIds.add(entity.getId());
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Post evt) {
        if (!OrthoviewClientEvents.isEnabled()) return;

        // Can only detect clicks client side but only see and modify goals serverside so produce entity queues here
        // and consume in onWorldTick; we also can't add entities directly as they will not have goals populated

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {

            if (selectedUnitIds.size() > 0 && isLeftClickAttack()) {
                // A + left click -> force attack single unit (even if friendly)
                if (preselectedUnitIds.size() == 1 && !targetingSelf()) {
                    sendUnitCommand(UnitAction.ATTACK);
                }
                // A + left click -> attack move ground
                else {
                    sendUnitCommand(UnitAction.ATTACK_MOVE);
                }
            }

            // select all nearby units of the same type when double-clicked
            if (selectedUnitIds.size() == 1 && MC.level != null && !Keybinding.shiftMod.isDown() &&
               (System.currentTimeMillis() - lastLeftClickTime) < doubleClickTimeMs) {

                lastLeftClickTime = 0;
                Entity selectedUnit = MC.level.getEntity(selectedUnitIds.get(0));
                List<? extends Entity> nearbyEntities = MiscUtil.getEntitiesWithinRange(
                        new Vector3d(selectedUnit.position().x, selectedUnit.position().y, selectedUnit.position().z),
                        OrthoviewClientEvents.getZoom(),
                        MC.level.getEntity(selectedUnitIds.get(0)).getClass(),
                        MC.level
                );
                selectedUnitIds = new ArrayList<>();
                for (Entity entity : nearbyEntities)
                    if (getPlayerToEntityRelationship(entity.getId()) == Relationship.OWNED)
                        addSelectedUnitId(entity.getId());

            }
            // move on left click
            else if (CursorClientEvents.getLeftClickAction() == UnitAction.MOVE)
                resolveMoveAction();

            // TODO: resolve unit special abilities
            //else if ()

            // left click -> select a single unit
            // if shift is held, deselect a unit or add it to the selected group
            else if (preselectedUnitIds.size() == 1 && !isLeftClickAttack()) {
                boolean deselectedUnit = false;

                if (Keybinding.shiftMod.isDown())
                    deselectedUnit = selectedUnitIds.removeIf(id -> id.equals(preselectedUnitIds.get(0)));

                if (Keybinding.shiftMod.isDown() && !deselectedUnit &&
                    MC.level.getEntity(preselectedUnitIds.get(0)) instanceof Unit &&
                    getPlayerToEntityRelationship(preselectedUnitIds.get(0)) == Relationship.OWNED) {
                        addSelectedUnitId(preselectedUnitIds.get(0));
                }
                else if (!deselectedUnit) { // select a single unit - this should be the only code path that allows you to select a non-owned unit
                    selectedUnitIds = new ArrayList<>();
                    addSelectedUnitId(preselectedUnitIds.get(0));
                }
            }
            // deselect any non-owned units if we managed to select them with owned units
            // and disallow selecting > 1 non-owned unit or the client player
            if (selectedUnitIds.size() > 1)
                selectedUnitIds.removeIf(id -> getPlayerToEntityRelationship(id) != Relationship.OWNED || id == MC.player.getId());

            lastLeftClickTime = System.currentTimeMillis();
            CursorClientEvents.setLeftClickAction(UnitAction.ATTACK);
        }
        else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            if (selectedUnitIds.size() > 0) {
                // right click -> attack unfriendly unit
                if (preselectedUnitIds.size() == 1 &&
                    !targetingSelf() &&
                    getPlayerToEntityRelationship(preselectedUnitIds.get(0)) == Relationship.HOSTILE) {

                    sendUnitCommand(UnitAction.ATTACK);
                }
                // right click -> follow friendly unit
                else
                    resolveMoveAction();
            }
        }
        // clear all cursor actions
        CursorClientEvents.setLeftClickAction(null);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.ClientTickEvent evt) {

        // deselect everything
        if (Keybinding.getFnum(1).isDown()) {
            selectedUnitIds = new ArrayList<>();
            BuildingClientEvents.setSelectedBuilding(null);
            BuildingClientEvents.setBuildingToPlace(null);
        }

        // manage control groups
        if (controlGroups.size() <= 0) // initialise with empty arrays
            for (KeyMapping keyMapping : Keybinding.nums)
                controlGroups.add(new ArrayList<>());

        for (KeyMapping keyMapping : Keybinding.nums) {
            int index = Integer.parseInt(keyMapping.getKey().getDisplayName().getString());

            if (Keybinding.ctrlMod.isDown() &&
                keyMapping.isDown() &&
                selectedUnitIds.size() > 0 &&
                getPlayerToEntityRelationship(selectedUnitIds.get(0)) == Relationship.OWNED)
                controlGroups.set(index, selectedUnitIds);
            else if (keyMapping.isDown() && controlGroups.get(index).size() > 0)
                selectedUnitIds = controlGroups.get(index);
        }
    }


    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        if (MC.level == null)
            return;

        /**
         *  TODO: make these visible to 1st-person players but currently had a visual glitch
         *  doesnt align to camera very well, sometimes sinks below ground and too thin
         */
        // always-shown highlights to indicate unit relationships
        for (int unitId : allUnitIds) {
            Entity entity = MC.level.getEntity(unitId);
            if (entity != null) {
                Relationship unitRs = getPlayerToEntityRelationship(unitId);

                switch (unitRs) {
                    case OWNED -> MyRenderer.drawOutlineBottom(evt.getPoseStack(), entity.getBoundingBox(), 0.3f, 1.0f, 0.3f, 0.2f);
                    case FRIENDLY -> MyRenderer.drawOutlineBottom(evt.getPoseStack(), entity.getBoundingBox(), 0.3f, 0.3f, 1.0f, 0.2f);
                    case HOSTILE -> MyRenderer.drawOutlineBottom(evt.getPoseStack(), entity.getBoundingBox(), 1.0f, 0.3f, 0.3f, 0.2f);
                }
            }
        }

        ArrayList<Integer> selectedUnitIds = getSelectedUnitIds();
        ArrayList<Integer> preselectedUnitIds = getPreselectedUnitIds();

        Set<Integer> unitIdsToDraw = new HashSet<>();
        unitIdsToDraw.addAll(selectedUnitIds);
        unitIdsToDraw.addAll(preselectedUnitIds);

        // draw outlines on all (pre)selected units but only draw once per unit based on conditions
        for (int idToDraw : unitIdsToDraw) {
            Entity entity = MC.level.getEntity(idToDraw);
            if (entity != null) {
                if (preselectedUnitIds.contains(idToDraw) &&
                        isLeftClickAttack() &&
                        !targetingSelf())
                    MyRenderer.drawEntityOutline(evt.getPoseStack(), entity, 1.0f, 0.3f,0.3f, 1.0f);
                else if (selectedUnitIds.contains(idToDraw))
                    MyRenderer.drawEntityOutline(evt.getPoseStack(), entity, 1.0f);
                else if (preselectedUnitIds.contains(idToDraw))
                    MyRenderer.drawEntityOutline(evt.getPoseStack(), entity, 0.5f);
            }
        }
    }

    public static boolean targetingSelf() {
        return selectedUnitIds.size() == 1 &&
                preselectedUnitIds.size() == 1 &&
                selectedUnitIds.get(0).equals(preselectedUnitIds.get(0));
    }

    public static Relationship getPlayerToEntityRelationship(int entityId) {
        if (MC.level != null) {
            Entity entity = MC.level.getEntity(entityId);

            if (entity instanceof Player)
                return Relationship.HOSTILE;
            else if (!(entity instanceof Unit))
                return Relationship.NEUTRAL;

            String ownerName = ((Unit) entity).getOwnerName();

            if (ownerName.equals(MC.player.getName().getString()))
                return Relationship.OWNED;
            else
                return Relationship.HOSTILE;
        }
        return Relationship.NEUTRAL;
    }
}
