package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.matchstart.MatchEndClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

// Sent to all clients once a match ends, carrying the final scoreboard so the
// end-of-match stats screen (MatchEndScreen) can be rendered. Scores otherwise
// only exist server-side, so this is the only way the client learns them.
public class MatchStatsClientboundPacket {

    // one results-table row per player that took part in the match
    public static class MatchStatRow {
        public final String name;
        public final Faction faction;
        public final boolean winner;
        public final int teamId; // startPosColorId - players sharing it are on the same team
        public final int[] scores; // ordered as RTSPlayerScoresEnum.values()

        public MatchStatRow(String name, Faction faction, boolean winner, int teamId, int[] scores) {
            this.name = name;
            this.faction = faction;
            this.winner = winner;
            this.teamId = teamId;
            this.scores = scores;
        }
    }

    private final long gameDurationTicks;
    private final List<MatchStatRow> rows;

    public static void broadcast(long gameDurationTicks, List<MatchStatRow> rows) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new MatchStatsClientboundPacket(gameDurationTicks, rows));
    }

    public MatchStatsClientboundPacket(long gameDurationTicks, List<MatchStatRow> rows) {
        this.gameDurationTicks = gameDurationTicks;
        this.rows = rows;
    }

    public MatchStatsClientboundPacket(FriendlyByteBuf buffer) {
        this.gameDurationTicks = buffer.readLong();
        int n = buffer.readInt();
        this.rows = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String name = buffer.readUtf();
            Faction faction = buffer.readEnum(Faction.class);
            boolean winner = buffer.readBoolean();
            int teamId = buffer.readVarInt();
            int[] scores = buffer.readVarIntArray();
            this.rows.add(new MatchStatRow(name, faction, winner, teamId, scores));
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(gameDurationTicks);
        buffer.writeInt(rows.size());
        for (MatchStatRow row : rows) {
            buffer.writeUtf(row.name);
            buffer.writeEnum(row.faction);
            buffer.writeBoolean(row.winner);
            buffer.writeVarInt(row.teamId);
            buffer.writeVarIntArray(row.scores);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        MatchEndClientEvents.receive(gameDurationTicks, rows);
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
