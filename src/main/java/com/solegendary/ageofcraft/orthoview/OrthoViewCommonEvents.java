package com.solegendary.ageofcraft.orthoview;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handler that implements and manages hotkeys for the orthographic camera.
 *
 * @author SoLegendary, adapted from Mineshot by Nico Bergemann <barracuda415 at yahoo.de>
 */
public class OrthoViewCommonEvents {

    private static boolean isGuiOpen = false;
    private static ChickenEntity cursorEntity;
    private static boolean cursorEntityAdded = false;

    // create an entity to track the position of the cursor in game space
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        ServerWorld world = (ServerWorld) evt.world;

        if (!evt.world.isClientSide() && cursorEntity == null)
            cursorEntity = EntityType.CHICKEN.create(world);

        if (cursorEntity != null && !cursorEntityAdded) {
            world.addFreshEntity(cursorEntity);
            cursorEntityAdded = true;
        }
    }

    public static void moveCursorEntity(double x, double y, double z) {
        if (cursorEntity != null)
            cursorEntity.absMoveTo(x, y, z);
    }
}
