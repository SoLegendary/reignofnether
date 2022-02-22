package com.solegendary.reignofnether.units;

import com.solegendary.reignofnether.cursor.CursorClientVanillaEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.ArrayList;

public class UnitServerVanillaEvents {

    // TODO: consider changing PathfinderMob to Unit?

    private static ServerLevel level = null;

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        level = (ServerLevel) evt.world;
    }

    public static void consumeUnitActionQueues(
            boolean stopCommand,
            int unitIdToAttack,
            int unitIdToFollow,
            int[] unitIdsToMove,
            int[] unitIdsToAttackMove,
            int[] preselectedUnitIds,
            int[] selectedUnitIds
    ) {
        if (level == null) {
            System.out.println("Serverside level is null, cannot perform unit actions");
            return;
        }

        if (stopCommand) {
            for (int id : unitIdsToMove) {
                Unit unit = (Unit) level.getEntity(id);
                if (unit != null)
                    unit.resetTargets();
            }
        }
        for (int id : unitIdsToMove) {
            Unit unit = (Unit) level.getEntity(id);
            if (unit != null)
                unit.setMoveTarget(CursorClientVanillaEvents.getPreselectedBlockPos());
        }
        for (int id : unitIdsToAttackMove) {
            Unit unit = (Unit) level.getEntity(id);
            if (unit != null)
                unit.setAttackMoveTarget(CursorClientVanillaEvents.getPreselectedBlockPos());
        }
        for (int id : selectedUnitIds) {
            Unit unit = (Unit) level.getEntity(id);
            if (unit != null) {
                if (unitIdToAttack >= 0)
                    unit.setAttackTarget((LivingEntity) level.getEntity(unitIdToAttack));
                if (unitIdToFollow >= 0)
                    unit.setFollowTarget((LivingEntity) level.getEntity(unitIdToFollow));
            }
        }
    }
}
