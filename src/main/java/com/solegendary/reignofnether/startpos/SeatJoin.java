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

    public SeatJoin(String playerName, PlayerRole role, int colorId, BlockPos seat) {
        this.playerName = playerName;
        this.role = role;
        this.colorId = colorId;
        this.seat = seat;
    }

    public boolean isSoldier() {
        return role == PlayerRole.SOLDIER;
    }
}
