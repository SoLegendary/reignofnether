package com.solegendary.reignofnether.player;

// what a connected player is in the current match. Co-commanders are commanders that share a lead's faction
public enum PlayerRole {
    COMMANDER(true, true),
    SOLDIER(true, true),
    SPECTATOR(false, false);

    public final boolean inMatch; // can act on the match: break, attack, loot, talk to their team
    public final boolean hasFog;  // only sees what their faction sees

    PlayerRole(boolean inMatch, boolean hasFog) {
        this.inMatch = inMatch;
        this.hasFog = hasFog;
    }
}
