package com.solegendary.reignofnether.units;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.ActionName;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class UnitClientEvents {

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
    private static ArrayList<Integer> allUnitIds = new ArrayList<>();

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
    public static int getUnitIdToAttack() { return unitIdToAttack; }
    public static int getUnitIdToFollow() { return unitIdToFollow; }
    public static void setUnitIdToAttack(int id) { unitIdToAttack = id; }
    public static void setUnitIdToFollow(int id) { unitIdToFollow = id; }
    public static void setUnitIdsToMove(ArrayList<Integer> unitIds) { unitIdsToMove = unitIds; }
    public static void setUnitIdsToAttackMove(ArrayList<Integer> unitIds) { unitIdsToAttackMove = unitIds; }

    private static long lastLeftClickTime = 0; // to track double clicks
    private static final long doubleClickTimeMs = 500;

    private static boolean isLeftClickAttack() {
        return CursorClientEvents.getLeftClickAction() == ActionName.ATTACK;
    }

    private static void resolveMoveAction() {
        // follow friendly unit
        if (preselectedUnitIds.size() == 1 && !targetingSelf())
            setUnitIdToFollow(preselectedUnitIds.get(0));
        // move to ground pos (disabled during camera manip)
        else if (!Keybinds.altMod.isDown()) {
            ArrayList<Integer> unitIdsToMove = new ArrayList<>();
            unitIdsToMove.addAll(selectedUnitIds);
            setUnitIdsToMove(unitIdsToMove);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveWorldEvent evt) {
        int entityId = evt.getEntity().getId();

        preselectedUnitIds.removeIf(e -> e == entityId);
        selectedUnitIds.removeIf(e -> e == entityId);
        allUnitIds.removeIf(e -> e == entityId);

        for (ArrayList<Integer> controlGroup : controlGroups)
            controlGroup.removeIf(e -> e == entityId);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent evt) {
        Entity entity = evt.getEntity();
        if (entity instanceof Unit && evt.getWorld().isClientSide)
            allUnitIds.add(entity.getId());
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseClickedEvent.Post evt) {
        if (!OrthoviewClientEvents.isEnabled()) return;

        // Can only detect clicks client side but only see and modify goals serverside so produce entity queues here
        // and consume in onWorldTick; we also can't add entities directly as they will not have goals populated

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {

            if (selectedUnitIds.size() > 0 && isLeftClickAttack()) {
                // A + left click -> force attack single unit (even if friendly)
                if (preselectedUnitIds.size() == 1 && !targetingSelf())
                    setUnitIdToAttack(preselectedUnitIds.get(0));
                // A + left click -> attack move ground
                else {
                    ArrayList<Integer> unitIdsToAttackMove = new ArrayList<>();
                    unitIdsToAttackMove.addAll(selectedUnitIds);
                    setUnitIdsToAttackMove(unitIdsToAttackMove);
                }
            }

            // select all nearby units of the same type when double clicked
            if (selectedUnitIds.size() == 1 && MC.level != null &&
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
            else if (CursorClientEvents.getLeftClickAction() == ActionName.MOVE)
                resolveMoveAction();

            // TODO: resolve unit special abilities
            //else if ()

            // left click -> (de)select a single unit
            // if shift is held, deselect a unit or add it to the selected group
            else if (preselectedUnitIds.size() == 1 && !isLeftClickAttack()) {

                if (Keybinds.shiftMod.isDown() &&
                    !selectedUnitIds.removeIf(id -> id.equals(preselectedUnitIds.get(0))) &&
                    MC.level.getEntity(preselectedUnitIds.get(0)) instanceof Unit &&
                    getPlayerToEntityRelationship(preselectedUnitIds.get(0)) == Relationship.OWNED) {
                        addSelectedUnitId(preselectedUnitIds.get(0));
                }
                else { // this should be the only code path that allows you to select a non-owned unit
                    selectedUnitIds = new ArrayList<>();
                    addSelectedUnitId(preselectedUnitIds.get(0));
                }
            }
            lastLeftClickTime = System.currentTimeMillis();
            CursorClientEvents.setLeftClickAction(ActionName.ATTACK);
        }
        else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            if (selectedUnitIds.size() > 0) {
                // right click -> attack unfriendly unit
                if (preselectedUnitIds.size() == 1 &&
                        !targetingSelf() &&
                        getPlayerToEntityRelationship(preselectedUnitIds.get(0)) == Relationship.HOSTILE)
                    setUnitIdToAttack(preselectedUnitIds.get(0));
                // right click -> follow friendly unit
                else
                    resolveMoveAction();
            }
        }

        // send all of the commands over to server to enact
        if (unitIdToAttack >= 0 ||
            unitIdToFollow >= 0 ||
            unitIdsToMove.size() > 0 ||
            unitIdsToAttackMove.size() > 0) {

            PacketHandler.INSTANCE.sendToServer(new UnitServerboundPacket(
                    ActionName.NONE,
                    unitIdToAttack,
                    unitIdToFollow,
                    unitIdsToMove.stream().mapToInt(i -> i).toArray(), // convert List<Integer> to int[]
                    unitIdsToAttackMove.stream().mapToInt(i -> i).toArray(),
                    preselectedUnitIds.stream().mapToInt(i -> i).toArray(),
                    selectedUnitIds.stream().mapToInt(i -> i).toArray(),
                    CursorClientEvents.getPreselectedBlockPos()
            ));
            unitIdToAttack = -1;
            unitIdToFollow = -1;
            unitIdsToMove = new ArrayList<>();
            unitIdsToAttackMove = new ArrayList<>();
        }

        // clear all cursor actions
        CursorClientEvents.setLeftClickAction(null);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.ClientTickEvent evt) {

        // deselect everything
        if (Keybinds.fnums[1].isDown()) {
            selectedUnitIds = new ArrayList<>();
            BuildingClientEvents.setSelectedBuilding(null);
        }

        // manage control groups
        if (controlGroups.size() <= 0) // initialise with empty arrays
            for (KeyMapping keyMapping : Keybinds.nums)
                controlGroups.add(new ArrayList<>());

        for (KeyMapping keyMapping : Keybinds.nums) {
            int index = Integer.parseInt(keyMapping.getKey().getDisplayName().getContents());

            if (Keybinds.ctrlMod.isDown() &&
                keyMapping.isDown() &&
                selectedUnitIds.size() > 0 &&
                getPlayerToEntityRelationship(selectedUnitIds.get(0)) == Relationship.OWNED)
                controlGroups.set(index, selectedUnitIds);
            else if (keyMapping.isDown() && controlGroups.get(index).size() > 0)
                selectedUnitIds = controlGroups.get(index);
        }
    }


    @SubscribeEvent
    public static void onRenderWorld(RenderLevelLastEvent evt) {

        if (MC.level == null || !OrthoviewClientEvents.isEnabled())
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

    public static void sendCommand(ActionName actionName) {
        PacketHandler.INSTANCE.sendToServer(new UnitServerboundPacket(
                actionName,
                -1, // unitIdToAttack
                -1, // unitIdToFollow
                new int[0], // unitIdsToMove
                new int[0], // unitIdsToAttackMove
                preselectedUnitIds.stream().mapToInt(i -> i).toArray(),
                selectedUnitIds.stream().mapToInt(i -> i).toArray(),
                CursorClientEvents.getPreselectedBlockPos() // used for actions that ground target
        ));
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

    /*
    @SubscribeEvent
    public static void onRenderLivingEntity(RenderLivingEvent.Pre<? extends LivingEntity, ? extends Model> evt) {
        LivingEntity entity = evt.getEntity();
        Model model = evt.getRenderer().getModel();

        if (entity instanceof Unit) {
            if (entity instanceof Creeper) {
                ((CreeperModel) model).root().visible = false;
            }
            else {
                ((HumanoidModel) model).body.visible = false;
            }
        }
    }*/
}
