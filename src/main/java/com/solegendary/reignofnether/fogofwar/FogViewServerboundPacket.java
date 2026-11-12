package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

// A spectator (non-RTS player) asks to hide or show fog for themselves only. Independent of /rts-fog.
public class FogViewServerboundPacket {

    boolean hidden;

    public static void setFogHidden(boolean hidden) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null)
            PacketHandler.INSTANCE.sendToServer(new FogViewServerboundPacket(hidden));
    }

    // packet-handler functions
    public FogViewServerboundPacket(boolean hidden) {
        this.hidden = hidden;
    }

    public FogViewServerboundPacket(FriendlyByteBuf buffer) {
        this.hidden = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.hidden);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("FogViewServerboundPacket: Sender was null");
                return;
            }
            String name = player.getName().getString();
            if (PlayerServerEvents.isRTSPlayer(name)) {
                ReignOfNether.LOGGER.warn("FogViewServerboundPacket: Rejected packet from RTS player " + name);
                return;
            }
            FogOfWarServerEvents.setFogHiddenFor(player, hidden);
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
