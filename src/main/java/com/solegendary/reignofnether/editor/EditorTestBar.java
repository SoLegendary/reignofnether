package com.solegendary.reignofnether.editor;

import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Toast;
import com.solegendary.reignofnether.ui.Tooltip;
import net.minecraft.client.resources.language.I18n;

/**
 * "TEST MATCH · ■ Stop", floating at the top centre for the whole test match, the end screen included, so a match
 * that ended on its own is stopped the same way. Call render every frame: it draws nothing outside a test.
 */
public final class EditorTestBar {

    private EditorTestBar() { }

    private static final int H = 20;
    private static final int PAD = 5;
    private static final int BUTTON_H = 14;

    public static void render(HudFrame f) {
        if (EditorTest.restored) {
            EditorTest.restored = false;
            Toast.show(I18n.get("editor.reignofnether.test.restored"), Toast.DEFAULT_TICKS);
        }
        if (!EditorTest.isTesting())
            return;
        String label = I18n.get("editor.reignofnether.test.bar");
        String stop = I18n.get("editor.reignofnether.test.stop");
        int labelW = Text.smallWidth(label);
        int buttonW = 8 + 4 + Text.width(stop) + 12; // square, gap, text, padding
        int w = PAD + labelW + 8 + buttonW + PAD;
        int x = (f.layout.width() - w) / 2;
        int y = HudLayout.MARGIN;
        Theme.popup(f.g, x, y, w, H, Theme.ORANGE);
        f.zone(x, y, w, H);
        Text.small(f.g, label, x + PAD, y + (H - 7) / 2, Theme.GOLD);

        Rect button = new Rect(x + w - PAD - buttonW, y + (H - BUTTON_H) / 2 + 1, buttonW, BUTTON_H);
        Theme.primaryButton(f.g, button.x(), button.y(), button.w(), button.h(), f.hovered(button));
        int sx = button.x() + 6, sy = button.centerY() - 3;
        f.g.fill(sx, sy, sx + 6, sy + 6, Theme.ON_ORANGE); // ■
        Text.draw(f.g, stop, sx + 10, button.y() + (BUTTON_H - Text.lineHeight()) / 2 + 1, Theme.ON_ORANGE);
        f.register(new ButtonBuilder("editor_test_stop").onLeftClick(EditorTest::stop).build(), button, null,
                () -> Tooltip.of(I18n.get("editor.reignofnether.test.stop_tip")));
    }
}
