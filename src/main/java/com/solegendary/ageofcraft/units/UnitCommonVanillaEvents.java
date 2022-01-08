package com.solegendary.ageofcraft.units;

import com.solegendary.ageofcraft.AgeOfCraft;
import com.solegendary.ageofcraft.cursor.CursorClientVanillaEvents;
import com.solegendary.ageofcraft.orthoview.OrthoviewClientVanillaEvents;
import com.solegendary.ageofcraft.units.goals.MoveToCursorBlockGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Set;

@Mod.EventBusSubscriber(modid=AgeOfCraft.MOD_ID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.DEDICATED_SERVER)
public class UnitCommonVanillaEvents {

    // note this seems to fire twice per entity, once serverside and once clientside
    // the clientside entity has no goals registered
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent evt) {
        Entity entity = evt.getEntity();

        // TODO: for some reason changing the selectedBlockPos briefly stops the goal from succeeding, maybe because recalculation takes way too long?
        if (entity instanceof Chicken && entity.getServer() != null) {
            Chicken chicken = (Chicken) entity;
            Set<WrappedGoal> goals = chicken.goalSelector.getAvailableGoals();
            System.out.println("Chicken (id " + chicken.getId() + ") joined world with " + goals.size() + " goals");
            chicken.goalSelector.removeAllGoals();
            chicken.goalSelector.addGoal(1, new MoveToCursorBlockGoal(chicken));
            System.out.println("Replaced all goals with MoveToCursorBlockGoal");
        }
    }
}
