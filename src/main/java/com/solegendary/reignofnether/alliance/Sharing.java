package com.solegendary.reignofnether.alliance;

import com.solegendary.reignofnether.player.PlayerRoles;

import java.util.Collection;
import java.util.function.BiPredicate;

// the only place that answers "may this player use that owner's stuff?"
public class Sharing {

    public static boolean shares(String owner, String player, Share share) {
        return shares(share, owner, PlayerRoles.commanderOf(player), PlayerRoles.ownerOf(player),
                AlliancesServerEvents::isAllied, AlliancesServerEvents.playersWithAlliedControl);
    }

    // the rule itself, for either side (SharingClient passes the client's tables). faction: who the player fights
    // for; me: the owner they act as
    static boolean shares(Share share, String owner, String faction, String me,
                          BiPredicate<String, String> isAllied, Collection<String> alliedControl) {
        return switch (share) {
            // soldiers see what their commander sees, and through them their commander's allies
            case VISION -> faction.equals(owner) || isAllied.test(faction, owner);
            case CONTROL -> me.equals(owner) || (isAllied.test(me, owner) && alliedControl.contains(owner));
        };
    }
}
