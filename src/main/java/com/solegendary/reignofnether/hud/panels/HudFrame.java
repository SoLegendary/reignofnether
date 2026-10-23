package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.hud.buttons.AbstractUnitItemButton;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.ui.HudScale;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Everything the HUD drew in one frame, in HUD units: the areas that block world clicks and every
 * clickable element with its rectangle, hotkey and tooltip. Clicks, hotkeys and tooltips are all
 * resolved here, so exactly one element reacts to a click or key press.
 */
public final class HudFrame {

    public final GuiGraphics g;
    public final HudScale scale;
    public final HudLayout layout;
    public final int mouseX;
    public final int mouseY;

    private final List<Rect> zones = new ArrayList<>();
    private final List<Placed> placed = new ArrayList<>();

    /**
     * @param key     hotkey that triggers it (null: none)
     * @param tooltip tooltip shown on hover (null: the button's own legacy tooltip)
     */
    public record Placed(Button button, Rect rect, @Nullable Keybinding key, @Nullable Supplier<Tooltip> tooltip,
                         @Nullable Runnable altAction) { }

    public HudFrame(GuiGraphics g, HudScale scale, HudLayout layout, int mouseX, int mouseY) {
        this.g = g;
        this.scale = scale;
        this.layout = layout;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public boolean hovered(Rect r) {
        return r.contains(mouseX, mouseY);
    }

    public boolean hovered(int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
    }

    /** Marks an area as HUD so clicks there don't reach the world. */
    public void zone(Rect r) {
        zones.add(r);
    }

    public void zone(int x, int y, int w, int h) {
        zones.add(new Rect(x, y, w, h));
    }

    /** Registers a clickable area drawn by the caller. */
    public void register(Button b, Rect r, @Nullable Keybinding key, @Nullable Supplier<Tooltip> tooltip) {
        b.x = r.x();
        b.y = r.y();
        b.drawnSize = r.w();
        placed.add(new Placed(b, r, key, tooltip, null));
    }

    /** Registers a clickable area whose hotkey runs altAction instead when Alt is held (hero rank-up). */
    public void register(Button b, Rect r, @Nullable Keybinding key, @Nullable Supplier<Tooltip> tooltip, @Nullable Runnable altAction) {
        b.x = r.x();
        b.y = r.y();
        b.drawnSize = r.w();
        placed.add(new Placed(b, r, key, tooltip, altAction));
    }

    /** Draws a button as a HUD cell and registers it. The key decides both the label and the trigger. */
    public void cell(Button b, int x, int y, int size, @Nullable Keybinding key) {
        cell(b, x, y, size, key, null);
    }

    public void cell(Button b, int x, int y, int size, @Nullable Keybinding key, @Nullable Runnable altAction) {
        String label = key != null ? key.getCurrentLabel() : null;
        Rect r = new Rect(x, y, size, size);
        ButtonCell.render(g, b, x, y, size, label, hovered(r));
        register(b, r, key, () -> ButtonCell.tooltipFor(b, label), altAction);
    }

    /** Same as {@link #cell} using the button's own hotkey. */
    public void cell(Button b, int x, int y, int size) {
        cell(b, x, y, size, b.hotkey);
    }

    public List<Placed> placed() {
        return placed;
    }

    public boolean isOverHud(int ux, int uy) {
        for (Rect r : zones)
            if (r.contains(ux, uy))
                return true;
        return buttonAt(ux, uy) != null;
    }

    @Nullable
    public Placed buttonAt(int ux, int uy) {
        // last registered is drawn on top
        for (int i = placed.size() - 1; i >= 0; i--)
            if (placed.get(i).rect().contains(ux, uy))
                return placed.get(i);
        return null;
    }

    /** Queues the tooltip of the hovered element; drawn at the end of the frame. */
    public void showHoveredTooltip() {
        Placed p = buttonAt(mouseX, mouseY);
        if (p == null)
            return;
        Button b = p.button();
        // items, effects and buttons without their own HUD tooltip restyle their legacy one
        Tooltip t = b instanceof AbstractUnitItemButton || p.tooltip() == null ? b.hudTooltip() : p.tooltip().get();
        if (t != null)
            t.show(p.rect().x(), p.rect().y(), p.rect().w(), p.rect().h());
    }

    /** @return true if a button took the click */
    public boolean click(int ux, int uy, boolean left) {
        Placed p = buttonAt(ux, uy);
        if (p == null || !p.button().isEnabled.get())
            return false;
        Runnable action = left ? p.button().onLeftClick : p.button().onRightClick;
        if (action == null)
            return false;
        playClick();
        action.run();
        return true;
    }

    public boolean release(int ux, int uy) {
        Placed p = buttonAt(ux, uy);
        if (p == null || p.button().onLeftClickRelease == null || !p.button().isEnabled.get())
            return false;
        p.button().onLeftClickRelease.run();
        return true;
    }

    /** Fires the first element bound to this key. @return true if one fired */
    public boolean press(int keyCode, boolean altDown) {
        for (Placed p : placed) {
            if (p.key() == null || p.key().getKey() != keyCode)
                continue;
            if (altDown && p.altAction() != null) {
                playClick();
                p.altAction().run();
                return true;
            }
            Button b = p.button();
            if (!b.isEnabled.get())
                return true; // the key belongs to this button even if it can't be used right now
            playClick();
            b.pressHotkey();
            return true;
        }
        return false;
    }

    private static void playClick() {
        var player = Minecraft.getInstance().player;
        if (player != null)
            player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.2f, 1.0f);
    }
}
