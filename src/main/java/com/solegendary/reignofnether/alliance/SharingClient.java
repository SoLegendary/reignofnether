package com.solegendary.reignofnether.alliance;

import com.solegendary.reignofnether.player.PlayerRolesClient;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.LivingEntity;

// client mirror of Sharing, for the local player
public class SharingClient {

    public static boolean canControl(String owner) {
        return Sharing.shares(Share.CONTROL, owner, PlayerRolesClient.myCommander(), PlayerRolesClient.myOwner(),
                AlliancesClient::isAllied, AlliancesClient.playersWithAlliedControl);
    }

    public static boolean canControl(Unit unit) {
        return canControl(unit.getOwnerName());
    }

    public static boolean canControl(LivingEntity entity) {
        return entity instanceof Unit unit && canControl(unit.getOwnerName());
    }
}
