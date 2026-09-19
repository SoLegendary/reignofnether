package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.unit.units.monsters.AbstractTotem;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TotemOfShielding extends AbstractTotem {

    public TotemOfShielding(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    private static final double MAX_REPEL_FORCE = 1.0; // blocks/tick^2 applied at point-blank range

    public void tick() {
        super.tick();

        Vec3 totemCentre = position().add(0, getBbHeight() / 2, 0);

        for (Projectile proj : MiscUtil.getEntitiesWithinRange(position(), AURA_RANGE, Projectile.class, level())) {
            if (proj instanceof AbstractArrow arrow) {
                Vec3 motion = arrow.getDeltaMovement();
                Vec3 horizDeltaMovement = new Vec3(motion.x, 0, motion.z);

                // ignore arrows that are stuck in the ground or otherwise barely moving
                if (horizDeltaMovement.length() <= 0.1) {
                    continue;
                }

                // direction pointing from the totem's centre towards the arrow
                Vec3 offset = arrow.position().subtract(totemCentre);
                double dist = offset.length();
                if (dist < 0.001) {
                    continue;
                }
                Vec3 awayDir = offset.scale(1.0 / dist);

                // stronger the closer the arrow is, fading to zero at the aura's edge
                double falloff = Math.max(0, 1.0 - (dist / AURA_RANGE));
                arrow.addDeltaMovement(awayDir.scale(MAX_REPEL_FORCE * falloff));
            }
        }
    }
}
