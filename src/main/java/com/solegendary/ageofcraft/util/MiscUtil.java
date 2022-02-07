package com.solegendary.ageofcraft.util;

import com.mojang.math.Vector3d;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class MiscUtil {

    private static final Minecraft MC = Minecraft.getInstance();

    // use this for client side entities only
    public static <T extends Entity> List<T> getEntitiesWithinRange(Vector3d pos, float range, Class<T> entityType) {
        return getEntitiesWithinRange(pos, range, entityType, MC.level);
    }

    public static <T extends Entity> List<T> getEntitiesWithinRange(Vector3d pos, float range, Class<T> entityType, Level level) {
        AABB aabb = new AABB(
                pos.x - range,
                pos.y - range,
                pos.z - range,
                pos.x + range,
                pos.y + range,
                pos.z + range
        );
        if (level != null) {
            List<T> entities = level.getEntitiesOfClass(entityType, aabb);
            List<T> entitiesInRange = new ArrayList<>();

            for (Entity entity : entities)
                if (entity.position().distanceTo(new Vec3(pos.x, pos.y, pos.z)) <= range)
                    entitiesInRange.add((T) entity);

            return entitiesInRange;
        }
        else
            return new ArrayList<>();
    }
}
