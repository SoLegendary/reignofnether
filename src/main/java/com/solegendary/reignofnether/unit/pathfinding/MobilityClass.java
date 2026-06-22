package com.solegendary.reignofnether.unit.pathfinding;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Drowned;

public enum MobilityClass {
    HUMANOID, AQUATIC, LARGE;

    public static MobilityClass of(Unit unit) {
        if (!(unit instanceof Mob mob)) return HUMANOID;
        if (mob instanceof Drowned || mob instanceof WaterAnimal) return AQUATIC;
        // Anything wider than a full block (spider/panda/bear 1.3-1.4, hoglin, ravager 1.95, ...) spans
        // more than one cell and needs a multi-cell footprint; 0.6-wide humanoids fit a single cell.
        if (mob.getBbWidth() > 1.0f) return LARGE;
        return HUMANOID;
    }

    public float costFor(byte kind) {
        switch (kind) {
            case WalkabilityBuilder.KIND_LAND:  return 1.0f;
            case WalkabilityBuilder.KIND_WATER:
                if (this == AQUATIC) return 0.8f;
                if (this == LARGE)   return 5.0f;
                return 3.0f;
            case WalkabilityBuilder.KIND_FIRE:  return PathfinderConfig.FIRE_AVOID_COST;
            default: return Float.POSITIVE_INFINITY;
        }
    }

    // Mobility (land/water) is the same for every unit of a class, but the fire cost is per-unit (it depends
    // on fire immunity + the unit's DAMAGE_FIRE malus), so the A* search injects it per request.
    public float costFor(byte kind, float fireCost) {
        if (kind == WalkabilityBuilder.KIND_FIRE) return fireCost;
        return costFor(kind);
    }
}
