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
 * The map controls: left of the minimap, or in a row below the big map. First lock, zoom in, zoom out and the two
 * turn buttons, then the display toggles that matter mid-match; the rest are in the settings menu.
 */
public final class MinimapTools {

    private MinimapTools() { }

    public static void render(HudFrame f) {
        f.zone(f.layout.tools());
        HudLayout l = f.layout;
        int size = HudLayout.TOOL;
        // Alt + arrow is handled in OrthoviewClientEvents (it needs Alt held), so the tiles carry no key
        f.cell(MinimapClientEvents.getMapLockButton(), l.toolX(0), l.toolY(0), size, null);
        MinimapClientEvents.ZOOM.renderIn(f, l.toolX(1), l.toolY(1), size);
        MinimapClientEvents.ZOOM.renderOut(f, l.toolX(2), l.toolY(2), size);
        f.cell(MinimapClientEvents.getCameraRotateCCWButton(), l.toolX(3), l.toolY(3), size, null);
        f.cell(MinimapClientEvents.getCameraRotateCWButton(), l.toolX(4), l.toolY(4), size, null);

        f.cell(MinimapClientEvents.getHighlightAnimalsButton(), l.toolX(5), l.toolY(5), size, null);
        f.cell(OrthoviewClientEvents.getLeavesHidingButton(), l.toolX(6), l.toolY(6), size, null);
        f.cell(MinimapClientEvents.getCamSensitivityButton(), l.toolX(7), l.toolY(7), size, null);

        toggle(f, 8, Icon.SATELLITE, StrategicMapClientEvents.isOpen(), "key.reignofnether.satellite", Keybindings.satellite,
                StrategicMapClientEvents::toggle);
        boolean big = HudGeometry.isBigMap();
        toggle(f, 9, big ? Icon.SHRINK : Icon.EXPAND, big, "key.reignofnether.big_map", Keybindings.bigMap,
                () -> HudGeometry.setBigMap(!big));
    }

    /** Its key is handled in PlayerDisplayClientEvents, so it works while the HUD is hidden too. */
    private static void toggle(HudFrame f, int tool, Icon icon, boolean on, String name, Keybinding key, Runnable action) {
        int x = f.layout.toolX(tool);
        int y = f.layout.toolY(tool);
        int size = HudLayout.TOOL;
        Rect r = new Rect(x, y, size, size);
        Theme.secondaryButton(f.g, x, y, size, size, f.hovered(r));
        icon.drawIn(f.g, x + 3, y + 3, size - 6, size - 6, on ? Theme.GOLD : Theme.BODY);
        f.register(new ButtonBuilder(name).onLeftClick(action).build(), r, null,
                () -> Tooltip.of(I18n.get(name)).key(key.getCurrentLabel()));
    }
}
