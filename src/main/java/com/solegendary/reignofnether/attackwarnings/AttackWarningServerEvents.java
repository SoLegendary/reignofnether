package com.solegendary.reignofnether.attackwarnings;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AttackWarningServerEvents {

    // prevent potion damage effects from causing knockback
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent evt)  {
        if (evt.getEntity().getLevel().isClientSide())
            return;

        if (evt.getEntity() instanceof Unit unit)
            AttackWarningClientboundPacket.sendWarning(unit.getOwnerName(), evt.getEntity().getOnPos());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent evt) {
        if (!evt.getLevel().isClientSide())
            for (Building building : BuildingServerEvents.getBuildings())
                if (building.isPosPartOfBuilding(evt.getPos(), true))
                    AttackWarningClientboundPacket.sendWarning(building.ownerName, BuildingUtils.getCentrePos(building.getBlocks()));
    }
}
