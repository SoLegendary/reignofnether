package com.solegendary.reignofnether.time;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.sounds.FadeableMusicInstance;
import com.solegendary.reignofnether.sounds.SoundClientEvents;
import com.solegendary.reignofnether.survival.SurvivalClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.time.TimeUtils.*;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class TimeClientEvents {

    private static int xPos = 0;
    private static int yPos = 0;

    private static final Minecraft MC = Minecraft.getInstance();

    // setting this value causes the time of day to smoothly move towards it regardless of the server time
    public static long targetClientTime = 0;
    // actual time on the server
    public static long serverTime = 0;

    public static boolean showClockTooltip = false;

    private static final Button clockButton = new Button("Clock",
            10,
            null,
            null,
            null,
            () -> false,
            () -> !OrthoviewClientEvents.isEnabled(),
            () -> true,
            () -> showClockTooltip = !showClockTooltip,
            null,
            null
    );
    private static Button bloodMoonButton = getBloodMoonButton();
    private static int bloodMoonTicksLeft = 0;
    private static BlockPos bloodMoonPos = null;
    public static void resetBloodMoon() {
        bloodMoonTicksLeft = 0;
        bloodMoonPos = null;
    }

    public static boolean isBloodMoonActive() {
        return bloodMoonTicksLeft > 0;
    }

    private static Button getBloodMoonButton() {
        return new Button("Clock",
                14,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/blood_moon.png"),
                null,
                null,
                () -> false,
                () -> !OrthoviewClientEvents.isEnabled() || !isBloodMoonActive(),
                () -> true,
                null,
                () -> {
                    if (bloodMoonPos != null)
                        OrthoviewClientEvents.centreCameraOnPos(bloodMoonPos);
                },
                List.of(
                        fcs(I18n.get("abilities.reignofnether.blood_moon.clock_warning1"), Style.EMPTY.withColor(0xFF0000)),
                        fcs(I18n.get("abilities.reignofnether.blood_moon.clock_warning2", getTimeStrFromTicks(bloodMoonTicksLeft))),
                        fcs(I18n.get("abilities.reignofnether.blood_moon.clock_warning3"))
                )
        );
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

    // render directly above the minimap
    @SubscribeEvent
    public static void renderOverlay(RenderGuiOverlayEvent.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() || MC.isPaused()
            || !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK)) {
            return;
        }

        if (!isBloodMoonActive()) {
            xPos = MC.getWindow().getGuiScaledWidth() - MinimapClientEvents.getMapGuiRadius() - (
                    MinimapClientEvents.CORNER_OFFSET * 2
            ) + 2;
            yPos = MC.getWindow().getGuiScaledHeight() - (MinimapClientEvents.getMapGuiRadius() * 2) - (
                    MinimapClientEvents.CORNER_OFFSET * 2
            ) - 6;

            evt.getGuiGraphics().renderItem(new ItemStack(Items.CLOCK), xPos, yPos);
            evt.getGuiGraphics().renderItemDecorations(MC.font, new ItemStack(Items.CLOCK), xPos, yPos);
        }
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() || MC.isPaused()
            || !TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK)) {
            return;
        }

        xPos = MC.getWindow().getGuiScaledWidth() - MinimapClientEvents.getMapGuiRadius() - (
            MinimapClientEvents.CORNER_OFFSET * 2
        ) + 2;
        yPos = MC.getWindow().getGuiScaledHeight() - (MinimapClientEvents.getMapGuiRadius() * 2) - (
            MinimapClientEvents.CORNER_OFFSET * 2
        ) - 6;

        bloodMoonButton = getBloodMoonButton();
        if (!bloodMoonButton.isHidden.get() && evt.getScreen() instanceof TopdownGui) {
            bloodMoonButton.render(evt.getGuiGraphics(), xPos - 3, yPos - 3, evt.getMouseX(), evt.getMouseY());
            if (bloodMoonButton.isMouseOver(evt.getMouseX(), evt.getMouseY()))
                bloodMoonButton.renderTooltip(evt.getGuiGraphics(), evt.getMouseX(), evt.getMouseY());
        }
        else if (!clockButton.isHidden.get() && evt.getScreen() instanceof TopdownGui)
            clockButton.render(evt.getGuiGraphics(), xPos - 3, yPos - 3, evt.getMouseX(), evt.getMouseY());
    }

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            clockButton.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), true);
            bloodMoonButton.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), true);
        }
        else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            clockButton.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), false);
            bloodMoonButton.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), false);
        }
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render evt) {
        if (!TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK) ||
            !(MC.screen instanceof TopdownGui)) {
            return;
        }

        final int GUI_LENGTH = 16;

        boolean isMouseOver = evt.getMouseX() > xPos &&
                evt.getMouseX() <= xPos + GUI_LENGTH &&
                evt.getMouseY() > yPos &&
                evt.getMouseY() <= yPos + GUI_LENGTH;

        if (!isBloodMoonActive() && (isMouseOver || showClockTooltip)) {

            // 'day' is when undead start burning, ~500
            // 'night' is when undead stop burning, ~12500
            boolean isDay = isDay(serverTime);
            String timeStr = get12HourTimeStr(serverTime);

            FormattedCharSequence timeUntilStr =
                FormattedCharSequence.forward(
                    isDay ? I18n.get("time.reignofnether.time_until_night",
                        getTimeUntilStr(serverTime, DUSK)) :
                        I18n.get("time.reignofnether.time_until_day",
                        getTimeUntilStr(serverTime, DAWN)),
                    Style.EMPTY);

            ArrayList<FormattedCharSequence> tooltip = new ArrayList<>();

            if (!showClockTooltip) {
                if (targetClientTime != serverTime) {
                    tooltip.add(FormattedCharSequence.forward(I18n.get("time.reignofnether.time_is_distorted"), Style.EMPTY.withBold(true)));
                    tooltip.add(FormattedCharSequence.forward(I18n.get("time.reignofnether.real_time", timeStr), Style.EMPTY));
                } else {
                    tooltip.add(FormattedCharSequence.forward(I18n.get("time.reignofnether" + ".time", timeStr), Style.EMPTY));
                }
            }
            tooltip.add(timeUntilStr);

            if (SurvivalClientEvents.isEnabled) {
                long timeOffset = -getWaveSurvivalTimeModifier(SurvivalClientEvents.difficulty);
                tooltip.add(FormattedCharSequence.forward(I18n.get("time.reignofnether.time_until_next_wave",
                        getTimeUntilStrWithOffset(serverTime, DUSK, isDay ? 0 : timeOffset)), Style.EMPTY));
            }

            if (PlayerClientEvents.isRTSPlayer() && !SurvivalClientEvents.isEnabled) {
                FormattedCharSequence gameLengthStr = FormattedCharSequence.forward(
                        I18n.get("time.reignofnether.game_time", getTimeStrFromTicks(PlayerClientEvents.rtsGameTicks)),
                        Style.EMPTY
                );
                tooltip.add(gameLengthStr);
            }
            if (showClockTooltip) {
                MyRenderer.renderTooltip(evt.getGuiGraphics(), tooltip, xPos + 57, yPos - 4);
            } else {
                MyRenderer.renderTooltip(evt.getGuiGraphics(), tooltip, evt.getMouseX(), evt.getMouseY());
            }
        }
    }
}














