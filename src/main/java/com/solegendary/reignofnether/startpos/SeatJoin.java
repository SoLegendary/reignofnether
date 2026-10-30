package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.player.PlayerRole;
import net.minecraft.core.BlockPos;

// a lobby player who plays without a seat of their own: a soldier on a team, or a co-commander sharing a seat
public class SeatJoin {
    public final String playerName;
    public final PlayerRole role; // SOLDIER or COMMANDER (co-commander)
    public final int colorId;     // the team
    public final BlockPos seat;   // co-commander: the shared seat; soldier: BlockPos.ZERO
    public boolean ready = false;
    public boolean pending = false; // asked to join, waiting on an answer in the roster; can't ready and doesn't hold up the start

    public SeatJoin(String playerName, PlayerRole role, int colorId, BlockPos seat) {
        this.playerName = playerName;
        this.role = role;
        this.colorId = colorId;
        this.seat = seat;
    }

    public boolean isSoldier() {
        return role == PlayerRole.SOLDIER;
    }

    // whoever sits here can answer this request: the shared seat's holder, or for a soldier anyone seated on the team
    public boolean answeredBy(StartPos sp) {
        return sp.enabled && !sp.playerName.isBlank() && !sp.isBot() && !sp.playerName.equals(playerName) &&
                (isSoldier() ? sp.colorId == colorId : sp.pos.equals(seat));
    }
}
