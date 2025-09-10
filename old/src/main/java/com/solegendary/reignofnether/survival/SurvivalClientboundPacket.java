package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SurvivalClientboundPacket {

    SurvivalSyncAction action;
    WaveDifficulty difficulty;
    long value;

    public static void enableAndSetDifficulty(WaveDifficulty diff) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SurvivalClientboundPacket(SurvivalSyncAction.ENABLE_AND_SET_DIFFICULTY, diff, 0, 0L));
    }

    public static void setWaveNumber(long waveNumber) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SurvivalClientboundPacket(SurvivalSyncAction.SET_WAVE_NUMBER, WaveDifficulty.EASY, waveNumber, 0L));
    }

    public static void setWaveRandomSeed(long seed) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SurvivalClientboundPacket(SurvivalSyncAction.SET_WAVE_RANDOM_SEED, WaveDifficulty.EASY, seed, 0L));
    }

    public SurvivalClientboundPacket(SurvivalSyncAction action, WaveDifficulty difficulty, long value, long bonusTicks) {
        this.action = action;
        this.difficulty = difficulty;
        this.value = value;
    }

    public SurvivalClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(SurvivalSyncAction.class);
        this.difficulty = buffer.readEnum(WaveDifficulty.class);
        this.value = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeEnum(this.difficulty);
        buffer.writeLong(this.value);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (action) {
                            case ENABLE_AND_SET_DIFFICULTY -> SurvivalClientEvents.enable(difficulty);
                            case SET_WAVE_NUMBER -> SurvivalClientEvents.setWaveNumber(value);
                            case SET_WAVE_RANDOM_SEED -> SurvivalClientEvents.setRandomSeed(value);
                        }
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
