package com.solegendary.reignofnether.util;

import com.solegendary.reignofnether.particles.BigVibrationParticleOption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class ParticleUtil {

    private static final Random RANDOM = new Random();

    public static void addParticleExplosion(SimpleParticleType particleType, int amount, Level level, Vec3 pos) {
        addParticleExplosion(particleType, amount, level, pos, 0.1f);
    }

    public static void addParticleExplosion(SimpleParticleType particleType, int amount, Level level, Vec3 pos, double velocityScale) {
        RandomSource rand = RandomSource.create();
        for (int j = 0; j < amount; ++j) {
            double d0 = rand.nextGaussian() * velocityScale;
            double d1 = rand.nextGaussian() * velocityScale;
            double d2 = rand.nextGaussian() * velocityScale;
            if (level.isClientSide()) {
                level.addParticle(particleType, pos.x, pos.y, pos.z, d0, d1, d2);
            } else {
                ((ServerLevel) level).sendParticles(particleType, pos.x, pos.y, pos.z, 0, d0, d1, d2, 1);
            }
        }
    }

    // called for flying windcallers and levitating mobs
    public static void spawnFlyingCloudParticles(Entity entity) {
        double px = entity.getX();
        double py = entity.getY();
        double pz = entity.getZ();

        // Spawn a loose ring of cloud puffs around the feet
        int numPuffs = 1;
        for (int i = 0; i < numPuffs; i++) {
            double angle = (entity.tickCount * 0.25 + (Math.PI * 2.0 / numPuffs) * i) % (Math.PI * 2.0);
            double radius = 0.3 + RANDOM.nextDouble() * 0.2;
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            double oy = -0.1 + RANDOM.nextDouble() * 0.1; // slightly below/at foot level

            // Gentle upward and outward drift
            double vx = ox * 0.015;
            double vy = 0.005 + RANDOM.nextDouble() * 0.01;
            double vz = oz * 0.015;

            entity.level().addParticle(
                    ParticleTypes.CLOUD,
                    px + ox, py + oy, pz + oz,
                    vx, vy, vz
            );
        }

        // Occasional extra wisp for density variation
        if (entity.tickCount % 10 == 0) {
            double ox = (RANDOM.nextDouble() - 0.5) * 0.5;
            double oz = (RANDOM.nextDouble() - 0.5) * 0.5;
            entity.level().addParticle(
                    ParticleTypes.CLOUD,
                    px + ox, py - 0.05, pz + oz,
                    0, 0.008, 0
            );
        }
    }

    /**
     * Spawns N vibration particles evenly spaced around a circle in the XZ plane,
     * each traveling outward from origin to a point at the given radius.
     *
     * @param level       the server level to spawn in
     * @param origin      origin point (spawn point of every vibration)
     * @param count       number of vibrations to spawn (N)
     * @param radius      distance outward each vibration travels
     * @param travelTicks how many ticks each vibration takes to reach its target
     */
    public static void spawnRadialVibrations(ServerLevel level, Vec3 origin,
                                             int count, double radius, int travelTicks) {
        if (count <= 0) return;

        double angleStep = (2 * Math.PI) / count;

        for (int i = 0; i < count; i++) {
            double angle = i * angleStep;

            double targetX = origin.x + radius * Math.cos(angle);
            double targetZ = origin.z + radius * Math.sin(angle);

            BlockPos targetPos = BlockPos.containing(targetX, origin.y, targetZ);
            PositionSource source = new BlockPositionSource(targetPos);
            BigVibrationParticleOption vibrationOptions = new BigVibrationParticleOption(source, travelTicks);

            level.sendParticles(
                    vibrationOptions,
                    origin.x, origin.y, origin.z,  // spawn at the center every time
                    1,                             // count per call
                    0.0, 0.0, 0.0, // no offset
                    0.0                            // no speed
            );
        }
    }
}
