package com.solegendary.reignofnether.alliance;

import com.solegendary.reignofnether.player.PlayerRoles;

// the only place that answers "may this player use that owner's stuff?"
public class Sharing {

    public static boolean shares(String owner, String player, Share share) {
        return switch (share) {
            // soldiers see what their commander sees, and through them their commander's allies
            case VISION -> {
                String faction = PlayerRoles.commanderOf(player);
                yield faction.equals(owner) || AlliancesServerEvents.isAllied(faction, owner);
            }
            case CONTROL -> {
                String me = PlayerRoles.ownerOf(player);
                yield me.equals(owner) || (AlliancesServerEvents.isAllied(me, owner) &&
                        AlliancesServerEvents.playersWithAlliedControl.contains(owner));
            }
        };
    }
}
