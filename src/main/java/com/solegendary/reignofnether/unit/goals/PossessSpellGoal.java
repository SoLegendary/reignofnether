package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.ability.abilities.Possess;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.WraithUnit;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Consumer;

public class PossessSpellGoal extends GenericTargetedSpellGoal {

    WraithUnit wraithUnit;

    public PossessSpellGoal(WraithUnit wraith, int channelTicks, int range, UnitAnimationAction animAction,
                   Consumer<LivingEntity> onEntityCast) {
        super(wraith, channelTicks, range, animAction, onEntityCast, null, null);
        this.wraithUnit = wraith;
        this.bonusChannelingRange = Possess.BONUS_CHANNELING_RANGE;
        this.setOnStartChanneling((castPos) -> {
            if (targetEntity instanceof Unit unit) {
                int ticks = Possess.BASE_CHANNEL_TICKS + (unit.getCost().population * Possess.CHANNEL_TICKS_PER_POP_COST);
                this.channelTicksMax = Math.min(Possess.MAX_CHANNEL_TICKS, ticks);
            }
            if (!wraithUnit.level().isClientSide()) {
                SoundClientboundPacket.playFadeableLoopingSoundAtPos(
                        SoundAction.WRAITH_POSSESS_CHANNEL,
                        wraithUnit.blockPosition(),
                        0.5f,
                        wraith.getId(),
                        channelTicksMax
                );
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (isCasting && targetEntity != null) {
            if (targetEntity instanceof Unit unit && unit.getOwnerName().equals(wraithUnit.getOwnerName()))
                Unit.fullResetBehaviours(wraithUnit);
            doPossessParticles();
            if (wraithUnit.tickCount % 10 == 0) {
                targetEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 15, 0, true, false));
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (!wraithUnit.level().isClientSide())
            SoundClientboundPacket.stopSoundWithId(wraithUnit.getId());
        if (this.mob.level().isClientSide() && !Keybindings.shiftMod.isDown())
            ((Unit) this.mob).getCheckpoints().clear();
    }

    public void doPossessParticles() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        if (this.targetEntity == null) return;
        if (targetEntity.tickCount % 2 != 0) return;

        double dx = targetEntity.getX() - mob.getX();
        double dy = (targetEntity.getY() + targetEntity.getBbHeight() * 0.5) - (mob.getY() + mob.getBbHeight() * 0.5);
        double dz = targetEntity.getZ() - mob.getZ();
        double distSqr = dx * dx + dy * dy + dz * dz;
        double dist = Math.sqrt(distSqr);
        double distSqrt = Math.sqrt(dist);
        if (dist == 0) return;

        double speed = distSqrt * 0.225;
        double vx = (dx / dist) * speed;
        double vy = (dy / dist) * speed;
        double vz = (dz / dist) * speed;

        double originX = mob.getX();
        double originY = mob.getY() + mob.getBbHeight() * 0.75;
        double originZ = mob.getZ();

        RandomSource rand = mob.getRandom();
        serverLevel.sendParticles(
                ParticleTypes.SCULK_SOUL,
                originX + rand.nextGaussian() * 0.2,
                originY + rand.nextGaussian() * 0.2,
                originZ + rand.nextGaussian() * 0.2,
                0,
                vx, vy, vz,
                speed
        );
    }
}
