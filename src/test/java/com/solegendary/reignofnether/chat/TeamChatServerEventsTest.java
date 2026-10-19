package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.chat.TeamChatServerEvents.Scope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeamChatServerEventsTest {

    @Test
    void playersTalkToTheirTeamOrEveryone() {
        assertEquals(Scope.TEAM, TeamChatServerEvents.scopeFor(true, "push mid"));
        assertEquals(Scope.ALL, TeamChatServerEvents.scopeFor(true, "!gg"));
    }

    @Test
    void spectatorsNeverReachPlayers() {
        assertEquals(Scope.SPECTATOR, TeamChatServerEvents.scopeFor(false, "they're at the river"));
        assertEquals(Scope.SPECTATOR, TeamChatServerEvents.scopeFor(false, "!they're at the river"));
    }
}
