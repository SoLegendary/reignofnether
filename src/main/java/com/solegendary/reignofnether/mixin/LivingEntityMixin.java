package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.entities.BlazeUnitFireball;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.villagers.MilitiaUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
            method = "onChangedBlock",
            at = @At("TAIL"),
            cancellable = true
    )
    protected void onChangedBlock(BlockPos pPos, CallbackInfo ci) {
        Entity entity = this.level().getEntity(this.getId());

        if (!this.level().isClientSide() && entity instanceof Unit unit)
            if (SurvivalServerEvents.isEnabled() && SurvivalServerEvents.ENEMY_OWNER_NAME.equals(unit.getOwnerName())) {
                ci.cancel();
                FrostWalkerOnEntityMoved((LivingEntity) entity, this.level(), pPos, 1);
            }
    }

    // copied from FrostWalkerEnchantment.onEntityMoved
    private void FrostWalkerOnEntityMoved(LivingEntity pLiving, Level pLevel, BlockPos pPos, int pLevelConflicting) {
        if (pLiving.onGround()) {

            float f = (float)Math.min(16, 2 + pLevelConflicting);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            Iterator var7 = BlockPos.betweenClosed(pPos.offset((int) -f, (int) -1.0, (int) -f), pPos.offset((int) f, (int) -1.0, (int) f)).iterator();

            while(true) {
                BlockPos blockpos;
                BlockState blockstate1;
                do {
                    do {
                        if (!var7.hasNext()) {
                            return;
                        }
                        blockpos = (BlockPos)var7.next();
                    } while(!blockpos.closerToCenterThan(pLiving.position(), f));

                    blockpos$mutableblockpos.set(blockpos.getX(), blockpos.getY() + 1, blockpos.getZ());
                    blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos);
                } while(!blockstate1.isAir());

                BlockState blockstate2 = pLevel.getBlockState(blockpos);
                boolean isFull = blockstate2.getBlock() == Blocks.WATER && blockstate2.getValue(LiquidBlock.LEVEL) == 0;

                BlockState iceState = Blocks.FROSTED_ICE.defaultBlockState();
                if (blockstate2.getFluidState().is(FluidTags.WATER) && isFull && iceState.canSurvive(pLevel, blockpos) &&
                        pLevel.isUnobstructed(iceState, blockpos, CollisionContext.empty()) &&
                        !ForgeEventFactory.onBlockPlace(pLiving, BlockSnapshot.create(pLevel.dimension(), pLevel, blockpos), Direction.UP)) {

                    pLevel.setBlockAndUpdate(blockpos, iceState);
                    pLevel.scheduleTick(blockpos, Blocks.FROSTED_ICE, Mth.nextInt(pLiving.getRandom(), 60, 120));
                }

                isFull = blockstate2.getBlock() == Blocks.LAVA && blockstate2.getValue(LiquidBlock.LEVEL) == 0;
                BlockState magmaState = Blocks.NETHERRACK.defaultBlockState();
                if (blockstate2.getFluidState().is(FluidTags.LAVA) && isFull && magmaState.canSurvive(pLevel, blockpos) &&
                        pLevel.isUnobstructed(magmaState, blockpos, CollisionContext.empty()) &&
                        !ForgeEventFactory.onBlockPlace(pLiving, BlockSnapshot.create(pLevel.dimension(), pLevel, blockpos), Direction.UP)) {

                    pLevel.setBlockAndUpdate(blockpos, magmaState);
                    pLevel.scheduleTick(blockpos, Blocks.NETHERRACK, Mth.nextInt(pLiving.getRandom(), 60, 120));
                }
            }
        }
    }

    @Shadow public float getDamageAfterArmorAbsorb(DamageSource pDamageSource, float pDamageAmount) { return 0f; }
    @Shadow public float getDamageAfterMagicAbsorb(DamageSource pDamageSource, float pDamageAmount) { return 0f; }
    @Shadow public float getAbsorptionAmount() { return 0f; }
    @Shadow public void setAbsorptionAmount(float pAbsorptionAmount) { }
    @Shadow public CombatTracker getCombatTracker() { return null; }
    @Shadow public float getHealth() { return 0f; }
    @Shadow public void setHealth(float pHealth) { }

    @Inject(
            method = "actuallyHurt",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void actuallyHurt(DamageSource pDamageSource, float pDamageAmount, CallbackInfo ci) {


        // ensure projectiles from units do the damage of the unit, not the item,
        // and that armour and anti-armour effects are considered through absorption
        if ((pDamageSource.is(DamageTypeTags.IS_PROJECTILE) ||
            (!pDamageSource.is(DamageTypeTags.WITCH_RESISTANT_TO) &&
            !pDamageSource.is(DamageTypeTags.BYPASSES_SHIELD) &&
            !pDamageSource.is(DamageTypeTags.BYPASSES_ARMOR) &&
            !pDamageSource.is(DamageTypeTags.BYPASSES_RESISTANCE) &&
            pDamageSource.is(DamageTypes.MOB_ATTACK))) &&
            pDamageSource.getEntity() instanceof AttackerUnit attackerUnit) {

            ci.cancel();

            boolean isHuntableAnimal = ResourceSources.isHuntableAnimal((LivingEntity) (Object) this);

            float dmg = attackerUnit.getUnitAttackDamage();
            boolean isMelee = pDamageSource.is(DamageTypes.MOB_ATTACK) && !pDamageSource.is(DamageTypeTags.IS_PROJECTILE);
            if (isMelee && !(pDamageSource.getEntity() instanceof WorkerUnit))
                dmg += AttackerUnit.getWeaponDamageModifier(attackerUnit);

            if (isHuntableAnimal) {
                if (pDamageSource.getEntity() instanceof MilitiaUnit)
                    dmg = 1f;
                else if (pDamageSource.getEntity() instanceof VillagerUnit vUnit &&
                        vUnit.getUnitProfession() == VillagerUnitProfession.HUNTER) {
                    dmg = vUnit.isVeteran() ? 2f : 1.5f;
                } else if (!(pDamageSource.getEntity() instanceof WorkerUnit)) {
                    dmg *= 0.5f;
                }
            }

            if (this instanceof Unit unit) {
                dmg *= (1 - unit.getUnitPhysicalArmorPercentage());
                if (pDamageSource.is(DamageTypeTags.IS_PROJECTILE))
                    dmg *= (1 - unit.getUnitRangedArmorPercentage());
                dmg *= (1 - unit.getUnitResistPercentage());
            }

            if (!this.isInvulnerableTo(pDamageSource)) {
                dmg = ForgeHooks.onLivingHurt((LivingEntity) (Object) this, pDamageSource, dmg);
                if (dmg <= 0.0F) {
                    return;
                }
                dmg = this.getDamageAfterMagicAbsorb(pDamageSource, dmg);
                float f1 = Math.max(dmg - this.getAbsorptionAmount(), 0.0F);
                this.setAbsorptionAmount(this.getAbsorptionAmount() - (dmg - f1));
                float f = dmg - f1;
                if (f > 0.0F && f < 3.4028235E37F) {
                    Entity entity = pDamageSource.getEntity();
                    if (entity instanceof ServerPlayer) {
                        ServerPlayer serverplayer = (ServerPlayer)entity;
                        serverplayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(f * 10.0F));
                    }
                }
                ForgeHooks.onLivingDamage((LivingEntity) (Object) this, pDamageSource, f1);
                if (f1 != 0.0F) {
                    this.setHealth(this.getHealth() - f1);
                    this.getCombatTracker().recordDamage(pDamageSource, f1);
                    this.gameEvent(GameEvent.ENTITY_DAMAGE);
                }
            }
        }
    }

    @Shadow public boolean hasEffect(MobEffect pEffect) { return true; }
    @Shadow public MobEffectInstance getEffect(MobEffect pEffect) { return null; }

    @Inject(
            method = "baseTick",
            at = @At("TAIL")
    )
    public void baseTick(CallbackInfo ci) {
        if (!this.level().isClientSide && this.remainingFireTicks > 0 && !fireImmune() && hasEffect(MobEffectRegistrar.INTENSE_HEAT.get())) {
            int amp = Math.min(39, getEffect(MobEffectRegistrar.INTENSE_HEAT.get()).getAmplifier());
            int fireTicks = (this.remainingFireTicks + 10);
            if (fireTicks % (80 - (amp * 2)) == 0) {
                this.hurt(this.damageSources().onFire(), 1.0F);
            }
        }
    }
}
