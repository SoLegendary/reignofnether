package com.solegendary.reignofnether.util;

import com.mojang.math.Vector3d;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.minecraft.util.Mth.cos;
import static net.minecraft.util.Mth.sin;

public class MyMath {

    // returns whether b is between a and c
    public static boolean isBetween(double a, double b, double c) {
        return (a <= b && b <= c) || (a >= b && b >= c);
    }

    // returns vec3d with a set amount of the given unit vector added to it
    public static Vector3d addVector3d(Vector3d vec, Vector3d unitVec, float scale) {
        Vector3d unitVecLocal = new Vector3d(0,0,0);
        unitVecLocal.set(unitVec);
        unitVecLocal.scale(scale);
        Vector3d vecLocal = new Vector3d(0,0,0);
        vecLocal.set(vec);
        vecLocal.add(unitVecLocal);
        return vecLocal;
    }

    public static boolean rayIntersectsAABBCustom(Vector3d origin, Vector3d rayVector, AABB aabb) {
        // r.dir is unit direction vector of ray
        Vector3d dirfrac = new Vector3d(
                1.0f / rayVector.x,
                1.0f / rayVector.y,
                1.0f / rayVector.z
        );
        // lb is the corner of AABB with minimal coordinates - left bottom, rt is maximal corner
        // r.org is origin of ray
        float t1 = (float) ((aabb.minX - origin.x) * dirfrac.x);
        float t2 = (float) ((aabb.maxX - origin.x) * dirfrac.x);
        float t3 = (float) ((aabb.minY - origin.y) * dirfrac.y);
        float t4 = (float) ((aabb.maxY - origin.y) * dirfrac.y);
        float t5 = (float) ((aabb.minZ - origin.z) * dirfrac.z);
        float t6 = (float) ((aabb.maxZ - origin.z) * dirfrac.z);

        float tmin = max(max(min(t1, t2), min(t3, t4)), min(t5, t6));
        float tmax = min(min(max(t1, t2), max(t3, t4)), max(t5, t6));

        // if tmax < 0, ray (line) is intersecting AABB, but the whole AABB is behind us
        // if (tmax < 0) return false;
        // if tmin > tmax, ray doesn't intersect AABB
        if (tmin > tmax) return false;

        return true;
    }

    public static Vec2 rotateCoords(float x, float y, double deg) {
        float camXRotRads = (float) Math.toRadians(deg);
        float moveXRotated = (x * cos(camXRotRads)) - (y * sin(camXRotRads));
        float moveyRotated = (y * cos(camXRotRads)) + (x * sin(camXRotRads));
        return new Vec2(moveXRotated, moveyRotated);
    }
}
