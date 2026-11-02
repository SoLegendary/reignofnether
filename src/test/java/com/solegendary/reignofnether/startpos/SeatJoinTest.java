package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.player.PlayerRole;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeatJoinTest {

    private static StartPos seat(int x, int team, String name) {
        StartPos sp = new StartPos(new BlockPos(x, 0, 0), team);
        sp.playerName = name;
        return sp;
    }

    @Test
    void aCoCommanderIsAnsweredByTheirSeatOnly() {
        StartPos holder = seat(0, 1, "holder"), mate = seat(1, 1, "mate");
        SeatJoin join = new SeatJoin("asker", PlayerRole.COMMANDER, 1, holder.pos);
        assertTrue(join.answeredBy(holder));
        assertFalse(join.answeredBy(mate));
    }

    @Test
    void aSoldierIsAnsweredByAnyoneSeatedOnTheTeam() {
        SeatJoin join = new SeatJoin("asker", PlayerRole.SOLDIER, 1, BlockPos.ZERO);
        assertTrue(join.answeredBy(seat(0, 1, "a")));
        assertTrue(join.answeredBy(seat(1, 1, "b")));
        assertFalse(join.answeredBy(seat(2, 2, "enemy")));
    }

    @Test
    void emptySeatsAndTheAskerCantAnswer() {
        SeatJoin join = new SeatJoin("asker", PlayerRole.SOLDIER, 1, BlockPos.ZERO);
        assertFalse(join.answeredBy(seat(0, 1, "")));
        assertFalse(join.answeredBy(seat(1, 1, "asker")));
    }
}
