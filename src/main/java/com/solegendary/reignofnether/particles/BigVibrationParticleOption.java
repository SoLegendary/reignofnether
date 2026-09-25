//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;

import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.phys.Vec3;

public class BigVibrationParticleOption implements ParticleOptions {
    public static final Codec<BigVibrationParticleOption> CODEC = RecordCodecBuilder.create((p_235978_) -> {
        return p_235978_.group(PositionSource.CODEC.fieldOf("destination").forGetter((p_235982_) -> {
            return p_235982_.destination;
        }), Codec.INT.fieldOf("arrival_in_ticks").forGetter((p_235980_) -> {
            return p_235980_.arrivalInTicks;
        })).apply(p_235978_, BigVibrationParticleOption::new);
    });
    public static final ParticleOptions.Deserializer<BigVibrationParticleOption> DESERIALIZER = new ParticleOptions.Deserializer<BigVibrationParticleOption>() {
        public BigVibrationParticleOption fromCommand(ParticleType<BigVibrationParticleOption> p_175859_, StringReader p_175860_) throws CommandSyntaxException {
            p_175860_.expect(' ');
            float $$2 = (float)p_175860_.readDouble();
            p_175860_.expect(' ');
            float $$3 = (float)p_175860_.readDouble();
            p_175860_.expect(' ');
            float $$4 = (float)p_175860_.readDouble();
            p_175860_.expect(' ');
            int $$5 = p_175860_.readInt();
            BlockPos $$6 = BlockPos.containing((double)$$2, (double)$$3, (double)$$4);
            return new BigVibrationParticleOption(new BlockPositionSource($$6), $$5);
        }

        public BigVibrationParticleOption fromNetwork(ParticleType<BigVibrationParticleOption> p_175862_, FriendlyByteBuf p_175863_) {
            PositionSource $$2 = PositionSourceType.fromNetwork(p_175863_);
            int $$3 = p_175863_.readVarInt();
            return new BigVibrationParticleOption($$2, $$3);
        }
    };
    private final PositionSource destination;
    private final int arrivalInTicks;

    public BigVibrationParticleOption(PositionSource p_235975_, int p_235976_) {
        this.destination = p_235975_;
        this.arrivalInTicks = p_235976_;
    }

    public void writeToNetwork(FriendlyByteBuf pBuffer) {
        PositionSourceType.toNetwork(this.destination, pBuffer);
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
