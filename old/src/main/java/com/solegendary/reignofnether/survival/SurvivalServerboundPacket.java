package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.registrars.PacketHandler;
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
            if (this.waveNumber <= 0) {
                SurvivalServerEvents.enable(difficulty);
            } else {
                SurvivalServerEvents.setWaveNumber(waveNumber);
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}