package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.function.Supplier;

public class HudClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<HudClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "hud_clientbound"));

    @Override
    public CustomPacketPayload.Type<HudClientboundPacket> type() {
        return TYPE;
    }

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
        PacketHandler.send(PacketHandler.toPlayer(() -> player),
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

    public HudClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.msgKey = buffer.readUtf();
        this.ticks = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.msgKey);
        buffer.writeInt(this.ticks);
    }

    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (this.ticks < 0)
                            HudClientEvents.showTempMessageI18n(this.msgKey);
                        else
                            HudClientEvents.showTempMessageI18n(this.msgKey, this.ticks);
                    });
        });
        return;
    }
}