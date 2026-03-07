package com.solegendary.reignofnether.sounds;

import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SoundClientEvents {

    // mute the next sound played at each pos in ClientLevelMixin, then remove it from the list
    public static ArrayList<BlockPos> mutedBps = new ArrayList<>();

    public static FadeableMusicInstance customSong = null;

    private static final ArrayList<FadeableSoundInstance> activeFadeableSounds = new ArrayList<>();

    public static int songTicksLeft = 0;

    private static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) {
            return;
        }
        if (songTicksLeft > 0) {
            songTicksLeft -= 1;
            if (customSong != null && songTicksLeft <= 0)
                stopFadeableMusicInstance();
        }
    }

    public static void playFactionCalmTheme(Faction faction, String playerName) {
        if (MC.player != null && MC.player.getName().getString().equals(playerName)) {
            switch (faction) {
                case VILLAGERS ->
                        SoundClientEvents.playFadeableMusicInstance(new FadeableMusicInstance(SoundRegistrar.VILLAGER_CALM_THEME_SONG.get()), 5200);
                case MONSTERS ->
                        SoundClientEvents.playFadeableMusicInstance(new FadeableMusicInstance(SoundRegistrar.MONSTER_CALM_THEME_SONG.get()), 5200);
                case PIGLINS ->
                        SoundClientEvents.playFadeableMusicInstance(new FadeableMusicInstance(SoundRegistrar.PIGLIN_CALM_THEME_SONG.get()), 5200);
            }
        }
    }

    public static void playFadeableMusicInstance(FadeableMusicInstance instance) {
        playFadeableMusicInstance(instance, 0);
    }

    public static void playFadeableMusicInstance(FadeableMusicInstance instance, int tickLength) {
        songTicksLeft = tickLength;
        MC.getMusicManager().stopPlaying();
        customSong = instance;
        MC.getSoundManager().play(instance);
    }

    public static void stopFadeableMusicInstance() {
        if (customSong != null) {
            songTicksLeft = 0;
            customSong.startFadeOut();
            customSong = null;
        }
    }

    public static void stopSound(int id) {
        activeFadeableSounds.removeIf(s -> {
            if (s.id == id) {
                s.fadeOut();
                return true;
            }
            return false;
        });
    }

    public static void playSoundAtPos(SoundAction soundAction, BlockPos bp) {
        playSoundAtPos(soundAction, bp, 1.0f);
    }

    public static void playSoundAtPos(SoundAction soundAction, BlockPos bp, float volume) {
        SoundEvent soundEvent = SOUND_MAP.get(soundAction);
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            level.playLocalSound(bp.getX(), bp.getY(), bp.getZ(), soundEvent, SoundSource.NEUTRAL, volume, 1.0F, false);
        }
    }

    public static void playFadeableSoundAtPos(SoundAction soundAction, BlockPos bp, float volume, int id, int tickDuration) {
        SoundEvent soundEvent = SOUND_MAP.get(soundAction);
        FadeableSoundInstance fsi = new FadeableSoundInstance(soundEvent, bp, id,true, volume, tickDuration);
        MC.getSoundManager().play(fsi);
        activeFadeableSounds.add(fsi);
    }

    public static void playSoundForLocalPlayer(SoundAction soundAction) {
        playSoundForLocalPlayer(soundAction, 1.0f);
    }

    public static void playSoundForLocalPlayer(SoundAction soundAction, float volume) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            MC.player.playSound(SOUND_MAP.get(soundAction), volume, 1.0f);
        }
    }

    public static void playSoundIfPlayer(SoundAction soundAction, String playerName) {
        playSoundIfPlayer(soundAction, playerName, 1.0f);
    }

    public static void playSoundIfPlayer(SoundAction soundAction, String playerName, float volume) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null && MC.player.getName().getString().equals(playerName)) {
            MC.player.playSound(SOUND_MAP.get(soundAction), volume, 1.0f);
        }
    }

    // sounds which shouldn't follow the ClientLevelMixin rules of being changed to the location of what is selected
    // and is always audible while in orthoview mode
    public static List<SoundEvent> STATIC_SOUNDS = List.of(
            SoundEvents.AMBIENT_CAVE.get(),
            SoundRegistrar.ALLY.get(),
            SoundRegistrar.CHAT.get(),
            SoundRegistrar.ENEMY.get()
    );

    private static final Map<SoundAction, SoundEvent> SOUND_MAP = new HashMap<>();

    static {
        SOUND_MAP.put(SoundAction.USE_PORTAL, SoundEvents.ENDERMAN_TELEPORT);
        SOUND_MAP.put(SoundAction.RANDOM_CAVE_AMBIENCE, SoundEvents.AMBIENT_CAVE.get());
        SOUND_MAP.put(SoundAction.ALLY, SoundRegistrar.ALLY.get());
        SOUND_MAP.put(SoundAction.CHAT, SoundRegistrar.CHAT.get());
        SOUND_MAP.put(SoundAction.ENEMY, SoundRegistrar.ENEMY.get());
        SOUND_MAP.put(SoundAction.BELL, SoundEvents.BELL_BLOCK);
        SOUND_MAP.put(SoundAction.BEACON_DEACTIVATE, SoundEvents.BEACON_DEACTIVATE);
        SOUND_MAP.put(SoundAction.BEACON_ACTIVATE, SoundEvents.BEACON_ACTIVATE);
        SOUND_MAP.put(SoundAction.BEACON_AMBIENT, SoundEvents.BEACON_AMBIENT);
        SOUND_MAP.put(SoundAction.ENCHANT, SoundEvents.ENCHANTMENT_TABLE_USE);
        SOUND_MAP.put(SoundAction.FORGE_ARMOUR, SoundEvents.SMITHING_TABLE_USE);
        SOUND_MAP.put(SoundAction.LEVEL_UP, SoundEvents.PLAYER_LEVELUP);
        SOUND_MAP.put(SoundAction.BLOODLUST, SoundRegistrar.BLOODLUST.get());
        SOUND_MAP.put(SoundAction.HEROISM, SoundRegistrar.HEROISM.get());
        SOUND_MAP.put(SoundAction.WRETCHED_WRAITH_ATTACK_QUIET, SoundRegistrar.WRETCHED_WRAITH_ATTACK_QUIET.get());
        SOUND_MAP.put(SoundAction.WRETCHED_WRAITH_ATTACK_LOUD, SoundRegistrar.WRETCHED_WRAITH_ATTACK_LOUD.get());
        SOUND_MAP.put(SoundAction.WRETCHED_WRAITH_TELEPORT_START, SoundRegistrar.WRETCHED_WRAITH_TELEPORT_START.get());
        SOUND_MAP.put(SoundAction.WRETCHED_WRAITH_TELEPORT_END, SoundRegistrar.WRETCHED_WRAITH_TELEPORT_END.get());
        SOUND_MAP.put(SoundAction.WRETCHED_WRAITH_BLIZZARD, SoundRegistrar.WRETCHED_WRAITH_BLIZZARD.get());
        SOUND_MAP.put(SoundAction.WILDFIRE_MOLTEN_BOMB, SoundRegistrar.WILDFIRE_MOLTEN_BOMB.get());
        SOUND_MAP.put(SoundAction.WILDFIRE_SCORCHING_GAZE_START, SoundRegistrar.WILDFIRE_SCORCHING_GAZE_START.get());
        SOUND_MAP.put(SoundAction.WILDFIRE_SCORCHING_GAZE_END, SoundRegistrar.WILDFIRE_SCORCHING_GAZE_END.get());
        SOUND_MAP.put(SoundAction.WILDFIRE_SOULS_AFLAME, SoundRegistrar.WILDFIRE_SOULS_AFLAME.get());
        SOUND_MAP.put(SoundAction.PIGLIN_MERCHANT_LOOT_EXPLOSION, SoundRegistrar.PIGLIN_MERCHANT_LOOT_EXPLOSION.get());
    }
}
