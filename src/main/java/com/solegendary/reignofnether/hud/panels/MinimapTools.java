package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;

/**
 * The map controls left of the minimap: one column with lock, zoom in, zoom out and the two turn
 * buttons. Display options (night circles, underlines...) live in the settings menu.
 */
public final class MinimapTools {

    private MinimapTools() { }

    public static void render(HudFrame f) {
        f.zone(f.layout.tools());
        // the rotate keys already turn the camera on their own, so the tiles carry no key
        f.cell(MinimapClientEvents.getMapLockButton(), f.layout.toolX(0), f.layout.toolY(0), HudLayout.TOOL, null);
        MinimapClientEvents.ZOOM.renderIn(f, f.layout.toolX(1), f.layout.toolY(1), HudLayout.TOOL);
        MinimapClientEvents.ZOOM.renderOut(f, f.layout.toolX(2), f.layout.toolY(2), HudLayout.TOOL);
        f.cell(MinimapClientEvents.getCameraRotateCCWButton(), f.layout.toolX(3), f.layout.toolY(3), HudLayout.TOOL, null);
        f.cell(MinimapClientEvents.getCameraRotateCWButton(), f.layout.toolX(4), f.layout.toolY(4), HudLayout.TOOL, null);
    }
}
