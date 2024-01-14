package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistrar {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ReignOfNether.MOD_ID);

    public static final RegistryObject<SoundEvent> UNDER_ATTACK_SOUND =
            SOUND_EVENTS.register("under_attack", () ->
                    new SoundEvent(new ResourceLocation(ReignOfNether.MOD_ID, "under_attack")));

    public static final RegistryObject<SoundEvent> VICTORY_SOUND =
            SOUND_EVENTS.register("victory", () ->
                    new SoundEvent(new ResourceLocation(ReignOfNether.MOD_ID, "victory")));

    public static final RegistryObject<SoundEvent> DEFEAT_SOUND =
            SOUND_EVENTS.register("defeat", () ->
                    new SoundEvent(new ResourceLocation(ReignOfNether.MOD_ID, "defeat")));

    public static void init() {
        SOUND_EVENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
