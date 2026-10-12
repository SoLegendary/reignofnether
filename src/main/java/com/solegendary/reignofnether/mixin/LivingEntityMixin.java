package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.util.MobEffectHelpers;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.villagers.MilitiaUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.ParticleUtil;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    private static final float CRITICAL_HIT_MULTIPLIER = 3f;

    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    public void tick(CallbackInfo ci) {
        if (this.level().isClientSide())
            if (this.hasEffect(MobEffects.LEVITATION))
                MiscUtil.spawnFlyingCloudParticles(this);
    }

    @Inject(
            method = "onChangedBlock",
            at = @At("TAIL")
    )
    protected void onChangedBlock(ServerLevel pLevel, BlockPos pPos, CallbackInfo ci) {
        LivingEntity le = (LivingEntity) (Object) this;

        if (!this.level().isClientSide() && le instanceof Unit unit) {
            boolean canFrost = false;
            boolean canMagma = false;
            boolean canNetherrack = false;

            if (SurvivalServerEvents.isEnabled() && SurvivalServerEvents.ENEMY_OWNER_NAME.equals(unit.getOwnerName())) {
                canFrost = true;
                canNetherrack = true;
            }
            if (le instanceof UnitInventory inv) {
                if (inv.isHolding(UnitItems.FROST_WALKER_BOOTS))
                    canFrost = true;
                if (inv.isHolding(UnitItems.MAGMA_WALKER_BOOTS))
                    canMagma = true;
            }
            liquidWalkerOnEntityMoved(le, pPos, 1, canFrost, canMagma, canNetherrack);
        }
    }

    // copied from FrostWalkerEnchantment.onEntityMoved, extended with 1.5.0's magma and
    // netherrack modes (each is decided per unit by the boots it holds)
    private void liquidWalkerOnEntityMoved(LivingEntity pLiving, BlockPos pPos, int pWalkerLevel,
                                           boolean doFrost, boolean doMagma, boolean doNetherrack) {
        if (!doFrost && !doMagma && !doNetherrack)
            return;
        if (!pLiving.onGround())
            return;

        Level pLevel = pLiving.level();
        float f = (float) Math.min(16, 2 + pWalkerLevel);
        int r = (int) f;
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        Vec3 livingPos = pLiving.position();

        for (BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-r, -1, -r), pPos.offset(r, -1, r))) {
            if (!blockpos.closerToCenterThan(livingPos, f))
                continue;

            // only convert liquid that has open air above it
            above.set(blockpos.getX(), blockpos.getY() + 1, blockpos.getZ());
            if (!pLevel.getBlockState(above).isAir())
                continue;

            BlockState state = pLevel.getBlockState(blockpos);
            Block block = state.getBlock();

            // cheap early-out: only source liquid blocks are ever converted
            if (block != Blocks.WATER && block != Blocks.LAVA)
                continue;
            if (state.getValue(LiquidBlock.LEVEL) != 0)
                continue;

            if (doFrost && block == Blocks.WATER)
                tryReplaceLiquid(pLiving, pLevel, blockpos, state, FluidTags.WATER, Blocks.FROSTED_ICE);
            else if (doMagma && block == Blocks.LAVA)
                tryReplaceLiquid(pLiving, pLevel, blockpos, state, FluidTags.LAVA,
                        BlockRegistrar.TEMPORARY_WALKABLE_MAGMA_BLOCK.get());
            else if (doNetherrack && block == Blocks.LAVA)
                tryReplaceLiquid(pLiving, pLevel, blockpos, state, FluidTags.LAVA, Blocks.NETHERRACK);
        }
    }

    private void tryReplaceLiquid(LivingEntity pLiving, Level pLevel, BlockPos pos, BlockState liquidState,
                                  TagKey<Fluid> fluidTag, Block replacement) {
        BlockState newState = replacement.defaultBlockState();
        if (liquidState.getFluidState().is(fluidTag) &&
                newState.canSurvive(pLevel, pos) &&
                pLevel.isUnobstructed(newState, pos, CollisionContext.empty()) &&
                !EventHooks.onBlockPlace(pLiving,
                        BlockSnapshot.create(pLevel.dimension(), pLevel, pos), Direction.UP)) {

            pLevel.setBlockAndUpdate(pos, newState);
            pLevel.scheduleTick(pos, replacement, Mth.nextInt(pLiving.getRandom(), 60, 120));
        }
    }

    @Shadow protected float getDamageAfterArmorAbsorb(DamageSource pDamageSource, float pDamageAmount) { return 0f; }
    @Shadow public boolean isInvulnerableTo(DamageSource pDamageSource) { return false; }

    // In 1.20.1 this hook cancelled actuallyHurt and re-applied the damage by hand, because the
    // Forge damage hooks (onLivingHurt/onLivingDamage) had to be called explicitly there. NeoForge
    // 1.21.1 routes the whole thing through DamageContainer: LivingEntity#hurt pushes a container,
    // fires CommonHooks.onEntityIncomingDamage, and actuallyHurt then reads it back. So instead of
    // cancelling and hand-rolling the damage, we rewrite the incoming damage on the container and
    // let vanilla apply armour/absorption/enchantments/game events as usual. The one behaviour the
    // old copy had was skipping vanilla armour absorption (units use their own armour percentage),
    // which the second redirect preserves.
    @Redirect(
            method = "hurt",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onEntityIncomingDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)Z")
    )
    private boolean ron$applyUnitAttackDamage(LivingEntity pSelf, DamageContainer pContainer) {
        float dmg = this.ron$unitAttackDamage(pContainer.getSource());
        if (dmg < 0.0F) // not a unit hit, let the vanilla path (and the event) run untouched
            return CommonHooks.onEntityIncomingDamage(pSelf, pContainer);

        if (dmg <= 0.0F || pSelf.isInvulnerableTo(pContainer.getSource()))
            return true; // ci.cancel() in the old copy: no damage at all

        pContainer.setNewDamage(dmg);
        return CommonHooks.onEntityIncomingDamage(pSelf, pContainer);
    }

    @Redirect(
            method = "actuallyHurt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
    )
    // INVOKEVIRTUAL keeps the receiver in the handler's parameters, so the entity being damaged
    // comes first; the rest are the target call's own arguments.
    private float ron$skipVanillaArmour(LivingEntity pSelf, DamageSource pDamageSource, float pAmount) {
        // unit armour is applied as a percentage on the incoming damage instead
        if (pDamageSource.getEntity() instanceof AttackerUnit)
            return pAmount;
        return this.getDamageAfterArmorAbsorb(pDamageSource, pAmount);
    }

    // returns -1 when the hit is not an attacker-unit hit and must be left to vanilla
    private float ron$unitAttackDamage(DamageSource pDamageSource) {
        boolean isProjectile = pDamageSource.is(DamageTypeTags.IS_PROJECTILE);
        boolean isMelee = pDamageSource.is(DamageTypes.MOB_ATTACK) && !isProjectile;

        // the old copy only hijacked plain hits: projectiles, or melee that isn't bypassing
        // shields/armour/resistance
        if (!isProjectile && !(isMelee &&
                !pDamageSource.is(DamageTypeTags.WITCH_RESISTANT_TO) &&
                !pDamageSource.is(DamageTypeTags.BYPASSES_SHIELD) &&
                !pDamageSource.is(DamageTypeTags.BYPASSES_ARMOR) &&
                !pDamageSource.is(DamageTypeTags.BYPASSES_RESISTANCE)))
            return -1.0F;

        if (!(pDamageSource.getEntity() instanceof AttackerUnit attackerUnit))
            return -1.0F;

        // ensure projectiles from units do the damage of the unit, not the item,
        // and that armour and anti-armour effects are considered through absorption
        boolean isHuntableAnimal = ResourceSources.isHuntableAnimal((LivingEntity) (Object) this);

        float dmg = attackerUnit.getUnitAttackDamage();
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
            if (isProjectile)
                dmg *= (1 - unit.getUnitRangedArmorPercentage());
            dmg *= (1 - unit.getUnitResistPercentage());

            // 1.5.0 critical hits (the critical_hit_chance attribute): triple damage with a sound
            // and particles, rolled server side only so the client does not predict extra damage
            if (!this.level().isClientSide() && getRandom().nextFloat() < attackerUnit.getCriticalChance()) {
                dmg *= CRITICAL_HIT_MULTIPLIER;
                SoundClientboundPacket.playSoundAtPos(SoundAction.CRITICAL_HIT, this.blockPosition());
                ParticleUtil.addParticleExplosion(ParticleRegistrar.FLOATING_CRIT.get(), 10,
                        ((Entity) attackerUnit).level(), this.getEyePosition());
            }
        }

        return dmg;
    }

    // 1.5.0's phasing effect: the afflicted unit walks through other mobs and ignores knockback
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    public void ron$phasingNotPushable(CallbackInfoReturnable<Boolean> cir) {
        if (this.hasEffect(MobEffectHelpers.holder(MobEffectRegistrar.PHASING.get())))
            cir.setReturnValue(false);
    }

    @Inject(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void ron$phasingNoPush(Entity pEntity, CallbackInfo ci) {
        if (this.hasEffect(MobEffectHelpers.holder(MobEffectRegistrar.PHASING.get())))
            ci.cancel();
    }

    @Inject(
            method = "doPush(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void ron$phasingNoDoPush(Entity pEntity, CallbackInfo ci) {
        if (this.hasEffect(MobEffectHelpers.holder(MobEffectRegistrar.PHASING.get())))
            ci.cancel();
    }

    @Shadow public boolean hasEffect(Holder<MobEffect> pEffect) { return true; }
    @Shadow public MobEffectInstance getEffect(Holder<MobEffect> pEffect) { return null; }

    // MobEffectEvent.Added stopped being cancellable in 1.21.1, so uninterruptable units drop
    // interrupting effects here instead, before they are ever added.
    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ron$blockInterrupt(MobEffectInstance pInstance, Entity pSource, CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof Unit unit && unit.uninterruptable()
                && MobEffectRegistrar.isInterrupt(pInstance.getEffect()))
            cir.setReturnValue(false);
    }

    @Inject(
            method = "baseTick",
            at = @At("TAIL")
    )
    public void baseTick(CallbackInfo ci) {
        Holder<MobEffect> intenseHeat = MobEffectHelpers.holder(MobEffectRegistrar.INTENSE_HEAT.get());
        if (!this.level().isClientSide && this.remainingFireTicks > 0 && !fireImmune() && hasEffect(intenseHeat)) {
            int amp = Math.min(39, getEffect(intenseHeat).getAmplifier());
            int fireTicks = (this.remainingFireTicks + 10);
            if (fireTicks % (80 - (amp * 2)) == 0) {
                this.hurt(this.damageSources().onFire(), 1.0F);
            }
        }
    }
}
