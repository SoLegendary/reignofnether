package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.chat.TeamChatServerEvents.Scope;
import com.solegendary.reignofnether.player.PlayerRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeamChatServerEventsTest {

    @Test
    void playersTalkToTheirTeamOrEveryone() {
        assertEquals(Scope.TEAM, TeamChatServerEvents.scopeFor(PlayerRole.COMMANDER, "push mid", true));
        assertEquals(Scope.ALL, TeamChatServerEvents.scopeFor(PlayerRole.COMMANDER, "!gg", true));
        assertEquals(Scope.TEAM, TeamChatServerEvents.scopeFor(PlayerRole.SOLDIER, "on my way", true));
        assertEquals(Scope.ALL, TeamChatServerEvents.scopeFor(PlayerRole.SOLDIER, "!gg", true));
    }

    @Test
    void allChatOffKeepsEveryoneToTheirOwnGroup() {
        assertEquals(Scope.TEAM, TeamChatServerEvents.scopeFor(PlayerRole.COMMANDER, "!gg", false));
        assertEquals(Scope.TEAM, TeamChatServerEvents.scopeFor(PlayerRole.SOLDIER, "!gg", false));
        assertEquals(Scope.SPECTATOR, TeamChatServerEvents.scopeFor(PlayerRole.SPECTATOR, "!they're at the river", false));
    }

    @Test
    void spectatorsOnlyTalkToSpectators() {
        assertEquals(Scope.SPECTATOR, TeamChatServerEvents.scopeFor(PlayerRole.SPECTATOR, "nice fight", true));
        assertEquals(Scope.SPECTATOR, TeamChatServerEvents.scopeFor(PlayerRole.SPECTATOR, "!they're at the river", true));
    }
}
