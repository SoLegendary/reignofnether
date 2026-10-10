package com.solegendary.reignofnether.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;

public final class PearlThrower {

    // ThrowableProjectile physics: pos += vel; vel *= 0.99; vel.y -= 0.03
    private static final double DRAG = 0.99;
    private static final double GRAVITY = 0.03;

    /**
     * Throws an ender pearl in an upward (lobbed) arc toward the given block.
     * @return true if a valid arc was found and the pearl was thrown
     */
    public static boolean throwPearlAt(LivingEntity thrower, BlockPos target) {
        Level level = thrower.level();
        if (level.isClientSide) return false;

        // Where the pearl spawns (same as the ThrownEnderpearl constructor)
        double sx = thrower.getX();
        double sy = thrower.getEyeY() - 0.1;
        double sz = thrower.getZ();

        // Aim at the top surface of the block so the pearl lands on it
        double dx = target.getX() + 0.5 - sx;
        double dz = target.getZ() + 0.5 - sz;
        double dy = target.getY() + 1.0 - sy;
        double dist = Math.sqrt(dx * dx + dz * dz);

        double dirX = dist < 1.0E-4 ? 0 : dx / dist;
        double dirZ = dist < 1.0E-4 ? 0 : dz / dist;

        double loPitch = Math.toRadians(45); // flattest arc we allow
        double hiPitch = Math.toRadians(89); // steepest arc we allow

        // Use the slowest speed that can reach the target
        for (double speed = 0.8; speed <= 3.0; speed += 0.1) {
            if (horizontalRangeAtHeight(speed, loPitch, dy) < dist) continue; // too weak

            double lo = loPitch, hi = hiPitch;
            if (horizontalRangeAtHeight(speed, hi, dy) >= dist) {
                lo = hi; // target is basically straight up/very close
            } else {
                // Range decreases as pitch increases in the 45-89 deg band -> bisect
                for (int i = 0; i < 30; i++) {
                    double mid = (lo + hi) / 2;
                    if (horizontalRangeAtHeight(speed, mid, dy) >= dist) lo = mid;
                    else hi = mid;
                }
            }

            double pitch = lo;
            ThrownEnderpearl pearl = new ThrownEnderpearl(level, thrower);
            // shoot() normalizes the direction and scales it by speed (0 inaccuracy)
            pearl.shoot(dirX * Math.cos(pitch), Math.sin(pitch), dirZ * Math.cos(pitch), (float) speed, 0.0F);

            level.playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(),
                    SoundEvents.ENDER_PEARL_THROW, SoundSource.NEUTRAL, 0.5F,
                    0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            level.addFreshEntity(pearl);
            return true;
        }
        return false; // out of reach
    }

    /**
     * Simulates a launch and returns the horizontal distance travelled when the pearl
     * descends through height dy (relative to the launch point). Returns 0 if it never
     * gets there (e.g. apex is below dy).
     */
    private static double horizontalRangeAtHeight(double speed, double pitch, double dy) {
        double vx = speed * Math.cos(pitch);
        double vy = speed * Math.sin(pitch);
        double x = 0, y = 0;

        for (int tick = 0; tick < 600; tick++) {
            double px = x, py = y;
            boolean descending = vy < 0;

            x += vx;
            y += vy;
            vx *= DRAG;
            vy = vy * DRAG - GRAVITY;

            if (descending && y <= dy) {
                if (py < dy) return 0; // apex was below target height
                double t = (py - dy) / (py - y); // interpolate the crossing point
                return px + t * (x - px);
            }
        }
        return 0;
    }
}