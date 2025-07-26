package com.solegendary.reignofnether.hero;

import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class HeroExperienceOrb extends ExperienceOrb {

    private int age = 0;
    public static final float RANGE = 20;

    private static final Random RANDOM = new Random();

    private LivingEntity followingHero = null;
    private boolean isNeutral = false;

    public HeroExperienceOrb(EntityType<? extends ExperienceOrb> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static HeroExperienceOrb newOrb(Level pLevel, HeroUnit heroUnit, boolean isNeutral, double pX, double pY, double pZ, int pValue) {
        HeroExperienceOrb expOrb = new HeroExperienceOrb(EntityRegistrar.HERO_EXPERIENCE_ORB.get(), pLevel);
        expOrb.followingHero = (LivingEntity) heroUnit;
        expOrb.isNeutral = isNeutral;
        expOrb.setPos(pX, pY, pZ);
        expOrb.setYRot((float)(RANDOM.nextDouble() * 360.0));
        expOrb.setDeltaMovement(
            (RANDOM.nextDouble() * 0.2 - 0.1),
            RANDOM.nextDouble() * 0.2,
            (RANDOM.nextDouble() * 0.2 - 0.1)
        );
        expOrb.value = pValue;
        expOrb.setNoGravity(true);
        return expOrb;
    }

    @Override
    public void tick() {

        this.horizontalCollision = false;
        this.verticalCollision = false;

        this.level().getProfiler().push("entityBaseTick");
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.checkBelowWorld();
        this.firstTick = false;
        this.level().getProfiler().pop();

        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        if (this.followingHero != null) {
            Vec3 vec3 = new Vec3(
                this.followingHero.getX() - this.getX(),
                this.followingHero.getY() + ((double)this.followingHero.getEyeHeight() / 2.0) - this.getY(),
                this.followingHero.getZ() - this.getZ()
            ).normalize();
            double dist = this.position().distanceTo(followingHero.position());
            double speed = Math.max(Math.min(1.0, dist * 0.001), 0.01f);
            Vec3 newDeltaMovement = this.getDeltaMovement().add(vec3.scale(speed));

            // Check if this movement step overshoots the target
            double nextDist = this.position().add(newDeltaMovement).distanceTo(followingHero.position());
            if (nextDist > dist) {
                // If moving further away, stop at target
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
            } else {
                // Otherwise, apply the calculated movement
                this.setDeltaMovement(newDeltaMovement);
            }
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        checkTouchedHero();

        ++this.age;
        if (this.age >= 1200 || followingHero == null || followingHero.isDeadOrDying()) {
            this.discard();
        }
    }

    private void checkTouchedHero() {
        if (followingHero != null && !followingHero.isDeadOrDying() && !this.level().isClientSide()) {
            AABB aabb = followingHero.getBoundingBox().inflate(0.5, 0.25, 0.5);
            if (aabb.contains(this.position())) {
                followingHero.take(this, count);
                if (count > 0 && (!isNeutral || ((HeroUnit) followingHero).getHeroLevel() < HeroUnit.MAX_NEUTRAL_EXP_LEVEL)) {
                    ((HeroUnit) followingHero).addExperience(value * 10);
                }
                this.discard();
            }
        }
    }

    @Override
    public void playerTouch(Player pEntity) { }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        return false;
    }
}
