package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class SurvivalServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<SurvivalServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "survival_serverbound"));

    @Override
    public CustomPacketPayload.Type<SurvivalServerboundPacket> type() {
        return TYPE;
    }

    public WaveDifficulty difficulty;
    public int waveNumber;

    // copies the gamemode to all other clients
    public static void startSurvivalMode(WaveDifficulty mode) {
        PacketHandler.sendToServer(new SurvivalServerboundPacket(mode, 0));
    }

    // copies the gamemode to all other clients
    public static void setWaveNumber(int number) {
        if (number > 0)
            PacketHandler.sendToServer(new SurvivalServerboundPacket(WaveDifficulty.BEGINNER, number));
    }

    public SurvivalServerboundPacket(WaveDifficulty gameMode, int waveNumber) {
        this.difficulty = gameMode;
        this.waveNumber = waveNumber;
    }

    public SurvivalServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.difficulty = buffer.readEnum(WaveDifficulty.class);
        this.waveNumber = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.difficulty);
        buffer.writeInt(this.waveNumber);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (this.waveNumber <= 0) {
                ReignOfNether.LOGGER.info("[Survival] Enabling survival mode with difficulty: {}", difficulty);
                SurvivalServerEvents.enable(difficulty);
            } else {
                ReignOfNether.LOGGER.info("[Survival] Setting wave number to: {}", waveNumber);
                SurvivalServerEvents.setWaveNumber(waveNumber);
            }
        });
        return;
    }
}