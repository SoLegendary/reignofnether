package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.player.PlayerRole;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        List<StartPos> seats = List.of(holder, mate);
        assertTrue(join.answeredBy(holder, seats));
        assertFalse(join.answeredBy(mate, seats));
    }

    @Test
    void aSoldierIsAnsweredByAnyoneSeatedOnTheTeam() {
        SeatJoin join = new SeatJoin("asker", PlayerRole.SOLDIER, 1, BlockPos.ZERO);
        StartPos a = seat(0, 1, "a"), b = seat(1, 1, "b"), enemy = seat(2, 2, "enemy");
        List<StartPos> seats = List.of(a, b, enemy);
        assertTrue(join.answeredBy(a, seats));
        assertTrue(join.answeredBy(b, seats));
        assertFalse(join.answeredBy(enemy, seats));
    }

    @Test
    void emptySeatsAndTheAskerCantAnswer() {
        SeatJoin join = new SeatJoin("asker", PlayerRole.SOLDIER, 1, BlockPos.ZERO);
        StartPos empty = seat(0, 1, ""), asker = seat(1, 1, "asker");
        List<StartPos> seats = List.of(empty, asker);
        assertFalse(join.answeredBy(empty, seats));
        assertFalse(join.answeredBy(asker, seats));
    }
}
