package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

// similar to castFangsGoal but does not require targeting, and so is not a MoveToTargetBlockGoal
public class CastFangsCircleGoal extends Goal {

    private final LivingEntity mob;
    private Ability ability; // used for syncing cooldown with clientside
    private int ticksCasting = 0; // how long have we spent trying to cast this spell
    public boolean isCasting() { return isCasting; }
    public final static int TICKS_CASTING_MAX = 1 * ResourceCost.TICKS_PER_SECOND; // max time required to cast a spell
    private boolean isCasting = false;

    public CastFangsCircleGoal(LivingEntity mob) {
        this.mob = mob;
    }

    public void setAbility(Ability ability) {
        this.ability = ability;
    }

    @Override
    public void tick() {
        if (isCasting) {
            ticksCasting += 1;
            if (ticksCasting >= TICKS_CASTING_MAX) {
                ((EvokerUnit) this.mob).createEvokerFangsCircle();

                if (this.ability != null && !this.mob.level.isClientSide())
                    AbilityClientboundPacket.sendSetCooldownPacket(this.mob.getId(), this.ability.action, this.ability.cooldownMax);
                this.stop();
            }
        }
    }

    @Override
    public boolean canUse() {
        return true;
    }

    public void startCasting() {
        this.isCasting = true;
        if (!this.mob.level.isClientSide())
            UnitSyncClientboundPacket.sendSyncCastingAnimationPacket(this.mob, true);
    }

    @Override
    public void stop() {
        this.ticksCasting = 0;
        this.isCasting = false;
        if (!this.mob.level.isClientSide())
            UnitSyncClientboundPacket.sendSyncCastingAnimationPacket(this.mob, false);
    }
}
