package com.solegendary.reignofnether.ui;

/**
 * Size of the RTS HUD, independent of Minecraft's GUI scale.
 *
 * The HUD is drawn in "units" where one unit is a whole number of physical pixels ({@link #pixelsPerUnit}),
 * so icons and text stay crisp at any window size. It follows Minecraft's GUI Scale setting: one unit is
 * half a GUI pixel, rounded up (Auto at 1080p is GUI scale 4, so 2 pixels per unit: the 1920x1080 design
 * at half size), and is lowered when needed so the layout keeps at least 640x360 units.
 *
 * @param pixelsPerUnit physical pixels per HUD unit (always >= 1)
 * @param width         HUD width in units
 * @param height        HUD height in units
 * @param guiScale      Minecraft's current GUI scale, used to convert from GUI coordinates
 */
public record HudScale(int pixelsPerUnit, int width, int height, double guiScale) {

    // a bigger user scale is never allowed to shrink the HUD below this
    public static final int HARD_MIN_WIDTH = 640;
    public static final int HARD_MIN_HEIGHT = 360;

    /**
     * @param guiScale     the GUI scale in use now (the RTS view caps it, so it can differ from the setting)
     * @param chosenScale  the GUI scale the player's setting resolves to, Auto included
     */
    public static HudScale of(int windowWidth, int windowHeight, double guiScale, int chosenScale) {
        int maxFit = Math.min(windowHeight / HARD_MIN_HEIGHT, windowWidth / HARD_MIN_WIDTH);
        int p = Math.max(1, Math.min((chosenScale + 1) / 2, maxFit));
        return new HudScale(p, windowWidth / p, windowHeight / p, guiScale);
    }

    /** Factor to apply to the pose stack so HUD units are drawn in GUI coordinates. */
    public float poseScale() {
        return (float) (pixelsPerUnit / guiScale);
    }

    public int toUnits(double guiCoord) {
        return (int) Math.floor(guiCoord * guiScale / pixelsPerUnit);
    }

    public double toGui(double units) {
        return units * pixelsPerUnit / guiScale;
    }
}
