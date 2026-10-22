package com.solegendary.reignofnether.time;

import com.solegendary.reignofnether.debug.RtsDebugClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.sounds.FadeableMusicInstance;
import com.solegendary.reignofnether.sounds.SoundClientEvents;
import com.solegendary.reignofnether.survival.SurvivalClientEvents;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.solegendary.reignofnether.time.TimeUtils.*;

/** Client copy of the day/night time and blood moon state. The HUD clock panel draws it. */
public class TimeClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    // setting this value causes the time of day to smoothly move towards it regardless of the server time
    public static long targetClientTime = 0;
    // actual time on the server
    public static long serverNormDayTime = 0;
    public static long serverGameTime = 0;
    public static double ticksSinceLastUpdate = 0; // for counting partial ticks since serverGameTime is only updated once per second

    // clicking the clock keeps its tooltip open
    public static boolean showClockTooltip = false;

    private static int bloodMoonTicksLeft = 0;
    private static BlockPos bloodMoonPos = null;

    public static void resetBloodMoon() {
        bloodMoonTicksLeft = 0;
        bloodMoonPos = null;
    }

    public static boolean isBloodMoonActive() {
        return bloodMoonTicksLeft > 0;
    }

    public static int getBloodMoonTicksLeft() {
        return bloodMoonTicksLeft;
    }

    public static void centreOnBloodMoon() {
        if (isBloodMoonActive() && bloodMoonPos != null)
            OrthoviewClientEvents.centreCameraOnPos(bloodMoonPos);
    }

    public static void setBloodMoonTicks(int tickDuration, BlockPos pos) {
        boolean newBloodMoon = tickDuration > 0 && bloodMoonTicksLeft <= 0;
        bloodMoonTicksLeft = tickDuration;
        bloodMoonPos = pos;
        if (newBloodMoon) {
            SoundClientEvents.playFadeableMusicInstance(new FadeableMusicInstance(SoundRegistrar.BLOOD_MOON_SONG.get()));
        } else if (tickDuration <= 0) {
            SoundClientEvents.stopFadeableMusicInstance();
        }
    }

    public static Tooltip clockTooltip() {
        if (isBloodMoonActive()) {
            return Tooltip.of(I18n.get("abilities.reignofnether.blood_moon.clock_warning1")).titleColour(Theme.ERROR)
                    .body(I18n.get("abilities.reignofnether.blood_moon.clock_warning2", getTimeStrFromTicks(bloodMoonTicksLeft)))
                    .body(I18n.get("abilities.reignofnether.blood_moon.clock_warning3"));
        }
        boolean isDay = isDay(serverNormDayTime);
        String timeStr = get12HourTimeStr(serverNormDayTime);

        Tooltip t;
        if (targetClientTime != serverNormDayTime)
            t = Tooltip.of(I18n.get("time.reignofnether.time_is_distorted"))
                    .body(I18n.get("time.reignofnether.real_time", timeStr));
        else
            t = Tooltip.of(I18n.get("time.reignofnether.time", timeStr));

        t.tag(I18n.get(isDay ? "hud.reignofnether.clock.tag_day" : "hud.reignofnether.clock.tag_night"),
                isDay ? Theme.GOLD : Theme.MANA_TEXT);
        t.body(I18n.get(isDay ? "hud.reignofnether.clock.desc_day" : "hud.reignofnether.clock.desc_night"));
        t.status(isDay ? I18n.get("time.reignofnether.time_until_night", getTimeUntilStr(serverNormDayTime, DUSK))
                : I18n.get("time.reignofnether.time_until_day", getTimeUntilStr(serverNormDayTime, DAWN)), Theme.MUTED);

        if (SurvivalClientEvents.isEnabled) {
            long timeOffset = -getWaveSurvivalTimeModifier(SurvivalClientEvents.difficulty);
            t.status(I18n.get("time.reignofnether.time_until_next_wave",
                    getTimeUntilStrWithOffset(serverNormDayTime, DUSK, isDay ? 0 : timeOffset)), Theme.ORANGE);
        }
        return t;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END && !MC.isPaused())
            ticksSinceLastUpdate += RtsDebugClientEvents.getCappedTPS() / 20D;
    }

    public static Long getClientTime() {
        return TimeClientEvents.serverGameTime + (long) TimeClientEvents.ticksSinceLastUpdate;
    }
}
