package com.solegendary.ageofcraft.units;

import com.solegendary.ageofcraft.cursor.CursorClientVanillaEvents;
import com.solegendary.ageofcraft.orthoview.OrthoviewClientVanillaEvents;
import com.solegendary.ageofcraft.units.goals.MoveToCursorBlockGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Set;

public class UnitCommonVanillaEvents {

    private static ArrayList<Chicken> chickens = new ArrayList<>();
    private static int entityIdToMove = -1;

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
}
