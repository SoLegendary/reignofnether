package com.solegendary.ageofcraft.cursorentity;

import com.solegendary.ageofcraft.orthoview.OrthoViewClientEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.solegendary.ageofcraft.cursorentity.CursorEntity;

/**
 * Handler that implements and manages the cursor entity that converts screen space to game space
 *
 * @author SoLegendary, adapted from Mineshot by Nico Bergemann <barracuda415 at yahoo.de>
 */
public class CursorEntityCommonEvents {

    private static ArmorStandEntity cursorEntity;
    private static boolean cursorEntityAdded = false;

    // create an entity to track the position of the cursor in game space
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        ServerWorld world = (ServerWorld) evt.world;

        if (!evt.world.isClientSide() && cursorEntity == null)
            cursorEntity = EntityType.ARMOR_STAND.create(world);

        if (cursorEntity != null) {
            if (!cursorEntityAdded && OrthoViewClientEvents.isEnabled()) {
                world.addFreshEntity(cursorEntity);
                cursorEntity.setNoGravity(true);
                cursorEntityAdded = true;
            }
            if (cursorEntityAdded && !OrthoViewClientEvents.isEnabled()) {
                world.removeEntity(cursorEntity, true);
                cursorEntity = null;
                cursorEntityAdded = false;
            }
        }
    }

    public static void moveCursorEntity(double x, double y, double z) {
        if (cursorEntity != null)
            cursorEntity.absMoveTo(x, y, z);
    }
}
