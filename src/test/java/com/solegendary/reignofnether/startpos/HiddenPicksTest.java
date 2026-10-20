package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.faction.Factions;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HiddenPicksTest {

    private static StartPos seat(int x, int team, String name) {
        StartPos sp = new StartPos(new BlockPos(x, 0, 0), team);
        sp.playerName = name;
        sp.faction = name.isBlank() ? Factions.NONE : Factions.VILLAGERS;
        return sp;
    }

    @Test
    void onlyOtherTeamsTakenSeatsAreHidden() {
        StartPos mine = seat(0, 1, "me"), mate = seat(1, 1, "mate"), enemy = seat(2, 2, "enemy"), empty = seat(3, 2, "");
        assertFalse(StartPosServerEvents.isHiddenFrom(mine, mine, true));
        assertFalse(StartPosServerEvents.isHiddenFrom(mate, mine, true));
        assertTrue(StartPosServerEvents.isHiddenFrom(enemy, mine, true));
        assertFalse(StartPosServerEvents.isHiddenFrom(empty, mine, true));
        assertFalse(StartPosServerEvents.isHiddenFrom(enemy, mine, false));
    }

    @Test
    void noSeatMeansNoTakenSeatIsShown() {
        assertTrue(StartPosServerEvents.isHiddenFrom(seat(0, 1, "a"), null, true));
        assertFalse(StartPosServerEvents.isHiddenFrom(seat(0, 1, ""), null, true));
    }
}
