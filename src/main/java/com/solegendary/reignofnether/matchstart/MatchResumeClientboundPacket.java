package com.solegendary.reignofnether.matchstart;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** Opens the paused resume screen of a saved game (open = true), or closes it for everyone once resumed. */
public class MatchResumeClientboundPacket {

    /** baseX/baseZ: their capitol (or first building), NO_BASE when they have none or it's hidden by fog. */
    public record Row(String name, String faction, int colorId, boolean bot, String allies, boolean online, int baseX, int baseZ) { }

    public static final int NO_BASE = Integer.MIN_VALUE;

    private final boolean open;
    private final String mapName;
    private final long gameTicks;
    private final boolean host;
    private final List<Row> rows;
    private final boolean fog;

    public MatchResumeClientboundPacket(boolean open, String mapName, long gameTicks, boolean host, List<Row> rows, boolean fog) {
        this.open = open;
        this.fog = fog;
        this.mapName = mapName;
        this.gameTicks = gameTicks;
        this.host = host;
        this.rows = rows;
    }

    public MatchResumeClientboundPacket(FriendlyByteBuf buffer) {
        this.open = buffer.readBoolean();
        this.mapName = buffer.readUtf();
        this.gameTicks = buffer.readLong();
        this.host = buffer.readBoolean();
        this.rows = buffer.readList(b -> new Row(b.readUtf(), b.readUtf(), b.readInt(), b.readBoolean(), b.readUtf(),
                b.readBoolean(), b.readInt(), b.readInt()));
        this.fog = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(open);
        buffer.writeUtf(mapName);
        buffer.writeLong(gameTicks);
        buffer.writeBoolean(host);
        buffer.writeCollection(rows, (b, r) -> {
            b.writeUtf(r.name());
            b.writeUtf(r.faction());
            b.writeInt(r.colorId());
            b.writeBoolean(r.bot());
            b.writeUtf(r.allies());
            b.writeBoolean(r.online());
            b.writeInt(r.baseX());
            b.writeInt(r.baseZ());
        });
        buffer.writeBoolean(fog);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                MatchResumeScreen.show(open, mapName, gameTicks, host, rows, fog)));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
