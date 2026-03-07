package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.entities.AdjustableAreaEffectCloud;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.research.researchItems.ResearchWaterPotions;
import com.solegendary.reignofnether.unit.units.villagers.WitchUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ThrownPotion.class)
public abstract class ThrownPotionMixin extends ThrowableItemProjectile {

    protected ThrownPotionMixin(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
            method = "makeAreaOfEffectCloud",
            at = @At("HEAD"),
            cancellable = true
    )
    private void makeAreaOfEffectCloud(ItemStack pStack, Potion pPotion, CallbackInfo ci) {
        ci.cancel();

        AdjustableAreaEffectCloud aec = new AdjustableAreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
        if (this.getOwner() instanceof LivingEntity le) {
            aec.setOwner(le);
        }
        aec.diminishWithTimeAndUse = false;
        aec.setRadius(3.0F);
        aec.setRadiusOnUse(-0.5F);
        aec.setWaitTime(10);

        if (this.getOwner() instanceof WitchUnit witchUnit) {
            aec.setRadius(3.0F);
            aec.setRadiusOnUse(0);
            aec.setDurationOnUse(0);
            aec.setWaitTime(10);

            int duration = WitchUnit.LINGERING_POTION_DURATION;
            if (this.level().isClientSide() && ResearchClient.hasResearch(ProductionItems.RESEARCH_LINGERING_POTIONS)) {
                duration = WitchUnit.LINGERING_POTION_DURATION_EXTENDED;
            } else if (!this.level().isClientSide() && ResearchServerEvents.playerHasResearch(witchUnit.getOwnerName(), ProductionItems.RESEARCH_LINGERING_POTIONS)) {
                duration = WitchUnit.LINGERING_POTION_DURATION_EXTENDED;
            }
            aec.setDuration(duration);
        }

        aec.setRadiusPerTick(-aec.getRadius() / (float)aec.getDuration());
        aec.setPotion(pPotion);

        for (MobEffectInstance $$4 : PotionUtils.getCustomEffects(pStack)) {
            aec.addEffect(new MobEffectInstance($$4));
        }

        CompoundTag $$5 = pStack.getTag();
        if ($$5 != null && $$5.contains("CustomPotionColor", 99)) {
            aec.setFixedColor($$5.getInt("CustomPotionColor"));
        }

        this.level().addFreshEntity(aec);
    }

    private boolean isWaterPotion() {
        ItemStack item = this.getItem();
        Potion potion = PotionUtils.getPotion(item);
        List<MobEffectInstance> mei = PotionUtils.getMobEffects(item);
        return potion == Potions.WATER && mei.isEmpty();
    }

    // lingering potions should not collide with entities so their AOE cloud is better placed
    @Inject(
            method = "onHit",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHit(HitResult pResult, CallbackInfo ci) {
        ItemStack item = this.getItem();
        if (pResult.getType() == HitResult.Type.ENTITY &&
            item.getItem() instanceof LingeringPotionItem &&
            this.getOwner() instanceof WitchUnit) {
            ci.cancel();
        } else if (!this.level().isClientSide && isWaterPotion() && pResult.getType() == HitResult.Type.ENTITY) {
            reignofnether$dowseNearbyFires(((EntityHitResult)pResult).getEntity().blockPosition());
        }
    }

    @Shadow private void dowseFire(BlockPos pPos) { }

    @Inject(
            method = "onHitBlock",
            at = @At("TAIL")
    )
    protected void onHitBlock(BlockHitResult pResult, CallbackInfo ci) {
        if (!this.level().isClientSide) {
            boolean isWater = isWaterPotion();
            Direction dir = pResult.getDirection();
            BlockPos hitBp = pResult.getBlockPos();
            BlockPos adjBp = hitBp.relative(dir);
            if (isWater) {
                reignofnether$dowseNearbyFires(adjBp);
            }
        }
    }

    @Unique
    private void reignofnether$dowseNearbyFires(BlockPos bp) {
        float radius = ResearchWaterPotions.DOWSE_RADIUS;
        for (LivingEntity entity : MiscUtil.getEntitiesWithinRange(bp.getCenter(), radius, LivingEntity.class, level())) {
            if (entity.isOnFire() && entity.isAlive()) {
                entity.extinguishFire();
            }
            if (entity.isSensitiveToWater()) {
                entity.hurt(this.damageSources().indirectMagic(this, this.getOwner()), 1.0F);
            }
        }
        for (BlockPos bp2 : BlockPos.withinManhattan(bp, (int) radius, (int) radius, (int) radius)) {
            dowseFire(bp2);
        }
    }
}
