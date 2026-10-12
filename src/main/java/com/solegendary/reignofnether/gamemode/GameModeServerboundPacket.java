package com.solegendary.reignofnether.gamemode;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class GameModeServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<GameModeServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "game_mode_serverbound"));

    @Override
    public CustomPacketPayload.Type<GameModeServerboundPacket> type() {
        return TYPE;
    }

    public GameMode gameMode;

    // copies the gamemode to all other clients
    public static void setAndLockAllClientGameModes(GameMode mode) {
        PacketHandler.sendToServer(new GameModeServerboundPacket(mode));
    }

    public GameModeServerboundPacket(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public GameModeServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.gameMode = buffer.readEnum(GameMode.class);
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.gameMode);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ReignOfNether.LOGGER.info("[GameMode] Setting game mode to: {}", this.gameMode);
            GameModeClientboundPacket.setAndLockAllClientGameModes(this.gameMode);
        });
        return;
    }
}