package com.solegendary.reignofnether.time;

import com.solegendary.reignofnether.building.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TimeUtils {

    public static boolean isInRangeOfNightSource(Vec3 pos, boolean clientSide) {
        List<Building> buildings = clientSide ? BuildingClientEvents.getBuildings() : BuildingServerEvents.getBuildings();
        for (Building building : buildings) {
            if (building.isDestroyedServerside)
                continue;
            if (building instanceof NightSource ns) {

                BlockPos centrePos = BuildingUtils.getCentrePos(building.getBlocks());
                Vec2 centrePos2d = new Vec2(centrePos.getX(), centrePos.getZ());
                Vec2 pos2d = new Vec2((float) pos.x, (float) pos.z);

                return centrePos2d.distanceToSqr(pos2d) < Math.pow(ns.getNightRange(), 2);
            }
        }
        return false;
    }
}
