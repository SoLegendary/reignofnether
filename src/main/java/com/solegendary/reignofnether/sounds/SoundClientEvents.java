package com.solegendary.reignofnether.sounds;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
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

    private static final Map<SoundAction, List<Long>> recentSoundTimes = new HashMap<>();
    private static final int SOUND_DEDUPE_WINDOW_TICKS = 5;
    private static final float SOUND_VOLUME_DIMINISH_FACTOR = 0.7f; // each stack multiplies by this
    private static final int SOUND_MAX_STACKS = 10; // volume floors at diminish^maxStacks

    public static final float SOUND_RANGE = 96; // all sounds have this max range

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

        // Periodically prune stale sound deduplication keys
        long currentTick = MC.level != null ? MC.level.getGameTime() : -1;
        if (currentTick > 0 && currentTick % 20 == 0) {
            recentSoundTimes.entrySet().removeIf(e -> {
                e.getValue().removeIf(t -> currentTick - t > SOUND_DEDUPE_WINDOW_TICKS);
                return e.getValue().isEmpty();
            });
        }
    }

    public static void playFactionCalmTheme(Faction faction, String playerName) {
        if (MC.player != null && MC.player.getName().getString().equals(playerName)) {
            SoundClientEvents.playFadeableMusicInstance(new FadeableMusicInstance(faction.sound), 5200);
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
        if (level == null) return;

        long currentTick = level.getGameTime();

        // Get or create the list of recent play times for this action
        List<Long> times = recentSoundTimes.computeIfAbsent(soundAction, k -> new ArrayList<>());

        // Prune entries outside the deduplication window
        times.removeIf(t -> currentTick - t > SOUND_DEDUPE_WINDOW_TICKS);

        // Calculate diminished volume based on how many have already played this window
        int stacks = Math.min(times.size(), SOUND_MAX_STACKS);
        float diminishedVolume = volume * (float) Math.pow(SOUND_VOLUME_DIMINISH_FACTOR, stacks);

        times.add(currentTick);

        Vec3 listenerPos = getOrthoviewSoundPos();

        float dist = (float) Math.sqrt(listenerPos.distanceToSqr(bp.getCenter()));
        if (dist > SOUND_RANGE) return;
        float attenuatedVolume = dist <= (SOUND_RANGE / 4) ? diminishedVolume : diminishedVolume * (1.0f - (dist - (SOUND_RANGE / 4)) / (((SOUND_RANGE * 3) / 4)));

        level.playLocalSound(bp.getX(), bp.getY(), bp.getZ(), soundEvent, SoundSource.NEUTRAL, attenuatedVolume, 1.0F, false);
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

    // calculates the location of where local world sounds should play for an orthoview player based on the selected
    // units and/or buildings that are currently on the screen
    // eg. if the player has selected a unit at pos (4,6) but their player entity is at (34,52) and a sound plays at
    // (10,10), the new sound position should instead be (10,10) - (4,6) + (34,52) = (40,56)
    // if nothing is selected, then default to the centre of the screen
    public static Vec3 getOrthoviewSoundPos() {
        return getOrthoviewSoundPos(null);
    }

    public static Vec3 getOrthoviewSoundPos(@Nullable Vec3 originalPos) {
        Player player = MC.player;
        if (player == null)
            return originalPos;

        ArrayList<Vec3> posList = new ArrayList<>();

        for (LivingEntity entity : UnitClientEvents.getSelectedUnits())
            posList.add(entity.getEyePosition());
        for (BuildingPlacement building : BuildingClientEvents.getSelectedBuildings()) {
            BlockPos bp = BuildingUtils.getCentrePos(building.getBlocks());
            posList.add(new Vec3(bp.getX(), bp.getY(), bp.getZ()));
        }
        // remove any positions that aren't on the screen
        List<Vec3> posListOnScreen = new ArrayList<>();
        for (Vec3 vec3 : posList) {
            if (MinimapClientEvents.isWorldXZinsideMap((int) vec3.x, (int) vec3.z)) {
                posListOnScreen.add(vec3);
            }
        }

        // calculate the average position
        Vec3 newPos = new Vec3(0,0,0);
        if (!posListOnScreen.isEmpty()) {
            for (Vec3 pos : posListOnScreen)
                newPos = newPos.add(pos);
            double m = 1D/posListOnScreen.size();
            newPos = newPos.multiply(m, m, m);
        } else {
            // do a similar kind of calculation to get the pos at the centre of the screen as in CursorClientEvents
            newPos = MiscUtil.getOrthoviewCentreWorldPos(MC);
        }
        // get the position for the sound as though the player was at position newPos
        Vec3 diffOriginalToNew = originalPos != null ?
                originalPos.add(newPos.multiply(-1,-1,-1)) : new Vec3(0,0,0);
        return new Vec3(player.getX(), player.getY(), player.getZ()).add(diffOriginalToNew);
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
        SOUND_MAP.put(SoundAction.DAWN_ROOSTER, SoundRegistrar.DAWN_ROOSTER.get());
        SOUND_MAP.put(SoundAction.DUSK_WOLF, SoundRegistrar.DUSK_WOLF.get());
        SOUND_MAP.put(SoundAction.BELL, SoundEvents.BELL_BLOCK);
        SOUND_MAP.put(SoundAction.BEACON_DEACTIVATE, SoundEvents.BEACON_DEACTIVATE);
        SOUND_MAP.put(SoundAction.BEACON_ACTIVATE, SoundEvents.BEACON_ACTIVATE);
        SOUND_MAP.put(SoundAction.BEACON_AMBIENT, SoundEvents.BEACON_AMBIENT);
        SOUND_MAP.put(SoundAction.ENCHANT, SoundEvents.ENCHANTMENT_TABLE_USE);
        SOUND_MAP.put(SoundAction.FORGE_ARMOUR, SoundEvents.SMITHING_TABLE_USE);
        SOUND_MAP.put(SoundAction.LEVEL_UP, SoundEvents.PLAYER_LEVELUP);
        SOUND_MAP.put(SoundAction.BLOODLUST, SoundRegistrar.BLOODLUST.get());
        SOUND_MAP.put(SoundAction.BLOODLUST_2, SoundRegistrar.BLOODLUST_2.get());
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
        SOUND_MAP.put(SoundAction.WRAITH_FEAR, SoundRegistrar.WRAITH_FEAR.get());
        SOUND_MAP.put(SoundAction.WRAITH_POSSESS_CHANNEL, SoundRegistrar.WRAITH_POSSESS_CHANNEL.get());
        SOUND_MAP.put(SoundAction.WRAITH_POSSESS_PARTIAL, SoundRegistrar.WRAITH_POSSESS_PARTIAL.get());
        SOUND_MAP.put(SoundAction.WRAITH_POSSESS_FULL, SoundRegistrar.WRAITH_POSSESS_FULL.get());
        SOUND_MAP.put(SoundAction.WINDCALLER_LIFT, SoundRegistrar.WINDCALLER_LIFT.get());
        SOUND_MAP.put(SoundAction.WINDCALLER_YELL, SoundRegistrar.WINDCALLER_YELL.get());
        SOUND_MAP.put(SoundAction.WINDCALLER_WIND_ATTACK, SoundRegistrar.WINDCALLER_WIND_ATTACK.get());
    }
}
