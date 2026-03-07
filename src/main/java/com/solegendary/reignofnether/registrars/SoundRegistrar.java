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
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "under_attack")));

    public static final RegistryObject<SoundEvent> VICTORY =
            SOUND_EVENTS.register("victory", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "victory")));

    public static final RegistryObject<SoundEvent> DEFEAT =
            SOUND_EVENTS.register("defeat", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "defeat")));

    public static final RegistryObject<SoundEvent> ALLY =
            SOUND_EVENTS.register("ally", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "ally")));

    public static final RegistryObject<SoundEvent> ENEMY =
            SOUND_EVENTS.register("enemy", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "enemy")));

    public static final RegistryObject<SoundEvent> CHAT =
            SOUND_EVENTS.register("chat", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "chat")));

    public static final RegistryObject<SoundEvent> MAIN_MENU =
            SOUND_EVENTS.register("main_menu", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "main_menu")));

    public static final RegistryObject<SoundEvent> BLOODLUST =
            SOUND_EVENTS.register("bloodlust", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "bloodlust")));

    public static final RegistryObject<SoundEvent> HEROISM =
            SOUND_EVENTS.register("heroism", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "heroism")));

    public static final RegistryObject<SoundEvent> BLOOD_MOON_SONG =
            SOUND_EVENTS.register("blood_moon", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "blood_moon")));

    public static final RegistryObject<SoundEvent> VILLAGER_CALM_THEME_SONG =
            SOUND_EVENTS.register("sharpened_grassblades_calm", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "sharpened_grassblades_calm")));

    public static final RegistryObject<SoundEvent> MONSTER_CALM_THEME_SONG =
            SOUND_EVENTS.register("life_scourge_calm", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "life_scourge_calm")));

    public static final RegistryObject<SoundEvent> PIGLIN_CALM_THEME_SONG =
            SOUND_EVENTS.register("soul_resonance_calm", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "soul_resonance_calm")));

    public static final RegistryObject<SoundEvent> WRETCHED_WRAITH_AMBIENT =
            SOUND_EVENTS.register("wretchedwraith_ambient", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretchedwraith_ambient")));

    public static final RegistryObject<SoundEvent> WRETCHED_WRAITH_HURT =
            SOUND_EVENTS.register("wretchedwraith_hurt", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretchedwraith_hurt")));

    public static final RegistryObject<SoundEvent> WRETCHED_WRAITH_DEATH =
            SOUND_EVENTS.register("wretchedwraith_death", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretchedwraith_death")));

    public static final RegistryObject<SoundEvent> WRETCHED_WRAITH_ATTACK_QUIET =
            SOUND_EVENTS.register("wretchedwraith_attack_quiet", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretchedwraith_attack_quiet")));

    public static final RegistryObject<SoundEvent> WRETCHED_WRAITH_ATTACK_LOUD =
            SOUND_EVENTS.register("wretchedwraith_attack_loud", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretchedwraith_attack_loud")));

    public static final RegistryObject<SoundEvent> WRETCHED_WRAITH_TELEPORT_START =
            SOUND_EVENTS.register("wretchedwraith_teleport_start", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretchedwraith_teleport_start")));

    public static final RegistryObject<SoundEvent> WRETCHED_WRAITH_TELEPORT_END =
            SOUND_EVENTS.register("wretchedwraith_teleport_end", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretchedwraith_teleport_end")));

    public static final RegistryObject<SoundEvent> WRETCHED_WRAITH_BLIZZARD =
            SOUND_EVENTS.register("wretchedwraith_blizzard", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wretchedwraith_blizzard")));

    public static final RegistryObject<SoundEvent> WILDFIRE_MOLTEN_BOMB =
            SOUND_EVENTS.register("wildfire_molten_bomb", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire_molten_bomb")));

    public static final RegistryObject<SoundEvent> WILDFIRE_SCORCHING_GAZE_START =
            SOUND_EVENTS.register("wildfire_scorching_gaze_start", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire_scorching_gaze_start")));

    public static final RegistryObject<SoundEvent> WILDFIRE_SCORCHING_GAZE_END =
            SOUND_EVENTS.register("wildfire_scorching_gaze_end", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire_scorching_gaze_end")));

    public static final RegistryObject<SoundEvent> WILDFIRE_HURT =
            SOUND_EVENTS.register("wildfire_hurt", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire_hurt")));

    public static final RegistryObject<SoundEvent> WILDFIRE_DEATH =
            SOUND_EVENTS.register("wildfire_death", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire_death")));

    public static final RegistryObject<SoundEvent> WILDFIRE_AMBIENT =
            SOUND_EVENTS.register("wildfire_ambient", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire_ambient")));

    public static final RegistryObject<SoundEvent> WILDFIRE_SOULS_AFLAME =
            SOUND_EVENTS.register("wildfire_souls_aflame", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "wildfire_souls_aflame")));

    public static final RegistryObject<SoundEvent> PIGLIN_MERCHANT_LOOT_EXPLOSION =
            SOUND_EVENTS.register("piglin_merchant_loot_explosion", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "piglin_merchant_loot_explosion")));

    public static void init(FMLJavaModLoadingContext context) {
        SOUND_EVENTS.register(context.getModEventBus());
    }
}
