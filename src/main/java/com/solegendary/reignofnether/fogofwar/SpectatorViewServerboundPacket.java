package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

// the players a spectator wants to see the fog of (empty = no fog); the server ignores it for non-spectators
public class SpectatorViewServerboundPacket {

    private static final int MAX_VIEWS = 16;

    private final Set<String> names;

    public static void send(Set<String> names) {
        PacketHandler.INSTANCE.sendToServer(new SpectatorViewServerboundPacket(names));
    }

    public SpectatorViewServerboundPacket(Set<String> names) {
        this.names = names;
    }

    public SpectatorViewServerboundPacket(FriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), MAX_VIEWS);
        this.names = new HashSet<>();
        for (int i = 0; i < n; i++)
            this.names.add(buf.readUtf(16));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(names.size());
        for (String name : names)
            buf.writeUtf(name, 16);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null)
                FogOfWarServerEvents.setSpectatorViews(player, names);
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
