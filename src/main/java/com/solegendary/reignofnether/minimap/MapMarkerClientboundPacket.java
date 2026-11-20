package com.solegendary.reignofnether.minimap;

import com.solegendary.reignofnether.chat.TeamChatClient;
import com.solegendary.reignofnether.chat.TeamChatServerEvents.Scope;
import com.solegendary.reignofnether.ping.PingType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class MapMarkerClientboundPacket {
    private final int x;
    private final int y;
    private final int z;
    private final String playerName;
    private final PingType type;
    private final Scope scope; // TEAM, or TEAM_SPECTATED for a spectator watching another team

    public MapMarkerClientboundPacket(int x, int y, int z, String playerName, PingType type, Scope scope) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.playerName = playerName;
        this.type = type;
        this.scope = scope;
    }

    public MapMarkerClientboundPacket(FriendlyByteBuf buffer) {
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
        this.playerName = buffer.readUtf();
        this.type = buffer.readEnum(PingType.class);
        this.scope = buffer.readEnum(Scope.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.x);
        buffer.writeInt(this.y);
        buffer.writeInt(this.z);
        buffer.writeUtf(this.playerName);
        buffer.writeEnum(this.type);
        buffer.writeEnum(this.scope);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                MinimapClientEvents.addMapMarker(x, y, z, playerName, type);
                TeamChatClient.show(playerName, scope, type.message());
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
