package com.solegendary.reignofnether.hud.layout;

import com.solegendary.reignofnether.hud.layout.CommandGrid.Command;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static com.solegendary.reignofnether.hud.layout.CommandGrid.*;
import static org.junit.jupiter.api.Assertions.*;

class CommandGridTest {

    private static List<String> abilities(int n) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < n; i++)
            list.add("a" + i);
        return list;
    }

    @Test
    void commandsTakeTheirPreferredSlots() {
        var layout = assign(List.of(
                new Command<>("attack", SLOT_A),
                new Command<>("stop", SLOT_S),
                new Command<>("hold", SLOT_D),
                new Command<>("garrison", SLOT_F)), abilities(2), 0);
        assertEquals("attack", layout.slots().get(SLOT_A));
        assertEquals("stop", layout.slots().get(SLOT_S));
        assertEquals("hold", layout.slots().get(SLOT_D));
        assertEquals("garrison", layout.slots().get(SLOT_F));
        assertEquals("a0", layout.slots().get(0));
        assertEquals("a1", layout.slots().get(1));
        assertEquals(-1, layout.pageSlot());
    }

    @Test
    void clashingCommandsMoveToTheNextFreeCommandSlot() {
        var layout = assign(List.of(
                new Command<>("build", SLOT_F),
                new Command<>("garrison", SLOT_F),
                new Command<>("callToArms", NO_PREFERENCE)), List.of(), 0);
        assertEquals("build", layout.slots().get(SLOT_F));
        assertEquals("garrison", layout.slots().get(SLOT_A));
        assertEquals("callToArms", layout.slots().get(SLOT_S));
    }

    @Test
    void abilitiesFillTopRowThenBottomRow() {
        var layout = assign(List.of(new Command<>("stop", SLOT_S)), abilities(7), 0);
        for (int i = 0; i < 5; i++)
            assertEquals("a" + i, layout.slots().get(i));
        assertEquals("a5", layout.slots().get(10));
        assertEquals("a6", layout.slots().get(11));
    }

    @Test
    void overflowPagesWithoutDuplicatesOrLosses() {
        List<Command<String>> commands = List.of(new Command<>("stop", SLOT_S), new Command<>("gather", SLOT_G));
        List<String> abs = abilities(20);
        var first = assign(commands, abs, 0);
        assertEquals(SLOT_B, first.pageSlot());
        assertTrue(first.pageCount() >= 2);
        assertNull(first.slots().get(SLOT_B));

        var seen = new HashSet<String>();
        for (int p = 0; p < first.pageCount(); p++) {
            var page = assign(commands, abs, p);
            assertEquals("stop", page.slots().get(SLOT_S));
            assertEquals("gather", page.slots().get(SLOT_G));
            page.slots().stream().filter(Objects::nonNull).filter(s -> s.startsWith("a")).forEach(s -> assertTrue(seen.add(s), s));
        }
        assertEquals(20, seen.size());
    }

    @Test
    void pageWrapsAround() {
        var a = assign(List.of(), abilities(30), 0);
        var b = assign(List.of(), abilities(30), a.pageCount());
        assertEquals(a.slots(), b.slots());
    }
}
