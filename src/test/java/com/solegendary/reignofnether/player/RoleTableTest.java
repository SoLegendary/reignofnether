package com.solegendary.reignofnether.player;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoleTableTest {

    @Test
    void rolesOwnersAndFactions() {
        RoleTable roles = new RoleTable("lead"::equals);
        roles.set("co", PlayerRole.COMMANDER, "lead");
        roles.set("sol", PlayerRole.SOLDIER, "lead");

        assertEquals(PlayerRole.COMMANDER, roles.roleOf("lead"));
        assertEquals(PlayerRole.COMMANDER, roles.roleOf("co"));
        assertEquals(PlayerRole.SOLDIER, roles.roleOf("sol"));
        assertEquals(PlayerRole.SPECTATOR, roles.roleOf("someone"));
        assertEquals("lead", roles.ownerOf("co"));
        assertEquals("sol", roles.ownerOf("sol")); // a soldier acts as themselves
        assertEquals("lead", roles.commanderOf("sol"));
        assertEquals(List.of("sol"), RoleTable.namesFor(roles.soldierOf, "lead"));

        roles.set("sol", PlayerRole.SPECTATOR, "sol");
        assertEquals(PlayerRole.SPECTATOR, roles.roleOf("sol"));
    }
}
