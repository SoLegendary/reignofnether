package com.solegendary.reignofnether.registrars;

import java.util.function.Function;
import java.util.function.Supplier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import com.solegendary.reignofnether.particles.BigVibrationParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ParticleRegistrar {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, ReignOfNether.MOD_ID);

    public static final Supplier<SimpleParticleType> BIG_ENCHANT =
            PARTICLES.register("big_enchant",
                    () -> new SimpleParticleType(false));

    public static final Supplier<SimpleParticleType> BIG_SOUL_FLAME =
            PARTICLES.register("big_soul_flame",
                    () -> new SimpleParticleType(false));

    public static final Supplier<SimpleParticleType> LEVEL_UP =
            PARTICLES.register("level_up",
                    () -> new SimpleParticleType(false));

    // mirrors the private generic register(...) helper in vanilla ParticleTypes
    private static <T extends ParticleOptions> Supplier<ParticleType<T>> register(
            String name,
            Function<ParticleType<T>, MapCodec<T>> codecFactory,
            Function<ParticleType<T>, StreamCodec<? super RegistryFriendlyByteBuf, T>> streamCodecFactory
    ) {
        return PARTICLES.register(name, () -> new ParticleType<T>(false) {
            @Override
            public MapCodec<T> codec() {
                return codecFactory.apply(this);
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
                return streamCodecFactory.apply(this);
            }
        });
    }

    public static final Supplier<ParticleType<BigVibrationParticleOption>> BIG_VIBRATION =
            register("big_vibration",
                    type -> BigVibrationParticleOption.CODEC,
                    type -> BigVibrationParticleOption.STREAM_CODEC);




    public static final Supplier<SimpleParticleType> FLOATING_CRIT =
            PARTICLES.register("floating_crit",
                    () -> new SimpleParticleType(false));

    public static final Supplier<SimpleParticleType> FLOATING_HEART =
            PARTICLES.register("floating_heart",
                    () -> new SimpleParticleType(false));

    public static final Supplier<SimpleParticleType> MANA =
            PARTICLES.register("mana",
                    () -> new SimpleParticleType(false));

    public static void init(ModContainer context) {
        PARTICLES.register(context.getEventBus());
    }
}
