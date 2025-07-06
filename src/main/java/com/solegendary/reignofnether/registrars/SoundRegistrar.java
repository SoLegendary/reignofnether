package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistrar {

    // Note for some reason mp3 files from the AOE2 resources folder do not work when converted to .ogg
    // Instead try rerecording them on OBS and converting the .mkv to .ogg

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ReignOfNether.MOD_ID);

    public static final RegistryObject<SoundEvent> UNDER_ATTACK =
            SOUND_EVENTS.register("under_attack", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(ReignOfNether.MOD_ID, "under_attack")));

    public static final RegistryObject<SoundEvent> VICTORY =
            SOUND_EVENTS.register("victory", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(ReignOfNether.MOD_ID, "victory")));

    public static final RegistryObject<SoundEvent> DEFEAT =
            SOUND_EVENTS.register("defeat", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(ReignOfNether.MOD_ID, "defeat")));

    public static final RegistryObject<SoundEvent> ALLY =
            SOUND_EVENTS.register("ally", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(ReignOfNether.MOD_ID, "ally")));

    public static final RegistryObject<SoundEvent> ENEMY =
            SOUND_EVENTS.register("enemy", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(ReignOfNether.MOD_ID, "enemy")));

    public static final RegistryObject<SoundEvent> CHAT =
            SOUND_EVENTS.register("chat", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(ReignOfNether.MOD_ID, "chat")));

    public static final RegistryObject<SoundEvent> MAIN_MENU =
            SOUND_EVENTS.register("main_menu", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(ReignOfNether.MOD_ID, "main_menu")));

    public static final RegistryObject<SoundEvent> BLOOD_MOON =
            SOUND_EVENTS.register("blood_moon", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(ReignOfNether.MOD_ID, "blood_moon")));

    public static final RegistryObject<SoundEvent> BLOODLUST =
            SOUND_EVENTS.register("bloodlust", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(ReignOfNether.MOD_ID, "bloodlust")));

    public static final RegistryObject<SoundEvent> HEROISM =
            SOUND_EVENTS.register("heroism", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(ReignOfNether.MOD_ID, "heroism")));

    public static void init() {
        SOUND_EVENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
