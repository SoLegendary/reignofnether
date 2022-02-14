package com.solegendary.reignofnether.cursor;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.orthoview.OrthoviewClientVanillaEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handler that implements and manages the cursor entity that converts screen space to game space
 */
public class CursorCommonVanillaEvents {

    private static ArmorStand cursorEntity;
    private static boolean cursorEntityAdded = false;

    // create an entity to track the position of the cursor in game space
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        ServerLevel world = (ServerLevel) evt.world;

        // on startup, remove all existing cursorEntities and create a new one
        if (!world.isClientSide() && cursorEntity == null)
            cursorEntity = EntityType.ARMOR_STAND.create(world);

        if (cursorEntity != null) {
            if (!cursorEntityAdded && OrthoviewClientVanillaEvents.isEnabled()) {
                world.addFreshEntity(cursorEntity);
                cursorEntity.setNoGravity(true);
                cursorEntityAdded = true;
            }
            if (cursorEntityAdded && !OrthoviewClientVanillaEvents.isEnabled()) {
                world.removeEntity(cursorEntity, true);
                cursorEntity = null;
                cursorEntityAdded = false;
            }
        }
    }

    public static void moveCursorEntity(Vector3d moveVec) {
        if (cursorEntity != null)
            cursorEntity.absMoveTo(moveVec.x, moveVec.y, moveVec.z);
    }

}
