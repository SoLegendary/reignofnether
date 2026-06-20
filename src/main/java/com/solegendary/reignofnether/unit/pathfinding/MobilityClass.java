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
        if (mob.getBbWidth() > 1.4f) return LARGE;
        return HUMANOID;
    }

    public float costFor(byte kind) {
        switch (kind) {
            case WalkabilityBuilder.KIND_LAND:  return 1.0f;
            case WalkabilityBuilder.KIND_WATER:
                if (this == AQUATIC) return 0.8f;
                if (this == LARGE)   return 5.0f;
                return 3.0f;
            case WalkabilityBuilder.KIND_FIRE:  return 50.0f;
            default: return Float.POSITIVE_INFINITY;
        }
    }
}
