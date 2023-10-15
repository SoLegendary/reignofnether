package com.solegendary.reignofnether.unit.goals;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

import java.util.function.Consumer;

public class CastFangsLineGoal extends AbstractCastTargetedSpellGoal {

    public CastFangsLineGoal(Mob mob, int channelTicks, int range, Consumer<BlockPos> onCast) {
        super(mob, channelTicks, range, true, onCast);
    }

    @Override
    public void startCasting() {
        super.startCasting();
        if (!this.mob.level.isClientSide())
            UnitSyncClientboundPacket.sendSyncAnimationPacket(this.mob, true);
    }

    @Override
    public void stopCasting() {
        super.stopCasting();
        if (!this.mob.level.isClientSide())
            UnitSyncClientboundPacket.sendSyncAnimationPacket(this.mob, false);
        ((Unit) this.mob).getCheckpoints().clear();
    }
}
