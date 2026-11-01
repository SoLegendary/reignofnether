package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.player.PlayerRole;
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
        assertFalse(StartPosServerEvents.isHiddenFrom(mine, mine, 1, true));
        assertFalse(StartPosServerEvents.isHiddenFrom(mate, mine, 1, true));
        assertTrue(StartPosServerEvents.isHiddenFrom(enemy, mine, 1, true));
        assertFalse(StartPosServerEvents.isHiddenFrom(empty, mine, 1, true));
        assertFalse(StartPosServerEvents.isHiddenFrom(enemy, mine, 1, false));
    }

    @Test
    void noTeamMeansNoTakenSeatIsShown() {
        assertTrue(StartPosServerEvents.isHiddenFrom(seat(0, 1, "a"), null, null, true));
        assertFalse(StartPosServerEvents.isHiddenFrom(seat(0, 1, ""), null, null, true));
    }

    @Test
    void aSoldiersTeamComesFromTheirJoin() {
        // a soldier has no seat of their own, only the team from their join
        assertFalse(StartPosServerEvents.isHiddenFrom(seat(0, 1, "lead"), null, 1, true));
        assertTrue(StartPosServerEvents.isHiddenFrom(seat(1, 2, "enemy"), null, 1, true));
    }

    @Test
    void joinsAreHiddenFromOtherTeams() {
        SeatJoin soldier = new SeatJoin("s", PlayerRole.SOLDIER, 2, BlockPos.ZERO);
        assertFalse(StartPosServerEvents.isHiddenFrom(soldier, "s", null, true));
        assertFalse(StartPosServerEvents.isHiddenFrom(soldier, "mate", 2, true));
        assertTrue(StartPosServerEvents.isHiddenFrom(soldier, "enemy", 1, true));
        assertTrue(StartPosServerEvents.isHiddenFrom(soldier, "nobody", null, true));
        assertFalse(StartPosServerEvents.isHiddenFrom(soldier, "enemy", 1, false));
    }
}
