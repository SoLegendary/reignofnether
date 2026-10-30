package com.solegendary.reignofnether.ui;

import com.solegendary.reignofnether.hud.MyEditBox;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.hud.panels.SettingRows;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Rows of the sandbox editor popups (custom building, scenario role), drawn like the settings menu rows:
 * name on the left, and on the right a checkbox, a ◀ value ▶ stepper or a value with a small button.
 * Every method draws one row at (x, y), registers its clickable parts on the frame and returns the next y.
 */
public final class EditorRows {

    private EditorRows() { }

    public static final int ROW_H = SettingRows.ROW_H;
    public static final int GROUP_H = 13;
    public static final int HEADER_H = 22;
    private static final int VALUE_W = 40;
    private static final int STEP = 13;
    private static final int BUTTON_W = 40;
    private static final int ICON = 11;

    /** Starts a frame in HUD units for a popup drawn on a screen render event; pair with {@link #end}. */
    public static HudFrame begin(GuiGraphics g, double mouseX, double mouseY) {
        HudScale scale = HudGeometry.scale();
        HudFrame f = new HudFrame(g, scale, HudGeometry.layout(), scale.toUnits(mouseX), scale.toUnits(mouseY));
        g.pose().pushPose();
        g.pose().scale(scale.poseScale(), scale.poseScale(), 1);
        return f;
    }

    public static void end(HudFrame f) {
        f.showHoveredTooltip();
        Tooltip.renderPending(f.g, f.layout.width(), f.layout.height());
        f.g.pose().popPose();
    }

    /** Mouse press in GUI coordinates (left or right button) on last frame's popup. */
    public static void click(@Nullable HudFrame f, double mouseX, double mouseY, int button) {
        if (f != null && (button == GLFW.GLFW_MOUSE_BUTTON_1 || button == GLFW.GLFW_MOUSE_BUTTON_2))
            f.click(f.scale.toUnits(mouseX), f.scale.toUnits(mouseY), button == GLFW.GLFW_MOUSE_BUTTON_1);
    }

    public static boolean isOver(@Nullable HudFrame f, double mouseX, double mouseY) {
        return f != null && f.isOverHud(f.scale.toUnits(mouseX), f.scale.toUnits(mouseY));
    }

    /** Height in units of a field that holds an unbordered vanilla edit box (drawn in GUI pixels). */
    public static int fieldHeight(HudFrame f) {
        return 12;
    }

    /**
     * Draws a sunken field and moves the edit box into it, in HUD units: TextInputClientEvents draws and
     * clicks it under the HUD scale, so its text is the same size as the rest of the HUD.
     */
    public static void field(HudFrame f, EditBox box, Rect r) {
        Theme.outline(f.g, r.x(), r.y(), r.w(), r.h(), 1, Theme.INK);
        f.g.fill(r.x(), r.y(), r.right(), r.bottom(), Theme.SUNKEN);
        f.zone(r);
        if (box instanceof MyEditBox my)
            my.hudUnits = true;
        box.setX(r.x() + 3);
        box.setY(r.centerY() - 4);
        box.setWidth(r.w() - 6);
    }

    public static Supplier<Tooltip> tip(String title, @Nullable String body) {
        return () -> body == null || body.isEmpty() ? Tooltip.of(title) : Tooltip.of(title).body(body);
    }

    /** Title row: heading on the left, then an optional help "?" and the close cross at the right end. */
    public static int header(HudFrame f, String title, int x, int y, int w, @Nullable Supplier<Tooltip> help, Runnable close) {
        float scale = Text.px(18);
        int room = w - (help != null ? 31 : 16);
        String shown = title;
        for (int cut = title.length(); cut > 0 && Text.headingWidth(shown, scale) > room; )
            shown = title.substring(0, --cut) + "…";
        Text.heading(f.g, shown, x, y + 2, Theme.GOLD, scale);
        Rect cross = new Rect(x + w - 12, y, 12, 12);
        Theme.secondaryButton(f.g, cross.x(), cross.y(), cross.w(), cross.h(), f.hovered(cross));
        Theme.cross(f.g, cross, f.hovered(cross) ? Theme.GOLD : Theme.BODY);
        f.register(new ButtonBuilder(title + " close").onLeftClick(close).build(), cross, null, null);
        if (help != null) {
            Rect q = new Rect(cross.x() - 15, y, 12, 12);
            Theme.secondaryButton(f.g, q.x(), q.y(), q.w(), q.h(), f.hovered(q));
            Text.drawCentered(f.g, "?", q.centerX() + 1, q.y() + 2, f.hovered(q) ? Theme.GOLD : Theme.BODY);
            f.register(new ButtonBuilder(title + " help").build(), q, null, help);
        }
        return y + HEADER_H;
    }

    /** One row of a group, drawn at (x, y) with the column width. */
    @FunctionalInterface
    public interface Row {
        void draw(HudFrame f, int x, int y, int w);
    }

    /** Rows under a small-caps label (null: no label). */
    public record Group(@Nullable String label, List<Row> rows) {
        int height() {
            return (label != null ? GROUP_H : 0) + rows.size() * ROW_H;
        }
    }

    private static final int GROUP_GAP = 4;
    private static final int COL_GAP = 10;

    /** Size of the groups in one column, or more when they don't fit maxH (a group never splits). */
    public static Rect measure(List<Group> groups, int colW, int maxH) {
        return layout(null, groups, 0, 0, colW, maxH);
    }

    public static Rect render(HudFrame f, List<Group> groups, int x, int y, int colW, int maxH) {
        return layout(f, groups, x, y, colW, maxH);
    }

    private static Rect layout(@Nullable HudFrame f, List<Group> groups, int x, int y, int colW, int maxH) {
        int col = 0, cy = 0, h = 0;
        for (Group group : groups) {
            if (cy > 0 && cy + group.height() > maxH) {
                col++;
                cy = 0;
            }
            int gx = x + col * (colW + COL_GAP);
            int ry = y + cy;
            if (group.label() != null) {
                if (f != null)
                    Text.small(f.g, group.label().toUpperCase(Locale.ROOT), gx, ry + 3, Theme.LABEL);
                ry += GROUP_H;
            }
            if (f != null)
                for (Row row : group.rows()) {
                    row.draw(f, gx, ry, colW);
                    ry += ROW_H;
                }
            h = Math.max(h, cy + group.height());
            cy += group.height() + GROUP_GAP;
        }
        return new Rect(x, y, (col + 1) * colW + col * COL_GAP, h);
    }

    /** Name on the left with its tooltip; returns the rectangle left for the row's control. */
    public static Rect label(HudFrame f, String name, @Nullable Supplier<Tooltip> tip, int x, int y, int w, int controlW) {
        return label(f, null, name, tip, x, y, w, controlW);
    }

    /** Same with an icon in front of the name. */
    public static Rect label(HudFrame f, @Nullable ResourceLocation icon, String name, @Nullable Supplier<Tooltip> tip, int x, int y, int w, int controlW) {
        f.g.fill(x, y + ROW_H - 1, x + w, y + ROW_H, Theme.RAISED);
        Rect label = new Rect(x, y, w - controlW - 6, ROW_H);
        int textX = x;
        if (icon != null) {
            MyRenderer.renderIcon(f.g, icon, x, y + (ROW_H - ICON) / 2, ICON);
            textX += ICON + 3;
        }
        Text.draw(f.g, Text.fit(name, label.right() - textX), textX, y + 4, Theme.TEXT);
        f.register(new ButtonBuilder(name).build(), label, null, tip);
        return new Rect(x + w - controlW, y + 2, controlW, ROW_H - 4);
    }

    /** Checkbox at the right end of the row; the whole row toggles it. */
    public static int toggle(HudFrame f, String name, @Nullable Supplier<Tooltip> tip, boolean on, Runnable toggle, int x, int y, int w) {
        Rect box = label(f, name, tip, x, y, w, ROW_H - 4);
        boolean hovered = f.hovered(x, y, w, ROW_H);
        Theme.bevel(f.g, box.x(), box.y(), box.w(), box.h(), on ? Theme.TOGGLE_ON : Theme.INK, 0, 0, 1, Theme.INK);
        Theme.ring(f.g, box.x(), box.y(), box.w(), box.h(), 1, on ? Theme.OK : hovered ? Theme.LABEL : Theme.FAINT);
        if (on)
            Icon.CHECK.drawIn(f.g, box.x(), box.y(), box.w(), box.h(), Theme.OK_TEXT);
        f.register(new ButtonBuilder(name + " toggle").onLeftClick(toggle).build(), new Rect(x, y, w, ROW_H), null, tip);
        return y + ROW_H;
    }

    /**
     * ◀ value ▶. Like the old icon buttons, a left click on the value or ▶ increases and a right click
     * decreases (◀ the other way round), so shift/ctrl read inside the handlers keep working.
     */
    public static int stepper(HudFrame f, String name, @Nullable Supplier<Tooltip> tip, String value,
                              Runnable increase, @Nullable Runnable decrease, int x, int y, int w) {
        return stepper(f, null, name, tip, value, null, increase, decrease, x, y, w);
    }

    /** Same with an icon in front of the name; valueIcon, when given, replaces the value text. */
    public static int stepper(HudFrame f, @Nullable ResourceLocation icon, String name, @Nullable Supplier<Tooltip> tip, String value,
                              @Nullable ResourceLocation valueIcon, Runnable increase, @Nullable Runnable decrease, int x, int y, int w) {
        Rect c = label(f, icon, name, tip, x, y, w, VALUE_W + 2 * (STEP + 2));
        Rect val = new Rect(c.x() + STEP + 2, c.y(), VALUE_W, c.h());
        Theme.outline(f.g, val.x(), val.y(), val.w(), val.h(), 1, Theme.INK);
        f.g.fill(val.x(), val.y(), val.right(), val.bottom(), Theme.INK);
        if (valueIcon != null)
            MyRenderer.renderIcon(f.g, valueIcon, val.centerX() - ICON / 2, val.y() + (val.h() - ICON) / 2, ICON);
        else
            Text.drawCentered(f.g, Text.fit(value, VALUE_W - 4), val.centerX(), val.y() + 3, Theme.GOLD);
        f.register(new ButtonBuilder(name + " value").onLeftClick(increase).onRightClick(decrease).build(), val, null, tip);
        arrow(f, name + " <", "◀", new Rect(c.x(), c.y(), STEP, c.h()), decrease != null ? decrease : increase, decrease != null ? increase : null, tip);
        arrow(f, name + " >", "▶", new Rect(val.right() + 2, c.y(), STEP, c.h()), increase, decrease, tip);
        return y + ROW_H;
    }

    private static void arrow(HudFrame f, String id, String glyph, Rect r, Runnable left, @Nullable Runnable right, @Nullable Supplier<Tooltip> tip) {
        Theme.secondaryButton(f.g, r.x(), r.y(), r.w(), r.h(), f.hovered(r));
        Text.drawCentered(f.g, glyph, r.centerX() + 1, r.y() + 2, f.hovered(r) ? Theme.GOLD : Theme.BODY);
        f.register(new ButtonBuilder(id).onLeftClick(left).onRightClick(right).build(), r, null, tip);
    }

    /** A value with a small secondary button after it ("Edit", "Select"). */
    public static int action(HudFrame f, String name, @Nullable Supplier<Tooltip> tip, String value, String button,
                             Runnable left, @Nullable Runnable right, int x, int y, int w) {
        return action(f, null, name, tip, value, button, BUTTON_W, left, right, x, y, w);
    }

    /** Same with an icon in front of the name and a button of the given width. */
    public static int action(HudFrame f, @Nullable ResourceLocation icon, String name, @Nullable Supplier<Tooltip> tip, String value, String button,
                             int buttonW, Runnable left, @Nullable Runnable right, int x, int y, int w) {
        Rect c = label(f, icon, name, tip, x, y, w, VALUE_W + buttonW + 4);
        Text.drawCentered(f.g, Text.fit(value, VALUE_W - 4), c.x() + VALUE_W / 2, c.y() + 3, Theme.GOLD);
        textButton(f, name + " action", button, new Rect(c.right() - buttonW, c.y(), buttonW, c.h()), Style.SECONDARY, left, right, tip);
        return y + ROW_H;
    }

    public enum Style { PRIMARY, SECONDARY, DANGER }

    /** A labelled button in one of the theme's button styles. */
    public static void textButton(HudFrame f, String id, String label, Rect r, Style style,
                                  @Nullable Runnable left, @Nullable Runnable right, @Nullable Supplier<Tooltip> tip) {
        boolean hovered = f.hovered(r);
        int colour = switch (style) {
            case PRIMARY -> {
                Theme.primaryButton(f.g, r.x(), r.y(), r.w(), r.h(), hovered);
                yield Theme.ON_ORANGE;
            }
            case DANGER -> {
                Theme.dangerButton(f.g, r.x(), r.y(), r.w(), r.h(), hovered);
                yield Theme.DANGER_TEXT;
            }
            default -> {
                Theme.button(f.g, r.x(), r.y(), r.w(), r.h(), hovered);
                yield hovered ? Theme.GOLD : Theme.BODY;
            }
        };
        Text.mediumCentered(f.g, Text.fit(label, r.w() - 4), r.centerX(), r.y() + (r.h() - 8) / 2, colour);
        f.register(new ButtonBuilder(id).onLeftClick(left).onRightClick(right).build(), r, null, tip);
    }
}
