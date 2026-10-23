package com.solegendary.reignofnether.alliance;

import com.solegendary.reignofnether.player.PlayerRolesClient;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.LivingEntity;

// client mirror of Sharing, for the local player
public class SharingClient {

    public static boolean shares(String owner, Share share) {
        return switch (share) {
            case VISION -> {
                String faction = PlayerRolesClient.myCommander();
                yield faction.equals(owner) || AlliancesClient.isAllied(faction, owner);
            }
            case CONTROL -> {
                String me = PlayerRolesClient.myOwner();
                yield me.equals(owner) || (AlliancesClient.isAllied(me, owner) &&
                        AlliancesClient.playersWithAlliedControl.contains(owner));
            }
        };
    }

    public static boolean canControl(String owner) {
        return shares(owner, Share.CONTROL);
    }

    public static boolean canControl(Unit unit) {
        return canControl(unit.getOwnerName());
    }

    public static boolean canControl(LivingEntity entity) {
        return entity instanceof Unit unit && canControl(unit.getOwnerName());
    }
}
