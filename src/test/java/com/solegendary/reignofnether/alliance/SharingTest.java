package com.solegendary.reignofnether.alliance;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

class SharingTest {

    private static final BiPredicate<String, String> A_AND_B_ALLIED =
            (x, y) -> Set.of(x, y).equals(Set.of("a", "b"));

    @Test
    void visionFollowsTheFactionAndItsAllies() {
        // a soldier of a: faction a, acts as themselves
        assertTrue(Sharing.shares(Share.VISION, "a", "a", "soldier", A_AND_B_ALLIED, Set.of()));
        assertTrue(Sharing.shares(Share.VISION, "b", "a", "soldier", A_AND_B_ALLIED, Set.of()));
        assertFalse(Sharing.shares(Share.VISION, "c", "a", "soldier", A_AND_B_ALLIED, Set.of()));
    }

    @Test
    void controlNeedsOwnershipOrAlliedControl() {
        // a co-commander of a acts as a
        assertTrue(Sharing.shares(Share.CONTROL, "a", "a", "a", A_AND_B_ALLIED, Set.of()));
        assertFalse(Sharing.shares(Share.CONTROL, "b", "a", "a", A_AND_B_ALLIED, Set.of()));
        assertTrue(Sharing.shares(Share.CONTROL, "b", "a", "a", A_AND_B_ALLIED, Set.of("b")));
        // a soldier never controls their commander's stuff
        assertFalse(Sharing.shares(Share.CONTROL, "a", "a", "soldier", A_AND_B_ALLIED, Set.of()));
    }
}
