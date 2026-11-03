package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SurvivalServerboundPacket {

    public WaveDifficulty difficulty;
    public int waveNumber;

    // copies the gamemode to all other clients
    public static void startSurvivalMode(WaveDifficulty mode) {
        PacketHandler.INSTANCE.sendToServer(new SurvivalServerboundPacket(mode, 0));
    }

    // copies the gamemode to all other clients
    public static void setWaveNumber(int number) {
        if (number > 0)
            PacketHandler.INSTANCE.sendToServer(new SurvivalServerboundPacket(WaveDifficulty.BEGINNER, number));
    }

    public SurvivalServerboundPacket(WaveDifficulty gameMode, int waveNumber) {
        this.difficulty = gameMode;
        this.waveNumber = waveNumber;
    }

    public SurvivalServerboundPacket(FriendlyByteBuf buffer) {
        this.difficulty = buffer.readEnum(WaveDifficulty.class);
        this.waveNumber = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.difficulty);
        buffer.writeInt(this.waveNumber);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;
            String playerName = player.getName().getString();
            boolean isOp = PlayerServerEvents.isOpOrHost(player);
            if (this.waveNumber <= 0) {
                // like the client, survival can only be picked by the first player to start and never with pvpModesOnly
                if (player.level().getGameRules().getRule(GameRuleRegistrar.PVP_MODES_ONLY).get() ||
                    (!isOp && !(PlayerServerEvents.isRTSPlayer(playerName) && PlayerServerEvents.rtsPlayers.size() == 1))) {
                    ReignOfNether.LOGGER.warn("SurvivalServerboundPacket: {} is not allowed to enable survival", playerName);
                    return;
                }
                ReignOfNether.LOGGER.info("[Survival] Enabling survival mode with difficulty: {}", difficulty);
                SurvivalServerEvents.enable(difficulty);
            } else {
                if (!isOp && !ResearchServerEvents.playerHasCheat(playerName, "thereisnospoon")) {
                    ReignOfNether.LOGGER.warn("SurvivalServerboundPacket: {} is not allowed to set the wave number", playerName);
                    return;
                }
                ReignOfNether.LOGGER.info("[Survival] Setting wave number to: {}", waveNumber);
                SurvivalServerEvents.setWaveNumber(waveNumber);
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}