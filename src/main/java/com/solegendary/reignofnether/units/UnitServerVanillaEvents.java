package com.solegendary.reignofnether.units;

import com.solegendary.reignofnether.cursor.CursorClientVanillaEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.ArrayList;

public class UnitServerVanillaEvents {

    // TODO: consider changing PathfinderMob to Unit?

    static boolean stopCommand = false;
    static int unitIdToAttack = -1;
    static int unitIdToFollow = -1;
    static int[] unitIdsToMove = new int[0];
    static int[] unitIdsToAttackMove = new int[0];
    static int[] preselectedUnitIds = new int[0];
    static int[] selectedUnitIds = new int[0];
    static BlockPos preselectedBlockPos = null;

    // for some reason we have to use the level in the same tick as the unit actions or else level.getEntity returns null
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        ServerLevel level = (ServerLevel) evt.world;

        if (!level.isClientSide()) {
            if (stopCommand) {
                for (int id : selectedUnitIds) {
                    Unit unit = (Unit) level.getEntity(id);
                    if (unit != null)
                        unit.resetTargets();
                }
            }
            for (int id : unitIdsToMove) {
                Unit unit = (Unit) level.getEntity(id);
                if (unit != null)
                    unit.setMoveTarget(preselectedBlockPos);
            }
            for (int id : unitIdsToAttackMove) {
                Unit unit = (Unit) level.getEntity(id);
                if (unit != null)
                    unit.setAttackMoveTarget(preselectedBlockPos);
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
            stopCommand = false;
            unitIdToAttack = -1;
            unitIdToFollow = -1;
            unitIdsToMove = new int[0];
            unitIdsToAttackMove = new int[0];
        }
    }

    public static void consumeUnitActionQueues(
            boolean stopCommandIn,
            int unitIdToAttackIn,
            int unitIdToFollowIn,
            int[] unitIdsToMoveIn,
            int[] unitIdsToAttackMoveIn,
            int[] preselectedUnitIdsIn,
            int[] selectedUnitIdsIn,
            BlockPos preselectedBlockPosIn
    ) {
        stopCommand = stopCommandIn;
        unitIdToAttack = unitIdToAttackIn;
        unitIdToFollow = unitIdToFollowIn;
        unitIdsToMove = unitIdsToMoveIn;
        unitIdsToAttackMove = unitIdsToAttackMoveIn;
        preselectedUnitIds = preselectedUnitIdsIn;
        selectedUnitIds = selectedUnitIdsIn;
        preselectedBlockPos = preselectedBlockPosIn;
    }
}
