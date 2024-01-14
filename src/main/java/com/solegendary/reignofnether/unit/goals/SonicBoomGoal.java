package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.function.Consumer;

public class SonicBoomGoal extends AbstractCastTargetedSpellGoal {

    public SonicBoomGoal(Mob mob, int channelTicks, int range, Consumer<LivingEntity> onCast) {
        super(mob, channelTicks, range, false, onCast);
    }

    @Override
    public void startCasting() {
        super.startCasting();
        if (!this.mob.level.isClientSide())
            UnitSyncClientboundPacket.sendSyncAnimationPacket(this.mob, true);
    }

    @Override
    public void stopCasting() {
        if (!this.mob.level.isClientSide() && ticksCasting < channelTicks)
            UnitSyncClientboundPacket.sendSyncAnimationPacket(this.mob, false);
        super.stopCasting();
        ((Unit) this.mob).getCheckpoints().clear();
    }

    @Override
    public void stop() {
        // hack fix to stop a weird bug where it gets stopped unexpectedly (serverside)
        // happens when needing to move towards the target first
        if (this.ticksCasting <= 2 && isInRange())
            return;
        super.stop();
    }
}
