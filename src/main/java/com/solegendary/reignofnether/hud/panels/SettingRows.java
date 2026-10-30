package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.SettingRow;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

/**
 * Rows of "name ... [-] value [+]" (or an ON/OFF toggle): the game rules (HUD popover and match setup
 * window) and the settings menu. Rows flow into a second column when they don't fit the height.
 */
public final class SettingRows {

    private SettingRows() { }

    public static final int ROW_H = 17;
    private static final int VALUE_W = 85; // "BRIGHT YELLOW" beside a swatch
    private static final int STEP = 13;

    /** Size the panel needs for these rules given a maximum height. */
    public static Rect measure(List<SettingRow> rules, int colW, int maxH) {
        int rowsPerCol = Math.max(1, maxH / ROW_H);
        int cols = (rules.size() + rowsPerCol - 1) / rowsPerCol;
        int rows = Math.min(rules.size(), rowsPerCol);
        return new Rect(0, 0, cols * colW + (cols - 1) * 10, rows * ROW_H);
    }

    public static void render(HudFrame f, List<SettingRow> rules, int x, int y, int colW, int maxH) {
        int rowsPerCol = Math.max(1, maxH / ROW_H);
        for (int i = 0; i < rules.size(); i++) {
            int col = i / rowsPerCol;
            int row = i % rowsPerCol;
            renderRow(f, rules.get(i), x + col * (colW + 10), y + row * ROW_H, colW);
        }
    }

    private static void renderRow(HudFrame f, SettingRow rule, int x, int y, int w) {
        boolean editable = rule.editable().get();
        f.g.fill(x, y + ROW_H - 1, x + w, y + ROW_H, Theme.RAISED);
        Rect label = new Rect(x, y, w - VALUE_W - 2 * STEP - 6, ROW_H);
        Text.draw(f.g, Text.fit(rule.name(), label.w()), x, y + 4, editable ? Theme.TEXT : Theme.MUTED);
        f.register(new ButtonBuilder(rule.name()).build(), label, null, () -> {
            Tooltip t = Tooltip.of(rule.name()).body(rule.description());
            if (rule.tag() != null)
                t.tag(rule.tag(), Theme.ORANGE);
            return t.status(editable ? "" : rule.lockedHint(), Theme.LABEL);
        });

        int vx = x + w - VALUE_W - (rule.isToggle() ? 0 : STEP + 2);
        int vy = y + 2;
        int vh = ROW_H - 4;
        Rect value = new Rect(vx, vy, VALUE_W, vh);
        if (rule.isToggle() && isOnOff(rule.value().get())) {
            // plain on/off: a checkbox at the right end of the row
            boolean on = rule.on().get();
            int bx = x + w - vh;
            Rect box = new Rect(bx, vy, vh, vh);
            Theme.bevel(f.g, bx, vy, vh, vh, on ? Theme.TOGGLE_ON : Theme.INK, 0, 0, 1, Theme.INK);
            Theme.ring(f.g, bx, vy, vh, vh, 1, on ? Theme.OK : editable && f.hovered(box) ? Theme.LABEL : Theme.FAINT);
            if (on)
                Icon.CHECK.drawIn(f.g, bx, vy, vh, vh, Theme.OK_TEXT);
            f.register(action(rule.name(), editable, rule.increase()), box, null, null);
            return;
        }
        if (rule.isToggle()) {
            boolean on = rule.on().get();
            Theme.outline(f.g, vx, vy, VALUE_W, vh, 1, Theme.INK);
            f.g.fill(vx, vy, vx + VALUE_W, vy + vh, on ? Theme.TOGGLE_ON : Theme.SECONDARY);
            if (on)
                Theme.ring(f.g, vx, vy, VALUE_W, vh, 1, Theme.OK);
            if (editable && f.hovered(value))
                f.g.fill(vx, vy, vx + VALUE_W, vy + vh, Theme.HOVER);
            Text.drawCentered(f.g, Text.fit(rule.value().get(), VALUE_W - 4), value.centerX(), vy + 3, on ? Theme.OK_TEXT : Theme.LABEL);
            f.register(action(rule.name(), editable, rule.increase()), value, null, null);
            return;
        }
        Theme.outline(f.g, vx, vy, VALUE_W, vh, 1, Theme.INK);
        f.g.fill(vx, vy, vx + VALUE_W, vy + vh, Theme.INK);
        if (rule.swatch() != null) {
            // the colour itself, so nobody has to guess what a name like "lime" looks like
            f.g.fill(vx + 2, vy + 2, vx + vh - 2, vy + vh - 2, 0xFF000000 | rule.swatch().get());
            Text.drawCentered(f.g, Text.fit(rule.value().get(), VALUE_W - vh - 2), value.centerX() + vh / 2, vy + 3, Theme.GOLD);
        } else
            Text.drawCentered(f.g, Text.fit(rule.value().get(), VALUE_W - 4), value.centerX(), vy + 3, Theme.GOLD);
        stepper(f, "−", rule.name() + "-", editable, rule.decrease(), vx - STEP - 2, vy, vh);
        stepper(f, "+", rule.name() + "+", editable, rule.increase(), vx + VALUE_W + 2, vy, vh);
    }

    private static boolean isOnOff(String value) {
        return value.equals(I18n.get("options.on")) || value.equals(I18n.get("options.off"));
    }

    private static void stepper(HudFrame f, String glyph, String id, boolean editable, Runnable run, int x, int y, int h) {
        Rect r = new Rect(x, y, STEP, h);
        Theme.secondaryButton(f.g, x, y, STEP, h, editable && f.hovered(r));
        (glyph.equals("+") ? Icon.PLUS : Icon.MINUS).drawIn(f.g, x, y, STEP, h, editable ? Theme.BODY : Theme.FAINT);
        f.register(action(id, editable, run), r, null, null);
    }

    private static Button action(String id, boolean enabled, Runnable run) {
        return new ButtonBuilder(id)
                .isEnabled(() -> enabled)
                .onLeftClick(run)
                .build();
    }

    /** A titled popup with these rows, anchored with its top-right corner at (right, top). */
    public static Rect renderPopover(HudFrame f, String title, List<SettingRow> rows, int right, int top, int maxBottom, int colW) {
        int pad = 7;
        int maxH = maxBottom - top - pad * 2 - 16;
        Rect size = measure(rows, colW, maxH);
        int w = size.w() + pad * 2;
        int h = size.h() + pad * 2 + 16;
        int x = right - w;
        Theme.popup(f.g, x, top, w, h, Theme.ORANGE);
        f.zone(x, top, w, h);
        Text.heading(f.g, title, x + pad, top + pad, Theme.GOLD, Text.px(18));
        render(f, rows, x + pad, top + pad + 16, colW, maxH);
        return new Rect(x, top, w, h);
    }
}
