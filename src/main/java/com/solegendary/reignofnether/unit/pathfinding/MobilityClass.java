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
        // Wider than a full block spans multiple cells and needs a multi-cell footprint; narrower fits one cell.
        if (mob.getBbWidth() > 1.0f) return LARGE;
        return HUMANOID;
    }

    public float costFor(byte kind) {
        switch (kind) {
            case WalkabilityBuilder.KIND_LAND: return 1.0f;
            case WalkabilityBuilder.KIND_WATER:
                if (this == AQUATIC) return 5.0f;
                return 20.0f;
            case WalkabilityBuilder.KIND_FIRE:  return PathfinderConfig.FIRE_AVOID_COST;
            case WalkabilityBuilder.KIND_SLIME:  return PathfinderConfig.SLIME_AVOID_COST;
            default: return Float.POSITIVE_INFINITY;
        }
    }

    // Land/water cost is the same for every unit of a class, but fire cost is per-unit (depends on fire immunity
    // + the unit's DAMAGE_FIRE malus), so A* injects it per request.
    public float costFor(byte kind, float fireCost) {
        if (kind == WalkabilityBuilder.KIND_FIRE) return fireCost;
        return costFor(kind);
    }
}
