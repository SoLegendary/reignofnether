package com.solegendary.reignofnether.hud.layout;

import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.ui.HudScale;
import com.solegendary.reignofnether.ui.Rect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HudLayoutTest {

    private static final int[][] WINDOWS = {
        { 800, 600 }, { 1280, 720 }, { 1366, 768 }, { 1920, 1080 }, { 2560, 1440 }, { 3440, 1440 }, { 3840, 2160 }
    };

    @Test
    void scaleFollowsGuiScaleAndKeepsTheDesignSize() {
        // GUI Scale Auto values from Minecraft for each window
        assertEquals(new HudScale(2, 960, 540, 3.0), HudScale.of(1920, 1080, 3.0, 4));
        assertEquals(4.5, HudScale.of(3840, 2160, 3.0, 9).pixelsPerUnit());
        assertEquals(1.5, HudScale.of(1280, 720, 3.0, 3).pixelsPerUnit());
        assertEquals(3, HudScale.of(2560, 1440, 3.0, 6).pixelsPerUnit());
        assertEquals(2, HudScale.of(2000, 960, 3.0, 4).pixelsPerUnit());
        // a smaller GUI Scale setting gives a smaller HUD
        assertEquals(1, HudScale.of(1920, 1080, 2.0, 2).pixelsPerUnit());
        for (int[] win : WINDOWS)
            for (int chosen = 1; chosen <= 8; chosen++) {
                HudScale s = HudScale.of(win[0], win[1], 2.0, chosen);
                assertTrue(s.pixelsPerUnit() >= 1);
                assertTrue(s.width() >= HudScale.HARD_MIN_WIDTH || s.pixelsPerUnit() == 1, Arrays.toString(win));
            }
    }

    @Test
    void bigGuiScaleCannotShrinkTheHudBelowTheMinimum() {
        HudScale big = HudScale.of(1920, 1080, 3.0, 6);
        assertEquals(3, big.pixelsPerUnit());
        assertTrue(big.width() >= HudScale.HARD_MIN_WIDTH && big.height() >= HudScale.HARD_MIN_HEIGHT);
        assertEquals(2, HudScale.of(1280, 720, 3.0, 6).pixelsPerUnit()); // 640x360 is the floor
        assertEquals(768 / 360.0, HudScale.of(1366, 768, 3.0, 6).pixelsPerUnit()); // shrinks exactly to the height floor
    }

    @Test
    void zoneCoversOnlyWhatWasRegisteredBeforeIt() {
        HudFrame f = new HudFrame(null, null, null, 0, 0);
        Rect r = new Rect(0, 0, 10, 10);
        f.register(new ButtonBuilder("under").build(), r, null, null);
        f.zone(r);
        assertNull(f.buttonAt(5, 5));
        assertTrue(f.isOverHud(5, 5));
        f.register(new ButtonBuilder("over").build(), r, null, null);
        assertEquals("over", f.buttonAt(5, 5).button().name);
    }

    @Test
    void mouseConversionRoundTrips() {
        HudScale s = HudScale.of(1920, 1080, 3.0, 4); // 640x360 GUI px, 960x540 units
        assertEquals(480, s.toUnits(320));
        assertEquals(320.0, s.toGui(480), 1e-9);
        assertEquals(2f / 3f, s.poseScale(), 1e-6);
    }

    @Test
    void regionsFitOnScreenAndDoNotOverlap() {
        for (int[] win : WINDOWS)
            for (int chosen = 1; chosen <= 8; chosen++) {
                HudScale s = HudScale.of(win[0], win[1], 2.0, chosen);
                HudLayout l = HudLayout.compute(s.width(), s.height());
                String ctx = Arrays.toString(win) + " gui scale " + chosen;

                List<Rect> regions = new ArrayList<>(List.of(l.commandCard(), l.selection(), l.tools(), l.minimap()));
                for (Rect r : regions) {
                    assertTrue(r.x() >= 0 && r.y() >= 0 && r.right() <= s.width() && r.bottom() <= s.height(), ctx + " " + r);
                    assertTrue(r.y() >= l.console().y(), ctx + " inside console " + r);
                }
                for (int i = 0; i < regions.size(); i++)
                    for (int j = i + 1; j < regions.size(); j++)
                        assertFalse(regions.get(i).intersects(regions.get(j)), ctx + " " + regions.get(i) + " vs " + regions.get(j));

                assertTrue(l.selection().w() >= 180, ctx + " selection too narrow: " + l.selection().w());
            }
    }

    @Test
    void barSplitsOnlyAboveItsMaximumWidth() {
        assertFalse(HudLayout.compute(HudLayout.MAX_BAR, 540).split());
        assertTrue(HudLayout.compute(HudLayout.MAX_BAR + 1, 540).split());
    }

    @Test
    void commandCardFitsTheConsole() {
        HudLayout l = HudLayout.compute(960, 540);
        assertEquals(HudLayout.CONSOLE_HEIGHT - HudLayout.CONSOLE_PAD * 2, HudLayout.cardHeight());
        int last = CommandGrid.SLOTS - 1;
        assertTrue(l.slotX(last) + HudLayout.CELL <= l.commandCard().right());
        assertTrue(l.slotY(last) + HudLayout.CELL <= l.commandCard().bottom());
    }
}
