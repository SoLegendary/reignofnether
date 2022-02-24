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

    private static boolean stopCommand = false;
    private static int unitIdToAttack = -1;
    private static int unitIdToFollow = -1;
    private static int[] unitIdsToMove = new int[0];
    private static int[] unitIdsToAttackMove = new int[0];
    private static int[] preselectedUnitIds = new int[0];
    private static int[] selectedUnitIds = new int[0];
    private static BlockPos preselectedBlockPos = null;

    public static int[] getPreselectedUnitIds() { return preselectedUnitIds; }
    public static int[] getSelectedUnitIds() { return selectedUnitIds; }
    public static BlockPos getPreselectedBlockPos() { return preselectedBlockPos; }



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

    // TODO: change this later to check for the unit's player controller instead of just type
    public static boolean isUnitFriendly(int unitId) {
        return true;
    }
}
