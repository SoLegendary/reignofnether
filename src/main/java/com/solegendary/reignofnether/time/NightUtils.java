package com.solegendary.reignofnether.time;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class NightUtils {

    public static boolean isInRangeOfNightSource(Vec3 pos, boolean clientSide) {
        if ((clientSide && TimeClientEvents.isBloodMoonActive()) ||
            (!clientSide && TimeServerEvents.isBloodMoonActive())) {
            return true;
        }
        List<BuildingPlacement> buildings = clientSide ? BuildingClientEvents.getBuildings() : BuildingServerEvents.getBuildings();

        Vec2 pos2d = new Vec2((float) pos.x, (float) pos.z);

        for (BuildingPlacement building : buildings) {
            if (building.isDestroyedServerside) continue;
            if (building instanceof NightSource ns && ns.getNightRange() > 0) {
                BlockPos centrePos = BuildingUtils.getCentrePos(building.getBlocks());
                Vec2 centrePos2d = new Vec2(centrePos.getX(), centrePos.getZ());
                float nightRangeSqr = ns.getNightRange() * ns.getNightRange();
                if (centrePos2d.distanceToSqr(pos2d) < nightRangeSqr) {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean isSunBurnTick(Mob mob) {
        if (mob.level().isClientSide)
            return false;

        if (mob instanceof Unit unit && ResearchServerEvents.playerHasCheat(unit.getOwnerName(), "slipslopslap"))
            return false;

        if (mob.tickCount % 10 == 0 && TimeUtils.isDay(mob.level().getDayTime())) {
            BlockPos blockpos = new BlockPos((int) mob.getX(), (int) mob.getEyeY(), (int) mob.getZ());
            boolean isProtected = mob.isInWaterRainOrBubble() || mob.isInPowderSnow || mob.wasInPowderSnow || mob.isOnFire();
            // Return early if mob is protected or sky is not visible
            if (isProtected || !mob.level().canSeeSky(blockpos)) return false;

            // Check if mob is within range of any NightSource
            Vec3 mobEyePos = mob.getEyePosition();
            return !NightUtils.isInRangeOfNightSource(mobEyePos, mob.level().isClientSide);
        }
        return false;
    }
}
