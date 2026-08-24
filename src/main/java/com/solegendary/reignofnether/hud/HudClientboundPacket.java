package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class HudClientboundPacket {

    // ticks < 0 means "use HudClientEvents' default duration"
    private static final int DEFAULT_TICKS = -1;

    private final String msgKey;
    private final int ticks;

    public static void showTempMessageI18n(ServerPlayer player, String msgKey) {
        showTempMessageI18n(player, msgKey, DEFAULT_TICKS);
    }

    public static void showTempMessageI18n(ServerPlayer player, String msgKey, int ticks) {
        if (player == null)
            return;
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new HudClientboundPacket(msgKey, ticks)
        );
    }

    public static void showTempMessageI18n(String playerName, String msgKey) {
        showTempMessageI18n(playerName, msgKey, DEFAULT_TICKS);
    }

    public static void showTempMessageI18n(String playerName, String msgKey, int ticks) {
        if (playerName == null || playerName.isBlank())
            return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        ServerPlayer sp = server.getPlayerList().getPlayerByName(playerName);
        if (sp != null)
            showTempMessageI18n(sp, msgKey, ticks);
    }

    public HudClientboundPacket(String message, int ticks) {
        this.msgKey = message;
        this.ticks = ticks;
    }

    public HudClientboundPacket(FriendlyByteBuf buffer) {
        this.msgKey = buffer.readUtf();
        this.ticks = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.msgKey);
        buffer.writeInt(this.ticks);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (this.ticks < 0)
                            HudClientEvents.showTempMessageI18n(this.msgKey);
                        else
                            HudClientEvents.showTempMessageI18n(this.msgKey, this.ticks);
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}