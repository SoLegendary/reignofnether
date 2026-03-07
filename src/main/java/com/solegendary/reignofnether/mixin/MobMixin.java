package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.BlockUtils;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

// prevent vexes from charging enemies too far from their parent EvokerUnit
@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity {

    protected MobMixin(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
            method = "setTarget",
            at = @At("HEAD"),
            cancellable = true
    )
    public void setTarget(@Nullable LivingEntity pTarget, CallbackInfo ci) {
        if (pTarget == null || !pTarget.isAlive())
            return;

        Entity entity = pTarget.level().getEntity(this.getId());
        if (entity instanceof Vex vex &&
            vex.getOwner() instanceof EvokerUnit eu) {

            boolean targetIsAlliedPlayer;

            if (level().isClientSide()) {
                targetIsAlliedPlayer = pTarget instanceof Player player &&
                        (AlliancesClient.isAllied(player.getName().getString(), eu.getOwnerName()) ||
                            player.getName().getString().equals(eu.getOwnerName()));
            } else {
                targetIsAlliedPlayer = pTarget instanceof Player player &&
                        (AlliancesServerEvents.isAllied(player.getName().getString(), eu.getOwnerName()) ||
                            player.getName().getString().equals(eu.getOwnerName()));
            }
            boolean outOfRange = eu.distanceTo(pTarget) > eu.getVexTargetRange();
            if (outOfRange || targetIsAlliedPlayer)
                ci.cancel();
        }
    }

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    public void tick(CallbackInfo ci) {
        MobEffectInstance mei = this.getEffect(MobEffectRegistrar.FROST_DAMAGE.get());
        BlockState inBlockState = level().getBlockState(getOnPos().above());
        if (mei != null && mei.getDuration() > 0 && mei.getDuration() % 20 == 0 && onGround()) {
            int layers = BlockUtils.getWraithSnowLayers(inBlockState);
            boolean inIce = inBlockState.getBlock() == Blocks.PACKED_ICE;
            if (layers > 0 || inIce) {
                hurt(damageSources().magic(), layers + (inIce ? 3 : 0));
            }
        }
    }
}
