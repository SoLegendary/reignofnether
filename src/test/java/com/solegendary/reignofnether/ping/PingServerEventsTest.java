package com.solegendary.reignofnether.ping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingServerEventsTest {

    @BeforeEach
    void forgetEarlierPings() {
        PingServerEvents.forget("a");
        PingServerEvents.forget("b");
    }

    @Test
    void pingsAreRateLimited() {
        assertTrue(PingServerEvents.allow("a", 0));
        assertTrue(PingServerEvents.allow("a", 100));
        assertTrue(PingServerEvents.allow("a", 200));
        assertFalse(PingServerEvents.allow("a", 300));
        assertTrue(PingServerEvents.allow("b", 300));
        assertTrue(PingServerEvents.allow("a", 5201));
    }
}
