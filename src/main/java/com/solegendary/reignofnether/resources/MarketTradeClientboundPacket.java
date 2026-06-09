package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class MarketTradeClientboundPacket {

    private final int[] rates;

    public static void sendRates(ServerPlayer player, int[] rates) {
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new MarketTradeClientboundPacket(rates));
    }

    public MarketTradeClientboundPacket(int[] rates) {
        this.rates = rates;
    }

    public MarketTradeClientboundPacket(FriendlyByteBuf buffer) {
        this.rates = buffer.readVarIntArray();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarIntArray(this.rates);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || rates == null || rates.length != 6) {
                success.set(false);
                return;
            }
            RTSPlayer self = PlayerClientEvents.getRTSPlayer(mc.player.getName().getString());
            if (self != null) {
                self.tradeRates = rates;
            }
            success.set(true);
        }));
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
