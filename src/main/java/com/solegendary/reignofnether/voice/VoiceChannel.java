package com.solegendary.reignofnether.voice;

import com.solegendary.reignofnether.player.PlayerRole;

import java.util.List;

// who a player's voice reaches during a match (RonVoicechatPlugin). No Simple Voice Chat types here, so this
// loads and is testable without it
public enum VoiceChannel {
    LOCAL, // soldiers close by, any team
    SQUAD, // your commander(s) and their soldiers
    TEAM,  // commanders: everyone playing on your side; spectators: other spectators
    ALL;   // everyone, when the allChat rule is on (from a spectator: other spectators only)

    public static final double LOCAL_RANGE = 32;

    public static List<VoiceChannel> allowedFor(PlayerRole role) {
        return switch (role) {
            case COMMANDER -> List.of(TEAM, SQUAD, ALL); // never LOCAL: their body follows the RTS camera
            case SOLDIER -> List.of(SQUAD, LOCAL);
            case SPECTATOR -> List.of(TEAM, ALL);
        };
    }

    public static VoiceChannel defaultFor(PlayerRole role) {
        return allowedFor(role).get(0);
    }

    /**
     * @param sameFaction speaker and listener play for the same commander (PlayerRoles.commanderOf)
     * @param sameTeam    same faction or allied factions
     */
    public static boolean canHear(VoiceChannel channel, PlayerRole speaker, PlayerRole listener,
                                  boolean sameFaction, boolean sameTeam, double distance, boolean allChat) {
        if (!allowedFor(speaker).contains(channel))
            return false;
        return switch (channel) {
            case ALL -> allChat && (speaker.inMatch || !listener.inMatch); // spectators can't call out to players
            case TEAM -> speaker == PlayerRole.COMMANDER ? listener.inMatch && sameTeam
                    : listener == PlayerRole.SPECTATOR;
            case SQUAD -> listener.inMatch && sameFaction;
            case LOCAL -> listener == PlayerRole.SOLDIER && distance <= LOCAL_RANGE;
        };
    }
}
