package com.solegendary.reignofnether.entities;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.WindcallerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class WindcallerProjectile extends AbstractMagicProjectile {

    boolean levitationDealt = false;
    boolean damageDealt = false;

    public WindcallerProjectile(EntityType<? extends AbstractMagicProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel, ParticleTypes.CLOUD);
    }

    public WindcallerProjectile(Level pLevel, LivingEntity pShooter, double offsetX, double offsetY, double offsetZ) {
        super(EntityRegistrar.WINDCALLER_PROJECTILE.get(), pShooter, offsetX, offsetY, offsetZ, pLevel, ParticleTypes.CLOUD);
    }

    // pass through any
    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        if (!this.level().isClientSide && this.getOwner() instanceof AttackerUnit aUnit) {
            boolean isHurt = false;
            if (!damageDealt)
                isHurt = pResult.getEntity().hurt(damageSources().mobProjectile(this, (LivingEntity) this.getOwner()), aUnit.getUnitAttackDamage());
            if (isHurt)
                damageDealt = true;
            if (!(pResult.getEntity() instanceof WindcallerUnit) &&
                    aUnit instanceof WindcallerUnit windcallerUnit &&
                    (!(pResult.getEntity() instanceof Unit unit) || !AlliancesServerEvents.isAlliedOrOwned(windcallerUnit.getOwnerName(), unit.getOwnerName())) &&
                    pResult.getEntity() instanceof LivingEntity le &&
                    !le.hasEffect(MobEffects.LEVITATION) &&
                    (windcallerUnit.onGround() || ResearchServerEvents.playerHasResearch(windcallerUnit.getOwnerName(), ProductionItems.RESEARCH_UPGRADED_WINDCALLERS)) &&
                    !(pResult.getEntity() instanceof HeroUnit) &&
                    !levitationDealt) {
                ItemStack itemStack = ((LivingEntity) this.getOwner()).getItemBySlot(EquipmentSlot.MAINHAND);
                int gustLevel = itemStack.getEnchantmentLevel(EnchantmentRegistrar.GUST.get());
                le.addEffect(new MobEffectInstance(MobEffects.LEVITATION, WindcallerUnit.LEVITATE_TICKS, gustLevel, true, false));
                levitationDealt = true;
            }
            if (levitationDealt && damageDealt) {
                MiscUtil.addParticleExplosion(ParticleTypes.CLOUD, 10, level(), position());
                discard();
            }
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return EntityDimensions.scalable(1.0f, 1.0f);
    }
}
