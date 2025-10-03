package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.hero.HeroExperienceOrb;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class ThrownHeroExperienceBottle extends ThrowableItemProjectile {

    private static float RANGE = 10;

    public ThrownHeroExperienceBottle(EntityType<? extends ThrownHeroExperienceBottle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ThrownHeroExperienceBottle(Level pLevel, LivingEntity pShooter) {
        super(EntityRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get(), pShooter, pLevel);
    }

    protected Item getDefaultItem() {
        return ItemRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get();
    }

    protected float getGravity() {
        return 0.07F;
    }

    protected void onHit(HitResult pResult) {
        super.onHit(pResult);
        if (this.level() instanceof ServerLevel) {
            this.level().levelEvent(2002, this.blockPosition(), PotionUtils.getColor(Potions.WATER));

            List<LivingEntity> nearbyEntities = MiscUtil.getEntitiesWithinRange(position(), RANGE, LivingEntity.class, level());
            HeroUnit closestHero = null;
            double closestDist = RANGE;
            for (LivingEntity entity : nearbyEntities) {
                if (entity instanceof HeroUnit heroUnit) {
                    double dist = entity.position().distanceTo(position());
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestHero = heroUnit;
                    }
                }
            }
            if (closestHero != null)
                for (int i = 0; i < 5; i++) {
                    HeroExperienceOrb expOrb = HeroExperienceOrb.newOrb(
                            level(),
                            closestHero,
                            false,
                            getX(),
                            getY(),
                            getZ(),
                            2
                    );
                    level().addFreshEntity(expOrb);
                }
            this.discard();
        }

    }
}
