package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class SurvivalClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<SurvivalClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "survival_clientbound"));

    @Override
    public CustomPacketPayload.Type<SurvivalClientboundPacket> type() {
        return TYPE;
    }

    SurvivalSyncAction action;
    WaveDifficulty difficulty;
    long value;

    public static void enableAndSetDifficulty(WaveDifficulty diff) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new SurvivalClientboundPacket(SurvivalSyncAction.ENABLE_AND_SET_DIFFICULTY, diff, 0, 0L));
    }

    public static void setWaveNumber(long waveNumber) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new SurvivalClientboundPacket(SurvivalSyncAction.SET_WAVE_NUMBER, WaveDifficulty.EASY, waveNumber, 0L));
    }

    public static void setWaveRandomSeed(long seed) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new SurvivalClientboundPacket(SurvivalSyncAction.SET_WAVE_RANDOM_SEED, WaveDifficulty.EASY, seed, 0L));
    }

    public SurvivalClientboundPacket(SurvivalSyncAction action, WaveDifficulty difficulty, long value, long bonusTicks) {
        this.action = action;
        this.difficulty = difficulty;
        this.value = value;
    }

    public SurvivalClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(SurvivalSyncAction.class);
        this.difficulty = buffer.readEnum(WaveDifficulty.class);
        this.value = buffer.readLong();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeEnum(this.difficulty);
        buffer.writeLong(this.value);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (action) {
                            case ENABLE_AND_SET_DIFFICULTY -> SurvivalClientEvents.enable(difficulty);
                            case SET_WAVE_NUMBER -> SurvivalClientEvents.setWaveNumber(value);
                            case SET_WAVE_RANDOM_SEED -> SurvivalClientEvents.setRandomSeed(value);
                        }
                    });
        });
        return;
    }
}
