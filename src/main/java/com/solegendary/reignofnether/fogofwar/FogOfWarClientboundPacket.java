package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.player.PlayerAction;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class FogOfWarClientboundPacket {

    public boolean enable;
    public String playerName;

    public static void setEnabled(boolean enable) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new FogOfWarClientboundPacket(enable, ""));
    }

    public static void revealOrHidePlayer(boolean reveal, String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new FogOfWarClientboundPacket(reveal, playerName));
    }

    public FogOfWarClientboundPacket(boolean enable, String playerName) {
        this.enable = enable;
        this.playerName = playerName;
    }

    public FogOfWarClientboundPacket(FriendlyByteBuf buffer) {
        this.enable = buffer.readBoolean();
        this.playerName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.enable);
        buffer.writeUtf(this.playerName);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    if (playerName.isEmpty())
                        FogOfWarClientEvents.setEnabled(enable);
                    else
                        FogOfWarClientEvents.revealOrHidePlayer(enable, playerName);
                    success.set(true);
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
