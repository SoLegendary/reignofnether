package com.solegendary.reignofnether.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorldStateTest {

    @Test
    void eachSideOnlyClearsItsOwnState() {
        List<String> clientList = WorldState.client(new ArrayList<>(List.of("a")));
        Map<String, Integer> serverMap = WorldState.server(new HashMap<>(Map.of("b", 1)));
        boolean[] flag = { true };
        WorldState.onClientReset(() -> flag[0] = false);

        WorldState.resetServer();
        assertTrue(serverMap.isEmpty());
        assertEquals(List.of("a"), clientList);
        assertTrue(flag[0]);

        WorldState.resetClient();
        assertTrue(clientList.isEmpty());
        assertFalse(flag[0]);
    }
}
