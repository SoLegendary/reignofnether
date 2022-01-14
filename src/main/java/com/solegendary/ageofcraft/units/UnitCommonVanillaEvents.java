package com.solegendary.ageofcraft.units;

import com.solegendary.ageofcraft.cursor.CursorClientVanillaEvents;
import com.solegendary.ageofcraft.orthoview.OrthoviewClientVanillaEvents;
import com.solegendary.ageofcraft.units.goals.MoveToCursorBlockGoal;
import com.solegendary.ageofcraft.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

import static net.minecraft.util.Mth.floor;

public class UnitCommonVanillaEvents {
    private static final Minecraft MC = Minecraft.getInstance();

    // entity moused over, vs entity selected by clicking
    private static ArrayList<PathfinderMob> preselectedUnits = new ArrayList<>();
    private static ArrayList<PathfinderMob> selectedUnits = new ArrayList<>();
    private static ArrayList<Integer> unitIdsToMove = new ArrayList<>();

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

    // note this seems to fire twice per entity, once serverside and once clientside
    // the clientside entity has no goals registered
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent evt) {
        Entity entity = evt.getEntity();

        // Remove all goals from chickens that join; these get readded on every world reload
        if (entity instanceof Chicken && entity.getServer() != null) {
            Chicken chicken = (Chicken) entity;
            Set<WrappedGoal> goals = chicken.goalSelector.getAvailableGoals();
            System.out.println("Chicken (id " + chicken.getId() + ") joined world with " + goals.size() + " goals");
            chicken.goalSelector.removeAllGoals();
            chicken.goalSelector.addGoal(1, new MoveToCursorBlockGoal(chicken, 1.0f));
        }
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

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {

            // TODO: various fixes:
            // 1. mobs always want to finish moving to the first block if moved again before they finish their journey
            // 3. improve performance of movement and selection in general

            // Can only detect clicks client side but only see and modify goals serverside so produce entity queue here
            // and consume in onWorldTick; we also can't add entities directly as they will not have goals populated
            unitIdsToMove = new ArrayList<>();

            for (PathfinderMob unit : selectedUnits)
                unitIdsToMove.add(unit.getId());
        }
    }


    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        if (!OrthoviewClientVanillaEvents.isEnabled()) return;

        ServerLevel world = (ServerLevel) evt.world;

        // enact queue of unit actions
        if (!world.isClientSide()) {
            // Consume list produced in onMouseClick
            for (int id : unitIdsToMove) {
                PathfinderMob unit = (PathfinderMob) world.getEntity(id);

                for (WrappedGoal goal : unit.goalSelector.getAvailableGoals()) {
                    if (goal.getGoal() instanceof MoveToCursorBlockGoal) {
                        MoveToCursorBlockGoal moveGoal = (MoveToCursorBlockGoal) goal.getGoal();
                        BlockPos bp = CursorClientVanillaEvents.getPreselectedBlockPos();
                        moveGoal.setNewTargetBp(bp);
                    }
                }
            }
            unitIdsToMove = new ArrayList<>();
        }
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldLastEvent evt) {
        if (MC.level != null && OrthoviewClientVanillaEvents.isEnabled()) {
            if (selectedUnits != null) {
                for (PathfinderMob unit : selectedUnits)
                    MyRenderer.drawEntityOutline(evt.getMatrixStack(), unit, 1.0f);
            }
            if (preselectedUnits != null) {
                for (PathfinderMob unit : preselectedUnits)
                    MyRenderer.drawEntityOutline(evt.getMatrixStack(), unit, 0.5f);
            }
        }
    }


}
