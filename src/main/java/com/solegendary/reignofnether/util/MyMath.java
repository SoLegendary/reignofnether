package com.solegendary.reignofnether.util;

import com.mojang.math.Vector3d;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;

import static java.lang.Math.*;
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
        float xRotRads = (float) Math.toRadians(deg);
        float moveXRotated = (x * cos(xRotRads)) - (y * sin(xRotRads));
        float moveyRotated = (y * cos(xRotRads)) + (x * sin(xRotRads));
        return new Vec2(moveXRotated, moveyRotated);
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(pow(x2-x1,2) + pow(y2-y1,2));
    }

    // https://stackoverflow.com/questions/11907947/how-to-check-if-a-point-lies-on-a-line-between-2-other-points
    // the greater quad_threshold, the thicker the lines will be; ptc is the tested point
    public static boolean isPointOnLine(Vec2 pt1, Vec2 pt2, Vec2 ptc, float threshold) {

        // gradient
        double dx1 = ptc.x - pt1.x;
        double dy1 = ptc.y - pt1.y;

        double dx2 = pt2.x - pt1.x;
        double dy2 = pt2.y - pt1.y;

        double cross = dx1 * dy2 - dy1 * dx2;

        // checks if on line
        if (Math.abs(cross) > threshold)
            return false;

        // checks if between the two points
        if (abs(dx2) >= abs(dy2))
            return dx2 > 0 ?
                pt1.x <= ptc.x && ptc.x <= pt2.x :
                pt2.x <= ptc.x && ptc.x <= pt1.x;
        else
            return dy2 > 0 ?
                pt1.y <= ptc.y && ptc.y <= pt2.y :
                pt2.y <= ptc.y && ptc.y <= pt1.y;
    }

    public static int randRangeInt(int min, int max) {
        int posRandInt = (int) ((max - min) * Math.random());
        return posRandInt + min;
    }
}
