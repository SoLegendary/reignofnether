package com.solegendary.ageofcraft.units;

import com.solegendary.ageofcraft.cursor.CursorClientVanillaEvents;
import com.solegendary.ageofcraft.orthoview.OrthoViewClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class UnitCommonVanillaEvents {

    @SubscribeEvent
    public static void onMouseClick(GuiScreenEvent.MouseReleasedEvent evt) {
        if (!OrthoViewClientEvents.isEnabled()) return;

        PathfinderMob selectedEntity = CursorClientVanillaEvents.getSelectedEntity();
        BlockPos cursorBlockPos = CursorClientVanillaEvents.getCursorBlockPos();


        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            if (selectedEntity != null) {
                System.out.println("Moving " + selectedEntity.getName().getString() + " to: " + cursorBlockPos.getX() + " " + cursorBlockPos.getY() + " " + cursorBlockPos.getZ());

                Goal goal = new MoveToBlockGoal(selectedEntity, 1.0, 100, 10) {
                    @Override
                    protected boolean isValidTarget(LevelReader levelReader, BlockPos blockPos) {
                        return blockPos.getX() == cursorBlockPos.getX() && blockPos.getZ() == cursorBlockPos.getZ();
                    }
                    public boolean canContinueToUse() { return true; }
                    public boolean canUse() { return true; }
                };
                // move at maximum priority towards block
                selectedEntity.goalSelector.addGoal(0, goal);
                goal.start();
            }
        }
    }

}
