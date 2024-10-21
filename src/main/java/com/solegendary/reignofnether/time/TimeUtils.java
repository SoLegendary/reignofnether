package com.solegendary.reignofnether.time;

import com.solegendary.reignofnether.building.*;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TimeUtils {

    public static boolean isInRangeOfNightSource(Vec3 pos, boolean clientSide) {
        List<Building> buildings = clientSide ? BuildingClientEvents.getBuildings() : BuildingServerEvents.getBuildings();
        for (Building building : buildings) {
            if (building.isDestroyedServerside)
                continue;
            if (building instanceof NightSource ns) {
                return BuildingUtils.getCentrePos(building.getBlocks())
                        .distToCenterSqr(pos.x, pos.y, pos.z) < Math.pow(ns.getNightRange(), 2);
            }
        }
        return false;
    }
}
