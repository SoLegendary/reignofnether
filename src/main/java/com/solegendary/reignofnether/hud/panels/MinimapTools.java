package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.minimap.StrategicMapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import net.minecraft.client.resources.language.I18n;

/**
 * The map controls: lock, zoom in, zoom out and the two turn buttons in a column left of the minimap, with its expand
 * button on the corner. The big map adds the display toggles that matter mid-match, all in a row below it; the rest
 * are in the settings menu.
 */
public final class MinimapTools {

    private MinimapTools() { }

    public static void render(HudFrame f) {
        HudLayout l = f.layout;
        int size = HudLayout.TOOL;
        boolean big = HudGeometry.isBigMap();
        f.zone(l.tools());
        // Alt + arrow is handled in OrthoviewClientEvents (it needs Alt held), so the tiles carry no key
        f.cell(MinimapClientEvents.getMapLockButton(), l.toolX(0), l.toolY(0), size, null);
        MinimapClientEvents.ZOOM.renderIn(f, l.toolX(1), l.toolY(1), size);
        MinimapClientEvents.ZOOM.renderOut(f, l.toolX(2), l.toolY(2), size);
        f.cell(MinimapClientEvents.getCameraRotateCCWButton(), l.toolX(3), l.toolY(3), size, null);
        f.cell(MinimapClientEvents.getCameraRotateCWButton(), l.toolX(4), l.toolY(4), size, null);

        if (big) {
            f.cell(MinimapClientEvents.getHighlightAnimalsButton(), l.toolX(5), l.toolY(5), size, null);
            f.cell(OrthoviewClientEvents.getLeavesHidingButton(), l.toolX(6), l.toolY(6), size, null);
            f.cell(MinimapClientEvents.getCamSensitivityButton(), l.toolX(7), l.toolY(7), size, null);
            toggle(f, l.toolX(8), l.toolY(8), Icon.SATELLITE, StrategicMapClientEvents.isOpen(), "key.reignofnether.satellite", Keybindings.satellite,
                    StrategicMapClientEvents::toggle);
        }
        int x = big ? l.toolX(9) : l.minimap().right() - size;
        int y = big ? l.toolY(9) : l.minimap().bottom() - size;
        f.zone(new Rect(x, y, size, size));
        toggle(f, x, y, big ? Icon.SHRINK : Icon.EXPAND, big, "key.reignofnether.big_map", Keybindings.bigMap,
                () -> HudGeometry.setBigMap(!big));
    }

    /** Its key is handled in PlayerDisplayClientEvents, so it works while the HUD is hidden too. */
    private static void toggle(HudFrame f, int x, int y, Icon icon, boolean on, String name, Keybinding key, Runnable action) {
        int size = HudLayout.TOOL;
        Rect r = new Rect(x, y, size, size);
        Theme.secondaryButton(f.g, x, y, size, size, f.hovered(r));
        icon.drawIn(f.g, x + 3, y + 3, size - 6, size - 6, on ? Theme.GOLD : Theme.BODY);
        f.register(new ButtonBuilder(name).onLeftClick(action).build(), r, null,
                () -> Tooltip.of(I18n.get(name)).key(key.getCurrentLabel()));
    }
}
