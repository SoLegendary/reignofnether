//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;

import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.phys.Vec3;
import com.solegendary.reignofnether.particles.BigVibrationParticleOption;

public class BigVibrationParticleOption implements ParticleOptions {
    public static final MapCodec<BigVibrationParticleOption> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                            PositionSource.CODEC.fieldOf("destination")
                                    .forGetter(BigVibrationParticleOption::getDestination),
                            Codec.INT.fieldOf("arrival_in_ticks")
                                    .forGetter(BigVibrationParticleOption::getArrivalInTicks))
                    .apply(instance, BigVibrationParticleOption::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BigVibrationParticleOption> STREAM_CODEC =
            StreamCodec.composite(
                    PositionSource.STREAM_CODEC,
                    BigVibrationParticleOption::getDestination,
                    ByteBufCodecs.VAR_INT,
                    BigVibrationParticleOption::getArrivalInTicks,
                    BigVibrationParticleOption::new
            );
    private final PositionSource destination;
    private final int arrivalInTicks;

    public BigVibrationParticleOption(PositionSource p_235975_, int p_235976_) {
        this.destination = p_235975_;
        this.arrivalInTicks = p_235976_;
    }

    public void writeToNetwork(FriendlyByteBuf pBuffer) {
        PositionSource.STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) pBuffer, this.destination);
        pBuffer.writeVarInt(this.arrivalInTicks);
    }

    public String writeToString() {
        Vec3 $$0 = (Vec3)this.destination.getPosition((Level)null).get();
        double $$1 = $$0.x();
        double $$2 = $$0.y();
        double $$3 = $$0.z();
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %d", BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()), $$1, $$2, $$3, this.arrivalInTicks);
    }

    public ParticleType<BigVibrationParticleOption> getType() {
        return ParticleRegistrar.BIG_VIBRATION.get();
    }

    public PositionSource getDestination() {
        return this.destination;
    }

    public int getArrivalInTicks() {
        return this.arrivalInTicks;
    }
}
