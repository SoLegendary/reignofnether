package com.solegendary.reignofnether.registrars;

import com.mojang.serialization.Codec;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.particles.BigVibrationParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

public class ParticleRegistrar {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ReignOfNether.MOD_ID);

    // mirrors vanilla ParticleTypes' private generic register(...) helper
    private static <T extends ParticleOptions> RegistryObject<ParticleType<T>> register(
            String name,
            boolean overrideLimiter,
            ParticleOptions.Deserializer<T> deserializer,
            Function<ParticleType<T>, Codec<T>> codecFactory
    ) {
        return PARTICLES.register(name, () -> new ParticleType<T>(overrideLimiter, deserializer) {
            @Override
            public Codec<T> codec() {
                return codecFactory.apply(this);
            }
        });
    }

    public static final RegistryObject<ParticleType<BigVibrationParticleOption>> BIG_VIBRATION =
            register("big_vibration", true, BigVibrationParticleOption.DESERIALIZER, (type) -> BigVibrationParticleOption.CODEC);

    public static final RegistryObject<SimpleParticleType> BIG_ENCHANT =
            PARTICLES.register("big_enchant",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> BIG_SOUL_FLAME =
            PARTICLES.register("big_soul_flame",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> LEVEL_UP =
            PARTICLES.register("level_up",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> FLOATING_CRIT =
            PARTICLES.register("floating_crit",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> FLOATING_HEART =
            PARTICLES.register("floating_heart",
                    () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> FLOATING_SOUL_FIRE =
            PARTICLES.register("floating_soul_fire",
                    () -> new SimpleParticleType(false));

    public static void init(FMLJavaModLoadingContext context) {
        PARTICLES.register(context.getModEventBus());
    }
}
