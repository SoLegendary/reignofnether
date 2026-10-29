package com.solegendary.reignofnether.gamemode;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class GameModeServerboundPacket {

    public GameMode gameMode;

    // copies the gamemode to all other clients
    public static void setAndLockAllClientGameModes(GameMode mode) {
        PacketHandler.INSTANCE.sendToServer(new GameModeServerboundPacket(mode));
    }

    public GameModeServerboundPacket(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public GameModeServerboundPacket(FriendlyByteBuf buffer) {
        this.gameMode = buffer.readEnum(GameMode.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.gameMode);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;
            // sent by each player right after they start RTS, so they must be an RTS player by now
            if ((this.gameMode != GameMode.CLASSIC && player.level().getGameRules().getRule(GameRuleRegistrar.PVP_MODES_ONLY).get()) ||
                (!PlayerServerEvents.isOpOrHost(player) && !PlayerServerEvents.isRTSPlayer(player.getName().getString()))) {
                ReignOfNether.LOGGER.warn("GameModeServerboundPacket: {} is not allowed to set game mode {}", player.getName().getString(), this.gameMode);
                return;
            }
            ReignOfNether.LOGGER.info("[GameMode] Setting game mode to: {}", this.gameMode);
            GameModeClientboundPacket.setAndLockAllClientGameModes(this.gameMode);
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}