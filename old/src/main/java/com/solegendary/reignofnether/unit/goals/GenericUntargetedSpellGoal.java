package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class GenericUntargetedSpellGoal extends Goal {

    private final LivingEntity mob;
    private Ability ability; // used for syncing cooldown with clientside
    private int ticksCasting = 0; // how long have we spent trying to cast this spell
    public boolean isCasting() { return isCasting; }
    public final int channelTicksMax;
    private boolean isCasting = false;

    private final Runnable onCast;
    private final UnitAnimationAction startAnimation;
    private final UnitAnimationAction stopAnimation;
    private final UnitAnimationAction castAnimation;

    public GenericUntargetedSpellGoal(LivingEntity mob,
                                      int channelTime,
                                      Runnable onCast,
                                      UnitAnimationAction startAnimation,
                                      UnitAnimationAction stopAnimation,
                                      UnitAnimationAction castAnimation) {
        this.mob = mob;
        this.channelTicksMax = channelTime;
        this.onCast = onCast;
        this.startAnimation = startAnimation;
        this.stopAnimation = stopAnimation;
        this.castAnimation = castAnimation;
    }

    public GenericUntargetedSpellGoal(LivingEntity mob,
                                      int channelTime,
                                      Runnable onCast,
                                      UnitAnimationAction startAnimation,
                                      UnitAnimationAction stopAnimation) {
        this(mob, channelTime, onCast, startAnimation, stopAnimation, null);
    }

    public void setAbility(Ability ability) {
        this.ability = ability;
    }

    @Override
    public void tick() {
        if (isCasting) {
            ticksCasting += 1;
            if (ticksCasting >= channelTicksMax) {
                if (!this.mob.level().isClientSide()) {
                    if (castAnimation != null) {
                        UnitAnimationClientboundPacket.sendBasicPacket(castAnimation, this.mob);
                    }
                    onCast.run();
                }
                if (this.ability != null && !this.mob.level().isClientSide()) {
                    if (!this.mob.level().isClientSide()) {
                        AbilityClientboundPacket.sendSetCooldownPacket(this.mob.getId(), this.ability.action, this.ability.cooldownMax);
                        if (mob instanceof HeroUnit heroUnit && this.ability instanceof HeroAbility heroAbility) {
                            heroUnit.setMana(heroUnit.getMana() - heroAbility.manaCost);
                        }
                    }
                    else if (mob instanceof Unit unit) {
                        this.ability.setToMaxCooldown();
                    }
                }
                this.ticksCasting = 0;
                this.isCasting = false;
            }
        }
    }

    @Override
    public boolean canUse() {
        return true;
    }

    public void startCasting() {
        this.isCasting = true;
        if (!this.mob.level().isClientSide() && startAnimation != null) {
            UnitAnimationClientboundPacket.sendBasicPacket(startAnimation, this.mob);
        }
    }

    @Override
    public void stop() {
        this.ticksCasting = 0;
        this.isCasting = false;
        if (!this.mob.level().isClientSide() && stopAnimation != null)
            UnitAnimationClientboundPacket.sendBasicPacket(stopAnimation, this.mob);
    }
}
