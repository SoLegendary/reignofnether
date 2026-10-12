package com.solegendary.reignofnether.attackwarnings;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class AttackWarningServerEvents {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre evt)  {
        if (evt.getEntity().level().isClientSide())
            return;

        if (evt.getEntity() instanceof Unit unit &&
                !evt.getSource().is(DamageTypeTags.IS_FALL) &&
                evt.getSource() != evt.getEntity().damageSources().starve() &&
                evt.getSource() != evt.getEntity().damageSources().inWall() &&
                evt.getSource() != evt.getEntity().damageSources().outOfBorder())
            AttackWarningClientboundPacket.sendWarning(unit.getOwnerName(), evt.getEntity().getOnPos());
    }
}
