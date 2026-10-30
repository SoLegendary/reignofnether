package com.solegendary.reignofnether.voice;

import com.solegendary.reignofnether.player.PlayerRole;
import org.junit.jupiter.api.Test;

import static com.solegendary.reignofnether.player.PlayerRole.*;
import static com.solegendary.reignofnether.voice.VoiceChannel.*;
import static org.junit.jupiter.api.Assertions.*;

class VoiceChannelTest {

    private static boolean hears(VoiceChannel ch, PlayerRole speaker, PlayerRole listener, boolean sameFaction,
                                 boolean sameTeam, double distance, boolean allChat) {
        return VoiceChannel.canHear(ch, speaker, listener, sameFaction, sameTeam, distance, allChat);
    }

    @Test
    void commandersTalkToEveryoneOnTheirSide() {
        assertTrue(hears(TEAM, COMMANDER, COMMANDER, false, true, 500, false));
        assertFalse(hears(TEAM, COMMANDER, COMMANDER, false, false, 500, false));
        assertTrue(hears(TEAM, COMMANDER, SOLDIER, false, true, 500, false)); // allied soldiers too
        assertFalse(hears(TEAM, COMMANDER, SOLDIER, false, false, 5, false));
        assertFalse(hears(TEAM, COMMANDER, SPECTATOR, false, true, 5, false));
        assertFalse(hears(LOCAL, COMMANDER, SOLDIER, false, false, 5, true)); // their body follows the camera
    }

    @Test
    void squadIsOneFaction() {
        assertTrue(hears(SQUAD, COMMANDER, SOLDIER, true, true, 500, false));
        assertTrue(hears(SQUAD, SOLDIER, COMMANDER, true, true, 500, false));
        assertFalse(hears(SQUAD, SOLDIER, SOLDIER, false, true, 5, false)); // an ally's soldier
        assertFalse(hears(SQUAD, SOLDIER, SPECTATOR, true, true, 5, false));
    }

    @Test
    void localReachesNearbySoldiersOfAnyTeam() {
        assertTrue(hears(LOCAL, SOLDIER, SOLDIER, false, false, 20, false));
        assertFalse(hears(LOCAL, SOLDIER, SOLDIER, false, false, 40, false));
        assertFalse(hears(LOCAL, SOLDIER, COMMANDER, true, true, 5, false));
    }

    @Test
    void allOnlyWithAllChat() {
        assertTrue(hears(ALL, COMMANDER, SPECTATOR, false, false, 500, true));
        assertFalse(hears(ALL, SPECTATOR, COMMANDER, false, false, 500, true)); // no callouts from spectators
        assertTrue(hears(ALL, SPECTATOR, SPECTATOR, false, false, 500, true));
        assertFalse(hears(ALL, COMMANDER, COMMANDER, false, false, 500, false));
        assertFalse(hears(ALL, SOLDIER, COMMANDER, true, true, 5, true)); // soldiers have no ALL
    }

    @Test
    void spectatorsTeamIsSpectators() {
        assertTrue(hears(TEAM, SPECTATOR, SPECTATOR, false, false, 500, false));
        assertFalse(hears(TEAM, SPECTATOR, COMMANDER, false, false, 500, true));
    }
}
